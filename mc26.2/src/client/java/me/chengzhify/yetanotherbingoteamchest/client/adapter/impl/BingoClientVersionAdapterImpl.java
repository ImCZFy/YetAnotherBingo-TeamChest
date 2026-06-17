package me.chengzhify.yetanotherbingoteamchest.client.adapter.impl;

import me.chengzhify.yetanotherbingoteamchest.client.adapter.ClientVersionAdapter;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class BingoClientVersionAdapterImpl implements ClientVersionAdapter {

    public void registerKeyBinding() {

        KeyMapping openTeamChestKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.yetanotherbingo-teamchest.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KeyMapping.Category.GAMEPLAY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openTeamChestKey.consumeClick()) {
                if (client.player != null) {
                    client.player.connection.sendCommand("teamchest");
                }
            }
        });
    }
}




