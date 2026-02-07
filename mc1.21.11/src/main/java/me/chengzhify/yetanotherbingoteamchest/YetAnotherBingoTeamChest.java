package me.chengzhify.yetanotherbingoteamchest;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import me.chengzhify.yetanotherbingoteamchest.adapter.VersionAdapter;
import me.chengzhify.yetanotherbingoteamchest.adapter.VersionAdapterProvider;
import me.jfenn.bingo.api.BingoEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.EnumSet;

public class YetAnotherBingoTeamChest implements ModInitializer {


    private static MinecraftServer server;
    private static boolean tcEnabled = true;
    private static boolean tpEnabled = true;

    private static VersionAdapter ADAPTER;

    @Override
    public void onInitialize() {
        ADAPTER = VersionAdapterProvider.get();
        TeamChestConfig.load();
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
                                            .requires(src -> src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(2))))
                                            .executes(this::tcToggle)
                            )
            );

            dispatcher.register(
                    CommandManager.literal("tc")
                            .executes(this::openTeamChest)
                            .then(
                                    CommandManager.literal("toggle")
                                            .requires(src -> src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(2))))
                                            .executes(this::tcToggle)
                            )
            );

            dispatcher.register(
                    CommandManager.literal("tptoggle")
                            .requires(src -> src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(2))))
                            .executes(this::tpToggle)
            );

            dispatcher.register(
                    CommandManager.literal("teamtp")
                            .executes(context -> {
                                if (!(context.getSource().getEntity() instanceof ServerPlayerEntity)) {
                                    context.getSource().sendError(
                                            Text.translatableWithFallback("yetanotherbingo-teamchest.error.players_only", "Only players can use this command!")
                                                    .formatted(Formatting.RED)
                                    );
                                    return 0;
                                }
                                context.getSource().sendError(
                                        Text.translatableWithFallback("yetanotherbingo-teamchest.error.usage", "Usage: /%s <player>", "teamtp")
                                                .formatted(Formatting.RED)
                                );
                                return 0;
                            })
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(context -> {
                                if (!(context.getSource().getEntity() instanceof ServerPlayerEntity)) {
                                    context.getSource().sendError(
                                            Text.translatableWithFallback("yetanotherbingo-teamchest.error.players_only", "Only players can use this command!")
                                                    .formatted(Formatting.RED)
                                    );
                                    return 0;
                                }
                                if (!YetAnotherBingoAPIImpl.isStarted()) {
                                    context.getSource().sendError(
                                            Text.translatableWithFallback("yetanotherbingo-teamchest.error.game_not_started", "The game hasn't started yet!")
                                                    .formatted(Formatting.RED)
                                    );
                                    return 0;
                                }
                                if (!tpEnabled) {
                                    context.getSource().sendError(
                                            Text.translatableWithFallback("yetanotherbingo-teamchest.error.tpdisabled", "Team Teleport is now disabled!")
                                                    .formatted(Formatting.RED)
                                    );
                                    return 0;
                                }
                                ServerPlayerEntity sender = context.getSource().getPlayer();
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                if (YetAnotherBingoAPIImpl.isInTeam(sender.getUuid())) {
                                    if (sender.getUuid().equals(target.getUuid())) {
                                        context.getSource().sendError(
                                                Text.translatableWithFallback("yetanotherbingo-teamchest.error.teleport_self", "You cannot teleport to yourself!")
                                                        .formatted(Formatting.RED)
                                        );
                                        return 0;
                                    }
                                    if (YetAnotherBingoAPIImpl.isInTheSameTeam(sender.getUuid(), target.getUuid())) {
                                        sender.teleport(target.getEntityWorld(),
                                                target.getX(),
                                                target.getY(),
                                                target.getZ(),
                                                EnumSet.noneOf(PositionFlag.class),
                                                target.getYaw(),
                                                target.getPitch(),
                                                true);
                                        context.getSource().sendFeedback(
                                                () -> Text.translatableWithFallback("yetanotherbingo-teamchest.message.team_teleport_success", "Teleported to team member %s.", target.getName().getString())
                                                        .formatted(Formatting.GREEN),
                                                false
                                        );
                                        target.sendMessage(Text.translatableWithFallback("yetanotherbingo-teamchest.message.team_teleport_notice", "%s has teleported to you.", sender.getName().getString()).formatted(Formatting.GREEN), false);
                                        return Command.SINGLE_SUCCESS;
                                    } else {
                                        context.getSource().sendError(
                                                Text.translatableWithFallback("yetanotherbingo-teamchest.error.not_in_same_team", "%s is not in your team!", target.getName().getString())
                                                        .formatted(Formatting.RED)
                                        );
                                        return 0;
                                    }
                                } else {
                                    context.getSource().sendError(
                                            Text.translatableWithFallback("yetanotherbingo-teamchest.error.no_team", "Cannot find your team!!")
                                                    .formatted(Formatting.RED)
                                    );
                                    return 0;
                                }
                            }))
            );

            dispatcher.register(
                    CommandManager.literal("ttp")
                            .executes(context -> {
                                if (!(context.getSource().getEntity() instanceof ServerPlayerEntity)) {
                                    context.getSource().sendError(
                                            Text.translatableWithFallback("yetanotherbingo-teamchest.error.players_only", "Only players can use this command!")
                                                    .formatted(Formatting.RED)
                                    );
                                    return 0;
                                }
                                context.getSource().sendError(
                                        Text.translatableWithFallback("yetanotherbingo-teamchest.error.usage", "Usage: /%s <player>", "ttp")
                                                .formatted(Formatting.RED)
                                );
                                return 0;
                            })
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                    .executes(context -> {
                                        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity)) {
                                            context.getSource().sendError(
                                                    Text.translatableWithFallback("yetanotherbingo-teamchest.error.players_only", "Only players can use this command!")
                                                            .formatted(Formatting.RED)
                                            );
                                            return 0;
                                        }
                                        if (!YetAnotherBingoAPIImpl.isStarted()) {
                                            context.getSource().sendError(
                                                    Text.translatableWithFallback("yetanotherbingo-teamchest.error.game_not_started", "The game hasn't started yet!")
                                                            .formatted(Formatting.RED)
                                            );
                                            return 0;
                                        }
                                        if (!tpEnabled) {
                                            context.getSource().sendError(
                                                    Text.translatableWithFallback("yetanotherbingo-teamchest.error.tpdisabled", "Team Teleport is now disabled!")
                                                            .formatted(Formatting.RED)
                                            );
                                            return 0;
                                        }
                                        ServerPlayerEntity sender = context.getSource().getPlayer();
                                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                        if (YetAnotherBingoAPIImpl.isInTeam(sender.getUuid())) {
                                            if (sender.getUuid().equals(target.getUuid())) {
                                                context.getSource().sendError(
                                                        Text.translatableWithFallback("yetanotherbingo-teamchest.error.teleport_self", "You cannot teleport to yourself!")
                                                                .formatted(Formatting.RED)
                                                );
                                                return 0;
                                            }
                                            if (YetAnotherBingoAPIImpl.isInTheSameTeam(sender.getUuid(), target.getUuid())) {
                                                sender.teleport(target.getEntityWorld(),
                                                        target.getX(),
                                                        target.getY(),
                                                        target.getZ(),
                                                        EnumSet.noneOf(PositionFlag.class),
                                                        target.getYaw(),
                                                        target.getPitch(),
                                                        true);
                                                context.getSource().sendFeedback(
                                                        () -> Text.translatableWithFallback("yetanotherbingo-teamchest.message.team_teleport_success", "Teleported to team member %s.", target.getName().getString())
                                                                .formatted(Formatting.GREEN),
                                                        false
                                                );
                                                target.sendMessage(Text.translatableWithFallback("yetanotherbingo-teamchest.message.team_teleport_notice", "%s has teleported to you.", sender.getName().getString()).formatted(Formatting.GREEN), false);
                                                return Command.SINGLE_SUCCESS;
                                            } else {
                                                context.getSource().sendError(
                                                        Text.translatableWithFallback("yetanotherbingo-teamchest.error.not_in_same_team", "%s is not in your team!", target.getName().getString())
                                                                .formatted(Formatting.RED)
                                                );
                                                return 0;
                                            }
                                        } else {
                                            context.getSource().sendError(
                                                    Text.translatableWithFallback("yetanotherbingo-teamchest.error.no_team", "Cannot find your team!!")
                                                            .formatted(Formatting.RED)
                                            );
                                            return 0;
                                        }
                                    }))
            );
        });
    }

    private void registerBingoHooks() {
        BingoEvents.GAME_RESET.register((e) -> {
            if (server != null) {
                ADAPTER.clearAllTeamInventories(server);
            }
        });
    }

    private int tcToggle(CommandContext<ServerCommandSource> ctx) {
        tcEnabled = !tcEnabled;

        ctx.getSource().sendFeedback(
                () -> Text.translatableWithFallback("yetanotherbingo-teamchest.message.toggle", "Team chest is now ")
                        .formatted(Formatting.GRAY)
                        .append(
                                Text.translatableWithFallback(
                                        tcEnabled
                                                ? "yetanotherbingo-teamchest.message.enabled"
                                                : "yetanotherbingo-teamchest.message.disabled",
                                        tcEnabled ? "enabled" : "disabled"
                                ).formatted(tcEnabled ? Formatting.GREEN : Formatting.RED)),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int tpToggle(CommandContext<ServerCommandSource> ctx) {
        tpEnabled = !tpEnabled;

        ctx.getSource().sendFeedback(
                () -> Text.translatableWithFallback("yetanotherbingo-teamchest.message.tptoggle", "Team teleport is now ")
                        .formatted(Formatting.GRAY)
                        .append(
                                Text.translatableWithFallback(
                                        tpEnabled
                                                ? "yetanotherbingo-teamchest.message.enabled"
                                                : "yetanotherbingo-teamchest.message.disabled",
                                        tpEnabled ? "enabled" : "disabled"
                                ).formatted(tpEnabled ? Formatting.GREEN : Formatting.RED)),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int openTeamChest(CommandContext<ServerCommandSource> ctx) {

        ServerCommandSource source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(
                    Text.translatableWithFallback("yetanotherbingo-teamchest.error.players_only", "Only players can use this command!")
                            .formatted(Formatting.RED)
            );
            return 0;
        }

        if (!tcEnabled) {
            source.sendError(
                    Text.translatableWithFallback("yetanotherbingo-teamchest.error.disabled", "Team chest is now disabled!")
                            .formatted(Formatting.RED)
            );
            return 0;
        }

        if (!YetAnotherBingoAPIImpl.isStarted()) {
            source.sendError(
                    Text.translatableWithFallback("yetanotherbingo-teamchest.error.game_not_started", "The game hasn't started yet!")
                            .formatted(Formatting.RED)
            );
            return 0;
        }

        if (!YetAnotherBingoAPIImpl.isInTeam(player.getUuid())) {
            source.sendError(
                    Text.translatableWithFallback("yetanotherbingo-teamchest.error.no_team", "Cannot find your team!")
                            .formatted(Formatting.RED)
            );
            return 0;
        }

        String teamId = YetAnotherBingoAPIImpl.getTeamId(player.getUuid());
        if (teamId == null) {
            source.sendError(
                    Text.translatableWithFallback("yetanotherbingo-teamchest.error.no_team_id", "Cannot fetch your team ID!")
                            .formatted(Formatting.RED)
            );
            return 0;
        }

        Inventory inventory = ADAPTER.getTeamInventory(source.getServer(), teamId);

        ADAPTER.openTeamChest(player, inventory, Text.translatableWithFallback("yetanotherbingo-teamchest.container.team_chest", "Team Chest"));
        return Command.SINGLE_SUCCESS;
    }
}
