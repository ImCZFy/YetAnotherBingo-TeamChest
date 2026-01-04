package me.chengzhify.yetanotherbingoteamchest.adapter.impl;

import me.chengzhify.yetanotherbingoteamchest.adapter.VersionAdapter;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class BingoVersionAdapterImpl implements VersionAdapter {

    @Override
    public Inventory createTeamInventory() {
        return new SimpleInventory(27);
    }

    @Override
    public void openTeamChest(ServerPlayerEntity player, Inventory inventory, Text title) {
        player.openHandledScreen(
                new SimpleNamedScreenHandlerFactory(
                        (syncId, inv, p) ->
                                GenericContainerScreenHandler.createGeneric9x3(syncId, inv, inventory),
                        title
                )
        );
    }

    @Override
    public Text literal(String text) {
        return Text.literal(text);
    }

    @Override
    public Text translatable(String key, String defaultText, Object... args) {
        return Text.translatableWithFallback(key, defaultText, args);
    }
}

