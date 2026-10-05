package com.dosparta.trivia.sdk

internal actual fun systemGameClock(): GameClock = object : GameClock {
    override fun wallTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedTimeMillis(): Long = System.nanoTime() / NANOS_PER_MILLISECOND
}

private const val NANOS_PER_MILLISECOND = 1_000_000L
