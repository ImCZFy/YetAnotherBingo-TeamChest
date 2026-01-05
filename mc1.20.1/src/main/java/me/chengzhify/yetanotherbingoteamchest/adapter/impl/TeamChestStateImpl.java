package me.chengzhify.yetanotherbingoteamchest.adapter.impl;

import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

public class TeamChestStateImpl extends PersistentState {
    private final Map<String, SimpleInventory> inventories = new HashMap<>();

    public SimpleInventory getInventory(String teamId) {
        return inventories.computeIfAbsent(teamId, id -> {
            SimpleInventory inv = new SimpleInventory(27);
            inv.addListener(i -> this.markDirty());
            return inv;
        });
    }

    public static TeamChestStateImpl fromNbt(NbtCompound nbt) {
        TeamChestStateImpl state = new TeamChestStateImpl();
        NbtCompound teamsTag = nbt.getCompound("TeamInventories");
        for (String teamId : teamsTag.getKeys()) {
            SimpleInventory inv = state.getInventory(teamId);
            DefaultedList<ItemStack> stacks = DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
            Inventories.readNbt(teamsTag.getCompound(teamId), stacks);
            for (int i = 0; i < stacks.size(); i++) inv.setStack(i, stacks.get(i));
        }
        return state;
    }

    public void clearAll() {
        inventories.values().forEach(SimpleInventory::clear);
        inventories.clear();
        this.markDirty();
    }
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound teamsTag = new NbtCompound();
        inventories.forEach((id, inv) -> {
            NbtCompound invTag = new NbtCompound();
            DefaultedList<ItemStack> stacks = DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
            for (int i = 0; i < inv.size(); i++) stacks.set(i, inv.getStack(i));
            Inventories.writeNbt(invTag, stacks);
            teamsTag.put(id, invTag);
        });
        nbt.put("TeamInventories", teamsTag);
        return nbt;
    }

    public static TeamChestStateImpl getServerState(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                TeamChestStateImpl::fromNbt,
                TeamChestStateImpl::new,
                "yetanotherbingo_teamchests"
        );
    }
}