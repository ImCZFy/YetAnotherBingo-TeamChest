package me.chengzhify.yetanotherbingoteamchest;

import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.data.BingoGame;
import me.jfenn.bingo.api.data.BingoGameStatus;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class YetAnotherBingoAPIImpl {


    public static boolean isInTeam(UUID uuid) {

        if (!isStarted()) return false;

        for (IBingoTeam team : BingoApi.getTeams()) {
            for (UUID members : team.getPlayers()) {
                if (members.equals(uuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getTeamId(UUID uuid) {

        if (!isStarted()) return null;

        for (IBingoTeam team : BingoApi.getTeams()) {
            for (UUID members : team.getPlayers()) {
                if (members.equals(uuid)) {
                    return team.getId();
                }
            }
        }
        return null;
    }

    public static boolean isStarted() {

        BingoGame game = BingoApi.getGame();
        if (game != null) {
            return game.getStatus().equals(BingoGameStatus.PLAYING);
        }
        return false;
    }

    public static boolean isInTheSameTeam(UUID executor, UUID target) {
        if (isStarted()) {
            if (isInTeam(executor) && isInTeam(target)) {
                String executorTeamId = getTeamId(executor);
                String targetTeamId = getTeamId(target);
                return executorTeamId.equalsIgnoreCase(targetTeamId);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

}

