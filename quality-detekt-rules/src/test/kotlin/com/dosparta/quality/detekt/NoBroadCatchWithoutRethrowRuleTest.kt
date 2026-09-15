package com.dosparta.quality.detekt

import io.gitlab.arturbosch.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

class NoBroadCatchWithoutRethrowRuleTest {

    @Test
    fun `reports unused broad catch`() {
        val findings = NoBroadCatchWithoutRethrowRule().lint(
            """
                fun sample() {
                    try {
                        error("boom")
                    } catch (e: Exception) {
                        println("handled")
                    }
                }
            """.trimIndent()
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `allows broad catch that uses exception`() {
        val findings = NoBroadCatchWithoutRethrowRule().lint(
            """
                fun sample() {
                    try {
                        error("boom")
                    } catch (e: Exception) {
                        println(e.message)
                    }
                }
            """.trimIndent()
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `allows broad catch that rethrows`() {
        val findings = NoBroadCatchWithoutRethrowRule().lint(
            """
                fun sample() {
                    try {
                        error("boom")
                    } catch (e: Throwable) {
                        throw e
                    }
                }
            """.trimIndent()
        )

        assertEquals(0, findings.size)
    }
}
