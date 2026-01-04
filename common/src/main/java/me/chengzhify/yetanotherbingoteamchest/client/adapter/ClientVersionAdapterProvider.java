package me.chengzhify.yetanotherbingoteamchest.client.adapter;

import net.fabricmc.loader.api.FabricLoader;

public final class ClientVersionAdapterProvider {

    private static final ClientVersionAdapter INSTANCE = create();

    private static ClientVersionAdapter create() {
        try {
            return (ClientVersionAdapter) Class.forName("me.chengzhify.yetanotherbingoteamchest.client.adapter.impl.BingoClientVersionAdapterImpl")
                    .getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate client version adapter. Ensure you are running a supported version.", e);
        }
    }

    public static ClientVersionAdapter get() {
        return INSTANCE;
    }

    private ClientVersionAdapterProvider() {}
}

