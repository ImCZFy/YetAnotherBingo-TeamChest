package me.chengzhify.yetanotherbingoteamchest;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import me.chengzhify.yetanotherbingoteamchest.adapter.VersionAdapter;
import me.chengzhify.yetanotherbingoteamchest.adapter.VersionAdapterProvider;
import me.jfenn.bingo.api.BingoEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.text.Text;
import net.minecraft.util.Clearable;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

public class YetAnotherBingoTeamChest implements ModInitializer {

    private static final Map<String, SimpleInventory> TEAM_INVENTORIES = new HashMap<>();

    private static MinecraftServer server;
    private static boolean enabled = true;

    private static VersionAdapter ADAPTER;

    @Override
    public void onInitialize() {
        ADAPTER = VersionAdapterProvider.get();

        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);

        registerCommands();
        registerBingoHooks();
    }


    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(
                    CommandManager.literal("teamchest")
                            .executes(this::openTeamChest)
                            .then(
                                    CommandManager.literal("toggle")
                                            .requires(src -> src.hasPermissionLevel(2))
                                            .executes(this::toggle)
                            )
            );

            dispatcher.register(
                    CommandManager.literal("tc")
                            .executes(this::openTeamChest)
                            .then(
                                    CommandManager.literal("toggle")
                                            .requires(src -> src.hasPermissionLevel(2))
                                            .executes(this::toggle)
                            )
            );
        });
    }

    private void registerBingoHooks() {
        BingoEvents.GAME_ENDED.register((e) -> {
            TEAM_INVENTORIES.values().forEach(c -> c.clear());
            TEAM_INVENTORIES.clear();
        });
        BingoEvents.GAME_RESET.register((e) -> {
            TEAM_INVENTORIES.values().forEach(c -> c.clear());
            TEAM_INVENTORIES.clear();
        });
    }

    private int toggle(CommandContext<ServerCommandSource> ctx) {
        enabled = !enabled;

        ctx.getSource().sendFeedback(
                () -> Text.translatable("yetanotherbingo-teamchest.message.toggle", "Team chest is now ")
                        .formatted(Formatting.GRAY)
                        .append(
                                Text.translatable(
                        enabled
                                ? "yetanotherbingo-teamchest.message.enabled"
                                : "yetanotherbingo-teamchest.message.disabled",
                        enabled ? "enabled" : "disabled"
                ).formatted(enabled ? Formatting.GREEN : Formatting.RED)),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int openTeamChest(CommandContext<ServerCommandSource> ctx) {

        ServerCommandSource source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(
                    Text.translatable("yetanotherbingo-teamchest.error.players_only", "Only players can use this command!")
                            .formatted(Formatting.RED)
            );
            return 0;
        }

        if (!enabled) {
            source.sendError(
                    Text.translatable("yetanotherbingo-teamchest.error.disabled", "Team chest is now disabled!")
                            .formatted(Formatting.RED)
            );
            return 0;
        }

        if (!YetAnotherBingoAPIImpl.isStarted()) {
            source.sendError(
                    Text.translatable("yetanotherbingo-teamchest.error.game_not_started", "The game hasn't started yet!")
                            .formatted(Formatting.RED)
            );
            return 0;
        }

        if (!YetAnotherBingoAPIImpl.isInTeam(player.getUuid())) {
            source.sendError(
                    Text.translatable("yetanotherbingo-teamchest.error.no_team", "Cannot find your team!")
                            .formatted(Formatting.RED)
            );
            return 0;
        }

        String teamId = YetAnotherBingoAPIImpl.getTeamId(player.getUuid());
        if (teamId == null) {
            source.sendError(
                    Text.translatable("yetanotherbingo-teamchest.error.no_team_id", "Cannot fetch your team ID!")
                            .formatted(Formatting.RED)
            );
            return 0;
        }

        Inventory inventory =
                TEAM_INVENTORIES.computeIfAbsent(teamId, id -> new SimpleInventory(27));

        ADAPTER.openTeamChest(
                player,
                inventory,
                Text.literal("Team Chest")
        );
        return Command.SINGLE_SUCCESS;
    }
}

