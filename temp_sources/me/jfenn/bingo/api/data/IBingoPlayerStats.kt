package me.jfenn.bingo.api.data

import java.time.Duration

interface IBingoPlayerStats {
    val totalPlaytime: Duration
    val totalItems: Long
    val totalGames: Long
    val totalWins: Long
    val totalLosses: Long
    val monthlyPlaytime: Duration
    val monthlyItems: Long
    val monthlyGames: Long
    val monthlyWins: Long
    val monthlyLosses: Long
    val favoriteTeam: String?
    val favoriteTeamPercentage: Float
    val winStreak: Long
    val winStreakBest: Long?
}