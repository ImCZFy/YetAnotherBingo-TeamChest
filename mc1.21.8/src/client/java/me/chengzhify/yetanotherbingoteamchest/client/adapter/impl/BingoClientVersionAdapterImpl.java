package me.chengzhify.yetanotherbingoteamchest.client.adapter.impl;

import me.chengzhify.yetanotherbingoteamchest.client.adapter.ClientVersionAdapter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class BingoClientVersionAdapterImpl implements ClientVersionAdapter {

    public void registerKeyBinding() {

        KeyBinding openTeamChestKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yetanotherbingo-teamchest.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.yetanotherbingo-teamchest"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openTeamChestKey.wasPressed()) {
                if (client.player != null) {
                    client.player.networkHandler.sendChatCommand("teamchest");
                }
            }
        });
    }

}


