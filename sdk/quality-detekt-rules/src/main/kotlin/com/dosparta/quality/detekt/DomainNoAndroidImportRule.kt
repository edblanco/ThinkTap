package com.dosparta.quality.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtImportDirective

class DomainNoAndroidImportRule(config: Config = Config.empty) : Rule(config) {

    override val issue: Issue = Issue(
        id = "DomainNoAndroidImport",
        severity = Severity.Defect,
        description = "Domain layer must not depend on Android/UI imports.",
        debt = Debt.FIVE_MINS
    )

    private val forbiddenPrefixes = listOf(
        "android.",
        "androidx.",
        "com.dosparta.trivia.ui."
    )

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val packageName = importDirective.containingKtFile.packageFqName.asString()
        val isCore = packageName.startsWith(DOMAIN_PACKAGE_PREFIX) ||
            (packageName.startsWith(SDK_PACKAGE_PREFIX) && !packageName.startsWith(ANDROID_SDK_PACKAGE_PREFIX))
        if (!isCore) return

        val importPath = importDirective.importPath?.pathStr ?: return
        if (forbiddenPrefixes.any { importPath.startsWith(it) }) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(importDirective),
                    message = "Forbidden domain import: $importPath"
                )
            )
        }
    }

    private companion object {
        const val DOMAIN_PACKAGE_PREFIX = "com.dosparta.trivia.domain"
        const val SDK_PACKAGE_PREFIX = "com.dosparta.trivia.sdk"
        const val ANDROID_SDK_PACKAGE_PREFIX = "com.dosparta.trivia.sdk.android"
    }
}
