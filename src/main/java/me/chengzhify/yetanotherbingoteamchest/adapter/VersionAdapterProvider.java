package me.chengzhify.yetanotherbingoteamchest.adapter;

import net.fabricmc.loader.api.FabricLoader;

public final class VersionAdapterProvider {

    private static final VersionAdapter INSTANCE = create();

    private static VersionAdapter create() {
        String mc = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .flatMap(c -> c.getMetadata().getVersion().getFriendlyString().describeConstable())
                .orElse("unknown");

        try {
            if (mc.startsWith("1.20")) {
                return (VersionAdapter) Class.forName("me.chengzhify.yetanotherbingoteamchest.adapter.v120.VersionAdapter120")
                        .getConstructor().newInstance();
            }

            if (mc.startsWith("1.21")) {
                return (VersionAdapter) Class.forName("me.chengzhify.yetanotherbingoteamchest.adapter.v121.VersionAdapter121")
                        .getConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate version adapter for version: " + mc, e);
        }

        throw new IllegalStateException("Unsupported Minecraft version: " + mc);
    }

    public static VersionAdapter get() {
        return INSTANCE;
    }

    private VersionAdapterProvider() {}
}
