package me.chengzhify.yetanotherbingoteamchest.adapter.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chengzhify.yetanotherbingoteamchest.TeamChestConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamChestStateImpl extends SavedData {

    private final Map<String, SimpleContainer> teamInventories = new HashMap<>();

    public TeamChestStateImpl() {}

    public TeamChestStateImpl(Map<String, List<ItemStack>> data) {
        data.forEach((teamId, items) -> {
            SimpleContainer inv = this.getInventory(teamId);
            for (int i = 0; i < Math.min(items.size(), inv.getContainerSize()); i++) {
                inv.setItem(i, items.get(i));
            }
        });
    }

    public SimpleContainer getInventory(String teamId) {
        return teamInventories.computeIfAbsent(teamId, id -> {
            SimpleContainer inv = new SimpleContainer(TeamChestConfig.getSize()) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    TeamChestStateImpl.this.setDirty();
                }
            };
            return inv;
        });
    }

    public static final Codec<TeamChestStateImpl> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(
                            Codec.STRING,
                            ItemStack.OPTIONAL_CODEC.listOf()
                    ).fieldOf("inventories").forGetter(state ->
                            state.teamInventories.entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> e.getValue().getItems()
                                    ))
                    )
            ).apply(instance, TeamChestStateImpl::new)
    );


    public static final SavedDataType<TeamChestStateImpl> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("yetanotherbingo-teamchest", "teamchests"),
            TeamChestStateImpl::new,
            CODEC,
            null
    );

    public static TeamChestStateImpl getServerState(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }


    public void clearAll() {
        teamInventories.values().forEach(SimpleContainer::clearContent);
        teamInventories.clear();
        this.setDirty();
    }

}
