package com.dosparta.quality.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class TriviaRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "trivia-quality"

    override fun instance(config: Config): RuleSet = RuleSet(
        id = ruleSetId,
        rules = listOf(
            DomainNoAndroidImportRule(config),
            NoBroadCatchWithoutRethrowRule(config)
        )
    )
}
