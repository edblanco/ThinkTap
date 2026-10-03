package com.dosparta.quality.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class NoBroadCatchWithoutRethrowRule(config: Config = Config.empty) : Rule(config) {

    override val issue: Issue = Issue(
        id = "NoBroadCatchWithoutRethrow",
        severity = Severity.Defect,
        description = "Avoid broad catch blocks unless the exception is rethrown or explicitly used.",
        debt = Debt.TEN_MINS
    )

    override fun visitCatchSection(catchClause: KtCatchClause) {
        super.visitCatchSection(catchClause)

        if (!catchClause.isBroadCatch()) return

        val parameterName = catchClause.catchParameter?.name
        val catchBody = catchClause.catchBody ?: return
        val hasThrow = catchBody.collectDescendantsOfType<KtThrowExpression>().isNotEmpty()
        val usesException = parameterName != null &&
            catchBody.collectDescendantsOfType<KtNameReferenceExpression>()
                .any { it.getReferencedName() == parameterName }

        if (!hasThrow && !usesException) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(catchClause),
                    message = "Broad catch should rethrow or use the exception value."
                )
            )
        }
    }

    private fun KtCatchClause.isBroadCatch(): Boolean {
        val typeText = catchParameter?.typeReference?.text ?: return false
        return typeText == "Exception" ||
            typeText == "Throwable" ||
            typeText == "java.lang.Exception" ||
            typeText == "java.lang.Throwable"
    }
}
