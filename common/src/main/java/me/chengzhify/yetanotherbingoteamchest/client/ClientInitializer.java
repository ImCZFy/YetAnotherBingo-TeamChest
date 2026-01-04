package me.chengzhify.yetanotherbingoteamchest.client;

import me.chengzhify.yetanotherbingoteamchest.client.adapter.ClientVersionAdapter;
import me.chengzhify.yetanotherbingoteamchest.client.adapter.ClientVersionAdapterProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ClientInitializer implements ClientModInitializer {

    private ClientVersionAdapter versionAdapter = ClientVersionAdapterProvider.get();

    @Override
    public void onInitializeClient() {
        versionAdapter.registerKeyBinding();
    }
}
