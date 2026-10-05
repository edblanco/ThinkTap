package com.dosparta.trivia.sdk

/** Wall time is used only for compatibility with legacy persisted sessions. */
interface GameClock {
    fun wallTimeMillis(): Long
    fun elapsedTimeMillis(): Long

    companion object {
        val SYSTEM: GameClock = systemGameClock()
    }
}

internal expect fun systemGameClock(): GameClock
