package com.dosparta.quality.detekt

import io.gitlab.arturbosch.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

class DomainNoAndroidImportRuleTest {

    @Test
    fun `reports android import in domain package`() {
        val findings = DomainNoAndroidImportRule().lint(
            """
                package com.dosparta.trivia.domain.usecase
                import androidx.lifecycle.ViewModel
                class Sample
            """.trimIndent()
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `reports android import in SDK core facade`() {
        val findings = DomainNoAndroidImportRule().lint(
            """
                package com.dosparta.trivia.sdk
                import androidx.lifecycle.ViewModel
                class Sample
            """.trimIndent()
        )
        assertEquals(1, findings.size)
    }

    @Test
    fun `allows android imports in SDK android adapter`() {
        val findings = DomainNoAndroidImportRule().lint(
            """
                package com.dosparta.trivia.sdk.android
                import android.content.Context
                class Sample
            """.trimIndent()
        )
        assertEquals(0, findings.size)
    }

    @Test
    fun `ignores non-domain package`() {
        val findings = DomainNoAndroidImportRule().lint(
            """
                package com.dosparta.trivia.ui.viewmodel
                import androidx.lifecycle.ViewModel
                class Sample
            """.trimIndent()
        )

        assertEquals(0, findings.size)
    }
}
