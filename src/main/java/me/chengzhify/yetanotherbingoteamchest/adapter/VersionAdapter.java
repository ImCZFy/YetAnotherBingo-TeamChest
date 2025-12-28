package me.chengzhify.yetanotherbingoteamchest.adapter;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.inventory.Inventory;

public interface VersionAdapter {

    Inventory createTeamInventory();

    void openTeamChest(ServerPlayerEntity player, Inventory inventory, Text title);

    Text literal(String text);

    Text translatable(String key);

}
