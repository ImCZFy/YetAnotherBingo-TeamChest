package me.jfenn.bingo.api.services

import me.jfenn.bingo.api.annotations.BingoInternalApi
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text

interface TextPlaceholderService {
    fun parseText(message: Text, server: MinecraftServer): Text
}

object TextPlaceholders {
    @BingoInternalApi
    @JvmStatic
    val services = mutableListOf<TextPlaceholderService>()

    @JvmStatic
    @OptIn(BingoInternalApi::class)
    fun register(service: TextPlaceholderService) {
        services.add(service)
    }
}
