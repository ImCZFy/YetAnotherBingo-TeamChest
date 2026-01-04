package me.jfenn.bingo.api

import me.jfenn.bingo.api.config.IBingoConfig
import me.jfenn.bingo.api.data.BingoGame
import me.jfenn.bingo.api.data.IBingoPlayerStats
import me.jfenn.bingo.api.data.IBingoTeams
import net.minecraft.server.MinecraftServer
import java.util.*

interface IBingoApi {
    val server: MinecraftServer
    val game: BingoGame
    val teams: IBingoTeams
    fun getPlayerStats(id: UUID): IBingoPlayerStats
    val config: IBingoConfig
}