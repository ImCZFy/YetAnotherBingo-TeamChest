package me.chengzhify.yetanotherbingoteamchest.adapter.impl;

import me.chengzhify.yetanotherbingoteamchest.YetAnotherBingoAPIImpl;
import me.chengzhify.yetanotherbingoteamchest.TeamChestConfig;
import me.chengzhify.yetanotherbingoteamchest.adapter.VersionAdapter;
import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.provider.IInventoryProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.List;

public class BingoVersionAdapterImpl implements VersionAdapter {

    private boolean inventoryProviderRegistered = false;

    public Container createTeamInventory() {
        return new SimpleContainer(TeamChestConfig.getSize());
    }

    public void clearAllTeamInventories(MinecraftServer server) {
        TeamChestStateImpl.getServerState(server).clearAll();
    }

    public Container getTeamInventory(MinecraftServer server, String teamId) {
        return TeamChestStateImpl.getServerState(server).getInventory(teamId);
    }

    public void openTeamChest(ServerPlayer player, Container inventory, Component title) {
        player.openMenu(
                new SimpleMenuProvider(
                        (syncId, inv, p) ->
                                createScreenHandler(syncId, inv, inventory),
                        title
                )
        );
    }

    public void openConfigMenu(ServerPlayer player) {
        player.openMenu(TeamChestConfigMenu.provider());
    }

    public void registerBingoInventoryProvider() {
        if (inventoryProviderRegistered) {
            return;
        }

        BingoApi.registerInventoryProvider(new TeamChestInventoryProvider());
        inventoryProviderRegistered = true;
    }

    private ChestMenu createScreenHandler(int syncId, Inventory playerInventory, Container chestInventory) {
        int rows = TeamChestConfig.getRows();
        MenuType<ChestMenu> type = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };

        return new ChestMenu(type, syncId, playerInventory, chestInventory, rows);
    }

    public Component literal(String text) {
        return Component.literal(text);
    }

    public Component translatable(String key, String defaultText, Object... args) {
        return Component.translatableWithFallback(key, defaultText, args);
    }

    private static final class TeamChestInventoryProvider implements IInventoryProvider {
        @Override
        public List<Inventory> getInventories(ServerPlayer player) {
            if (!TeamChestConfig.isTeamChestEnabled() || !TeamChestConfig.isCountForBingoEnabled()) {
                return List.of();
            }

            MinecraftServer server = player.level().getServer();
            if (server == null || !YetAnotherBingoAPIImpl.isStarted()) {
                return List.of();
            }

            String teamId = YetAnotherBingoAPIImpl.getTeamId(player.getUUID());
            if (teamId == null) {
                return List.of();
            }

            Container inventory = TeamChestStateImpl.getServerState(server).getInventory(teamId);
            return List.of(new TeamChestInventoryAdapter(player, inventory));
        }
    }
}
