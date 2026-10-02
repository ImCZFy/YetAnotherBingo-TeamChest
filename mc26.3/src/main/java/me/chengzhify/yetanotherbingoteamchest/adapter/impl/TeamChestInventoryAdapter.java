package me.chengzhify.yetanotherbingoteamchest.adapter.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TeamChestInventoryAdapter extends Inventory {

    private final Container backing;

    public TeamChestInventoryAdapter(ServerPlayer player, Container backing) {
        super(player, new EntityEquipment());
        this.backing = backing;
    }

    @Override
    public int getContainerSize() {
        return backing.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return backing.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return backing.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return backing.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        backing.setItem(slot, stack);
    }

    @Override
    public void setChanged() {
        backing.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return backing.stillValid(player);
    }

    @Override
    public void clearContent() {
        backing.clearContent();
    }
}
