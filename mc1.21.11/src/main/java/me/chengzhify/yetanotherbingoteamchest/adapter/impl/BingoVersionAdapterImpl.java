package me.chengzhify.yetanotherbingoteamchest.adapter.impl;

import me.chengzhify.yetanotherbingoteamchest.TeamChestConfig;
import me.chengzhify.yetanotherbingoteamchest.adapter.VersionAdapter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.PersistentState;

public class BingoVersionAdapterImpl implements VersionAdapter {

    public Inventory createTeamInventory() {
        return new SimpleInventory(TeamChestConfig.getSize());
    }

    public void clearAllTeamInventories(MinecraftServer server) {
        TeamChestStateImpl.getServerState(server).clearAll();
    }

    public Inventory getTeamInventory(MinecraftServer server, String teamId) {
        return TeamChestStateImpl.getServerState(server).getInventory(teamId);
    }

    public void openTeamChest(ServerPlayerEntity player, Inventory inventory, Text title) {
        player.openHandledScreen(
                new SimpleNamedScreenHandlerFactory(
                        (syncId, inv, p) ->
                                createScreenHandler(syncId, inv, inventory),
                        title
                )
        );
    }

    private GenericContainerScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory, Inventory chestInventory) {
        int rows = TeamChestConfig.getRows();
        ScreenHandlerType<GenericContainerScreenHandler> type = switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            case 6 -> ScreenHandlerType.GENERIC_9X6;
            default -> ScreenHandlerType.GENERIC_9X3;
        };

        return new GenericContainerScreenHandler(type, syncId, playerInventory, chestInventory, rows);
    }

    public Text literal(String text) {
        return Text.literal(text);
    }

    public Text translatable(String key, String defaultText, Object... args) {
        return Text.translatableWithFallback(key, defaultText, args);
    }


}
