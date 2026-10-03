package com.dosparta.trivia.sdk

/** Wall time is used only for compatibility with legacy persisted sessions. */
interface GameClock {
    fun wallTimeMillis(): Long
    fun elapsedTimeMillis(): Long

    companion object {
        private const val NANOS_PER_MILLISECOND = 1_000_000L
        val SYSTEM: GameClock = object : GameClock {
            override fun wallTimeMillis(): Long = System.currentTimeMillis()
            override fun elapsedTimeMillis(): Long = System.nanoTime() / NANOS_PER_MILLISECOND
        }
    }
}
