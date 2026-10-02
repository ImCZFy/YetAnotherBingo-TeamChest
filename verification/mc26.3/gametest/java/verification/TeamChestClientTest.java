package verification;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.JsonOps;
import me.chengzhify.yetanotherbingoteamchest.TeamChestConfig;
import me.chengzhify.yetanotherbingoteamchest.YetAnotherBingoAPIImpl;
import me.chengzhify.yetanotherbingoteamchest.adapter.impl.TeamChestStateImpl;
import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.BingoEvents;
import me.jfenn.bingo.api.internal.ExtensionManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.SavedDataStorage;

public class TeamChestClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            KeyMapping key = KeyMapping.get("key.yetanotherbingo-teamchest.open");
            require(key != null, "Team chest key binding registered");
            require(key.getDefaultKey().getType() == InputConstants.Type.KEYBOARD, "SDL keyboard input type");
            require(key.getDefaultKey().getValue() == InputConstants.KEY_B, "Default B key");
        });
        try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
            world.getServer().waitFor(server -> BingoApi.getINSTANCE() != null);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = player(server);
                require(execute(server, player, "teamchest") == 0, "Chest unavailable before Bingo starts");
                require(execute(server, player, "teamchest config") == 1, "Config opens before Bingo starts");
            });
            context.waitForScreen(ContainerScreen.class);
            context.takeScreenshot("teamchest-config-mc26.3");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = player(server);
                int before = TeamChestConfig.getRows();
                player.containerMenu.clicked(16, 0, ContainerInput.PICKUP, player);
                require(TeamChestConfig.getRows() == before % 6 + 1, "Config rows button");
                TeamChestConfig.setRows(3);
                player.containerMenu.clicked(10, 0, ContainerInput.PICKUP, player);
                require(!TeamChestConfig.isTeamChestEnabled(), "Config chest toggle");
                player.containerMenu.clicked(10, 0, ContainerInput.PICKUP, player);
                TeamChestConfig.setCountForBingoEnabled(true);
                TeamChestConfig.setTeamTeleportEnabled(true);
                player.closeContainer();
                var denied = player.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
                try {
                    server.getCommands().getDispatcher().execute("teamchest config", denied);
                    throw new AssertionError("Non-operator opened config");
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {
                    require(true, "Config requires operator permission");
                }
                require(execute(server, player, "join red") == 1, "Join a real Bingo team");
                require(execute(server, player, "bingo start ignore_warnings") == 1, "Start real Bingo game");
            });
            context.waitFor(client -> client.gui.screen() == null);
            world.getServer().waitFor(server -> YetAnotherBingoAPIImpl.isStarted(), 1200);
            context.getInput().pressKey(InputConstants.KEY_B);
            context.waitForScreen(ContainerScreen.class);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = player(server);
                String teamId = YetAnotherBingoAPIImpl.getTeamId(player.getUUID());
                require(teamId != null, "Bingo team resolved through API 2.14.0");
                var state = TeamChestStateImpl.getServerState(server);
                var chest = state.getInventory(teamId);
                require(player.containerMenu instanceof ChestMenu menu && menu.getContainer() == chest,
                        "B key opens the persistent team chest");
                require(chest.getContainerSize() == 27, "Three rows in team chest");
                chest.setItem(0, new ItemStack(Items.DIAMOND, 7));
                chest.setItem(26, new ItemStack(Items.POPLAR_LOG, 2));
                var provider = ExtensionManager.INSTANCE.getInventoryProviders().stream()
                        .filter(p -> p.getClass().getName().contains("TeamChestInventoryProvider")).findFirst().orElseThrow();
                var inventories = provider.getInventories(player);
                require(inventories.size() == 1, "Bingo receives team chest as an extra inventory");
                require(inventories.getFirst().removeItem(0, 2).getCount() == 2 && chest.getItem(0).getCount() == 5,
                        "Bingo inventory consumption updates the shared chest");
                require(state.isDirty(), "Inventory mutation marks saved data dirty");
                TeamChestConfig.setCountForBingoEnabled(false);
                require(provider.getInventories(player).isEmpty(), "Scoring toggle disables inventory provider");
                TeamChestConfig.setCountForBingoEnabled(true);
                var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
                var encoded = TeamChestStateImpl.CODEC.encodeStart(ops, state).getOrThrow();
                var decoded = TeamChestStateImpl.CODEC.parse(ops, encoded).getOrThrow();
                require(decoded.getInventory(teamId).getItem(0).getCount() == 5
                        && decoded.getInventory(teamId).getItem(26).is(Items.POPLAR_LOG)
                        && decoded.getInventory(teamId).getItem(1).isEmpty(),
                        "Chest codec preserves counts, empty slots and new 26.3 items");
                try {
                    var directory = java.nio.file.Files.createTempDirectory("teamchest-26.3-data-");
                    try (var storage = new SavedDataStorage(directory, DataFixers.getDataFixer(), server.registryAccess())) {
                        storage.set(TeamChestStateImpl.TYPE, state);
                        state.setDirty();
                        storage.saveAndJoin();
                    }
                    try (var storage = new SavedDataStorage(directory, DataFixers.getDataFixer(), server.registryAccess())) {
                        var restored = storage.get(TeamChestStateImpl.TYPE);
                        require(restored != null && restored.getInventory(teamId).getItem(0).getCount() == 5
                                        && restored.getInventory(teamId).getItem(26).is(Items.POPLAR_LOG),
                                "SavedDataStorage reloads chest items from disk");
                    }
                } catch (java.io.IOException e) { throw new AssertionError(e); }
                player.containerMenu.broadcastChanges();
                require(execute(server, player, "teamchest config") == 0, "Config command locked during Bingo game");
                require(execute(server, player, "teamchest toggle") == 0, "Chest toggle locked during Bingo game");
                require(execute(server, player, "tptoggle") == 0, "Teleport toggle locked during Bingo game");
                require(execute(server, player, "teamtp " + player.getName().getString()) == 0, "Self-teleport rejected");
            });
            world.getConnection().waitForClientboundPackets();
            context.takeScreenshot("teamchest-open-mc26.3");
            world.getServer().runOnServer(server -> {
                ServerPlayer player = player(server);
                player.closeContainer();
                require(execute(server, player, "tc") == 1, "Chest command alias opens the chest");
                player.closeContainer();
                BingoEvents.GAME_RESET.invoke(null);
                require(TeamChestStateImpl.getServerState(server)
                        .getInventory(YetAnotherBingoAPIImpl.getTeamId(player.getUUID())).isEmpty(),
                        "Bingo reset hook clears team chest items");
            });
            System.out.println("TEAMCHEST_CLIENT_SMOKE_PASSED");
        }
    }
    static ServerPlayer player(MinecraftServer server) { return server.getPlayerList().getPlayers().getFirst(); }
    static int execute(MinecraftServer server, ServerPlayer player, String command) {
        try {
            return server.getCommands().getDispatcher().execute(command,
                    player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) { throw new AssertionError(e); }
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("TEAMCHEST_CHECK: " + message);
    }
}
