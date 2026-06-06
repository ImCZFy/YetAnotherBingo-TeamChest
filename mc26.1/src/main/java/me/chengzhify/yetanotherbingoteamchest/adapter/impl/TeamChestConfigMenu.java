package me.chengzhify.yetanotherbingoteamchest.adapter.impl;

import me.chengzhify.yetanotherbingoteamchest.TeamChestConfig;
import me.chengzhify.yetanotherbingoteamchest.YetAnotherBingoAPIImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TeamChestConfigMenu {

    private static final int ROWS = 3;
    private static final int SIZE = ROWS * 9;
    private static final int SLOT_TEAM_CHEST = 10;
    private static final int SLOT_BINGO_SCORING = 12;
    private static final int SLOT_TEAM_TELEPORT = 14;
    private static final int SLOT_ROWS = 16;
    private static final int SLOT_CLOSE = 22;

    private TeamChestConfigMenu() {}

    public static SimpleMenuProvider provider() {
        return new SimpleMenuProvider(
                (syncId, inventory, player) -> new Menu(syncId, inventory, new SimpleContainer(SIZE)),
                Component.translatableWithFallback("yetanotherbingo-teamchest.config.title", "Team Chest Config")
        );
    }

    private static final class Menu extends ChestMenu {

        private final SimpleContainer menuInventory;

        private Menu(int syncId, Inventory playerInventory, SimpleContainer menuInventory) {
            super(MenuType.GENERIC_9x3, syncId, playerInventory, menuInventory, ROWS);
            this.menuInventory = menuInventory;
            refresh();
        }

        @Override
        public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
            if (slotId < 0) {
                super.clicked(slotId, button, clickType, player);
                return;
            }

            if (slotId < SIZE) {
                handleConfigClick(slotId, button, player);
                return;
            }

            if (clickType != ContainerInput.QUICK_MOVE) {
                super.clicked(slotId, button, clickType, player);
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        private void handleConfigClick(int slotId, int button, Player player) {
            if (slotId == SLOT_CLOSE) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.closeContainer();
                }
                return;
            }

            if (!YetAnotherBingoAPIImpl.isConfigEditable()) {
                player.sendSystemMessage(
                        Component.translatableWithFallback(
                                "yetanotherbingo-teamchest.error.config_locked",
                                "Configuration can only be changed before the Bingo game starts."
                        ).withStyle(ChatFormatting.RED)
                );
                refresh();
                broadcastChanges();
                return;
            }

            switch (slotId) {
                case SLOT_TEAM_CHEST -> TeamChestConfig.setTeamChestEnabled(!TeamChestConfig.isTeamChestEnabled());
                case SLOT_BINGO_SCORING -> TeamChestConfig.setCountForBingoEnabled(!TeamChestConfig.isCountForBingoEnabled());
                case SLOT_TEAM_TELEPORT -> TeamChestConfig.setTeamTeleportEnabled(!TeamChestConfig.isTeamTeleportEnabled());
                case SLOT_ROWS -> {
                    int delta = button == 1 ? -1 : 1;
                    int next = TeamChestConfig.getRows() + delta;
                    if (next > 6) next = 1;
                    if (next < 1) next = 6;
                    TeamChestConfig.setRows(next);
                }
                default -> {
                    return;
                }
            }

            refresh();
            broadcastChanges();
        }

        private void refresh() {
            menuInventory.clearContent();
            menuInventory.setItem(SLOT_TEAM_CHEST, toggleItem(
                    TeamChestConfig.isTeamChestEnabled(),
                    Items.CHEST,
                    "yetanotherbingo-teamchest.config.team_chest",
                    "Team Chest"
            ));
            menuInventory.setItem(SLOT_BINGO_SCORING, toggleItem(
                    TeamChestConfig.isCountForBingoEnabled(),
                    Items.TARGET,
                    "yetanotherbingo-teamchest.config.bingo_scoring",
                    "Count Chest Items for Bingo"
            ));
            menuInventory.setItem(SLOT_TEAM_TELEPORT, toggleItem(
                    TeamChestConfig.isTeamTeleportEnabled(),
                    Items.ENDER_PEARL,
                    "yetanotherbingo-teamchest.config.team_teleport",
                    "Team Teleport"
            ));
            menuInventory.setItem(SLOT_ROWS, namedItem(
                    Items.BARREL,
                    Component.translatableWithFallback(
                            "yetanotherbingo-teamchest.config.rows",
                            "Rows: %s",
                            TeamChestConfig.getRows()
                    ).withStyle(ChatFormatting.AQUA)
            ));
            menuInventory.setItem(SLOT_CLOSE, namedItem(
                    Items.BARRIER,
                    Component.translatableWithFallback("yetanotherbingo-teamchest.config.close", "Close")
                            .withStyle(ChatFormatting.RED)
            ));

            for (int i = 0; i < SIZE; i++) {
                getSlot(i).set(menuInventory.getItem(i));
            }
        }

        private static ItemStack toggleItem(boolean enabled, net.minecraft.world.item.Item item, String key, String fallback) {
            Component state = Component.translatableWithFallback(
                    enabled
                            ? "yetanotherbingo-teamchest.message.enabled"
                            : "yetanotherbingo-teamchest.message.disabled",
                    enabled ? "enabled" : "disabled"
            ).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);

            return namedItem(
                    item,
                    Component.translatableWithFallback(key, fallback)
                            .withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                            .append(state)
            );
        }

        private static ItemStack namedItem(net.minecraft.world.item.Item item, Component name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.CUSTOM_NAME, name);
            return stack;
        }
    }
}
