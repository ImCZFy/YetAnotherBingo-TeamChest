package me.chengzhify.yetanotherbingoteamchest.adapter;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;

public interface VersionAdapter {

    Container createTeamInventory();

    Container getTeamInventory(MinecraftServer server, String teamId);

    void clearAllTeamInventories(MinecraftServer server);

    void openTeamChest(ServerPlayer player, Container inventory, Component title);

    Component literal(String text);

    Component translatable(String key, String defaultText, Object... args);

}
