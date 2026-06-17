package me.chengzhify.yetanotherbingoteamchest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import me.chengzhify.yetanotherbingoteamchest.adapter.VersionAdapter;
import me.chengzhify.yetanotherbingoteamchest.adapter.VersionAdapterProvider;
import me.jfenn.bingo.api.BingoEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Relative;

import java.util.Set;

public class YetAnotherBingoTeamChest implements ModInitializer {

    private static MinecraftServer server;

    private static VersionAdapter ADAPTER;

    @Override
    public void onInitialize() {
        ADAPTER = VersionAdapterProvider.get();
        TeamChestConfig.load();
        ADAPTER.registerBingoInventoryProvider();
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
        registerCommands();
        registerBingoHooks();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("teamchest")
                            .executes(this::openTeamChest)
                            .then(Commands.literal("toggle")
                                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                    .executes(this::tcToggle))
                            .then(Commands.literal("config")
                                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                    .executes(this::openConfigMenu))
            );

            dispatcher.register(
                    Commands.literal("tc")
                            .executes(this::openTeamChest)
                            .then(Commands.literal("toggle")
                                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                    .executes(this::tcToggle))
                            .then(Commands.literal("config")
                                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                    .executes(this::openConfigMenu))
            );

            dispatcher.register(
                    Commands.literal("tptoggle")
                            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                            .executes(this::tpToggle)
            );

            registerTeleportCommand(dispatcher, "teamtp");
            registerTeleportCommand(dispatcher, "ttp");
        });
    }

    private void registerTeleportCommand(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(
                Commands.literal(name)
                        .executes(context -> {
                            context.getSource().sendFailure(error("yetanotherbingo-teamchest.error.usage", "Usage: /%s <player>", name));
                            return 0;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> teamTeleport(context, EntityArgument.getPlayer(context, "target"))))
        );
    }

    private void registerBingoHooks() {
        BingoEvents.GAME_RESET.register((e) -> {
            if (server != null) {
                ADAPTER.clearAllTeamInventories(server);
            }
        });
    }

    private int tcToggle(CommandContext<CommandSourceStack> ctx) {
        if (!YetAnotherBingoAPIImpl.isConfigEditable()) {
            ctx.getSource().sendFailure(error("yetanotherbingo-teamchest.error.config_locked", "Configuration can only be changed before the Bingo game starts."));
            return 0;
        }

        boolean enabled = !TeamChestConfig.isTeamChestEnabled();
        TeamChestConfig.setTeamChestEnabled(enabled);

        ctx.getSource().sendSuccess(
                () -> Component.translatableWithFallback("yetanotherbingo-teamchest.message.toggle", "Team chest is now ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(
                                Component.translatableWithFallback(
                                        enabled
                                                ? "yetanotherbingo-teamchest.message.enabled"
                                                : "yetanotherbingo-teamchest.message.disabled",
                                        enabled ? "enabled" : "disabled"
                                ).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int tpToggle(CommandContext<CommandSourceStack> ctx) {
        if (!YetAnotherBingoAPIImpl.isConfigEditable()) {
            ctx.getSource().sendFailure(error("yetanotherbingo-teamchest.error.config_locked", "Configuration can only be changed before the Bingo game starts."));
            return 0;
        }

        boolean enabled = !TeamChestConfig.isTeamTeleportEnabled();
        TeamChestConfig.setTeamTeleportEnabled(enabled);

        ctx.getSource().sendSuccess(
                () -> Component.translatableWithFallback("yetanotherbingo-teamchest.message.tptoggle", "Team teleport is now ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(
                                Component.translatableWithFallback(
                                        enabled
                                                ? "yetanotherbingo-teamchest.message.enabled"
                                                : "yetanotherbingo-teamchest.message.disabled",
                                        enabled ? "enabled" : "disabled"
                                ).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int openConfigMenu(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(error("yetanotherbingo-teamchest.error.players_only", "Only players can use this command!"));
            return 0;
        }

        if (!YetAnotherBingoAPIImpl.isConfigEditable()) {
            ctx.getSource().sendFailure(error("yetanotherbingo-teamchest.error.config_locked", "Configuration can only be changed before the Bingo game starts."));
            return 0;
        }

        ADAPTER.openConfigMenu(player);
        return Command.SINGLE_SUCCESS;
    }

    private int teamTeleport(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        CommandSourceStack source = context.getSource();
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.players_only", "Only players can use this command!"));
            return 0;
        }

        if (!YetAnotherBingoAPIImpl.isStarted()) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.game_not_started", "The game hasn't started yet!"));
            return 0;
        }

        if (!TeamChestConfig.isTeamTeleportEnabled()) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.tpdisabled", "Team Teleport is now disabled!"));
            return 0;
        }

        if (!YetAnotherBingoAPIImpl.isInTeam(sender.getUUID())) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.no_team", "Cannot find your team!!"));
            return 0;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.teleport_self", "You cannot teleport to yourself!"));
            return 0;
        }

        if (!YetAnotherBingoAPIImpl.isInTheSameTeam(sender.getUUID(), target.getUUID())) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.not_in_same_team", "%s is not in your team!", target.getName().getString()));
            return 0;
        }

        sender.teleportTo(
                target.level(),
                target.getX(),
                target.getY(),
                target.getZ(),
                Set.<Relative>of(),
                target.getYRot(),
                target.getXRot(),
                true
        );
        source.sendSuccess(
                () -> Component.translatableWithFallback("yetanotherbingo-teamchest.message.team_teleport_success", "Teleported to team member %s.", target.getName().getString())
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        target.sendSystemMessage(
                Component.translatableWithFallback("yetanotherbingo-teamchest.message.team_teleport_notice", "%s has teleported to you.", sender.getName().getString())
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private int openTeamChest(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.players_only", "Only players can use this command!"));
            return 0;
        }

        if (!TeamChestConfig.isTeamChestEnabled()) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.disabled", "Team chest is now disabled!"));
            return 0;
        }

        if (!YetAnotherBingoAPIImpl.isStarted()) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.game_not_started", "The game hasn't started yet!"));
            return 0;
        }

        if (!YetAnotherBingoAPIImpl.isInTeam(player.getUUID())) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.no_team", "Cannot find your team!"));
            return 0;
        }

        String teamId = YetAnotherBingoAPIImpl.getTeamId(player.getUUID());
        if (teamId == null) {
            source.sendFailure(error("yetanotherbingo-teamchest.error.no_team_id", "Cannot fetch your team ID!"));
            return 0;
        }

        Container inventory = ADAPTER.getTeamInventory(source.getServer(), teamId);

        ADAPTER.openTeamChest(player, inventory, Component.translatableWithFallback("yetanotherbingo-teamchest.container.team_chest", "Team Chest"));
        return Command.SINGLE_SUCCESS;
    }

    private static Component error(String key, String defaultText, Object... args) {
        return Component.translatableWithFallback(key, defaultText, args).withStyle(ChatFormatting.RED);
    }
}
