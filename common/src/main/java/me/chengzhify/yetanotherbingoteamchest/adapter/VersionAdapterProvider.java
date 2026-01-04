package me.chengzhify.yetanotherbingoteamchest.adapter;

import net.fabricmc.loader.api.FabricLoader;

public final class VersionAdapterProvider {

    private static final VersionAdapter INSTANCE = create();

    private static VersionAdapter create() {
        try {
            return (VersionAdapter) Class.forName("me.chengzhify.yetanotherbingoteamchest.adapter.impl.BingoVersionAdapterImpl")
                    .getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate version adapter. Ensure you are running a supported version.", e);
        }
    }

    public static VersionAdapter get() {
        return INSTANCE;
    }

    private VersionAdapterProvider() {}
}
