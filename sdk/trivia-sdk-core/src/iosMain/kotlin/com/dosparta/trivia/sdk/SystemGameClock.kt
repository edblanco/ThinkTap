package com.dosparta.trivia.sdk

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
internal actual fun systemGameClock(): GameClock = object : GameClock {
    override fun wallTimeMillis(): Long = (NSDate().timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()
    override fun elapsedTimeMillis(): Long =
        (NSProcessInfo.processInfo.systemUptime * MILLIS_PER_SECOND).toLong()
}

private const val MILLIS_PER_SECOND = 1_000
