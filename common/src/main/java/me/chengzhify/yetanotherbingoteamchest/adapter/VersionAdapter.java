package me.chengzhify.yetanotherbingoteamchest.adapter;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.inventory.Inventory;
import net.minecraft.world.PersistentState;

public interface VersionAdapter {

    Inventory createTeamInventory();

    Inventory getTeamInventory(MinecraftServer server, String teamId);

    void clearAllTeamInventories(MinecraftServer server);

    void openTeamChest(ServerPlayerEntity player, Inventory inventory, Text title);

    Text literal(String text);

    Text translatable(String key, String defaultText, Object... args);

}
