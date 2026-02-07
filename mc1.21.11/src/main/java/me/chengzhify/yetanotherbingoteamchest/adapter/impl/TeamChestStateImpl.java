package me.chengzhify.yetanotherbingoteamchest.adapter.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chengzhify.yetanotherbingoteamchest.TeamChestConfig;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamChestStateImpl extends PersistentState {

    private final Map<String, SimpleInventory> teamInventories = new HashMap<>();

    public TeamChestStateImpl() {}

    public TeamChestStateImpl(Map<String, List<ItemStack>> data) {
        data.forEach((teamId, items) -> {
            SimpleInventory inv = this.getInventory(teamId);
            for (int i = 0; i < Math.min(items.size(), inv.size()); i++) {
                inv.setStack(i, items.get(i));
            }
        });
    }

    public SimpleInventory getInventory(String teamId) {
        return teamInventories.computeIfAbsent(teamId, id -> {
            SimpleInventory inv = new SimpleInventory(TeamChestConfig.getSize());
            inv.addListener(sender -> this.markDirty());
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
                                            e -> e.getValue().getHeldStacks()
                                    ))
                    )
            ).apply(instance, TeamChestStateImpl::new)
    );


    public static final PersistentStateType<TeamChestStateImpl> TYPE = new PersistentStateType<>(
            "yetanotherbingo_teamchests",
            TeamChestStateImpl::new,
            CODEC,
            null
    );

    public static TeamChestStateImpl getServerState(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD)
                .getPersistentStateManager()
                .getOrCreate(TYPE);
    }


    public void clearAll() {
        teamInventories.values().forEach(SimpleInventory::clear);
        teamInventories.clear();
        this.markDirty();
    }

}
