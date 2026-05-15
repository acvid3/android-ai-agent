package com.androidaiagent.action

data class MicroAction(
    val name: String,
    val action: com.androidaiagent.ai.ActionType,
    val target: String?,
    val parameters: Map<String, Any> = emptyMap()
)

class MicroActionPlanner {
    fun plan(chainName: String, steps: List<MicroAction>): MicroActionChain {
        return MicroActionChain(chainName, steps)
    }

    fun forModalDismissal(modalKind: String, closeLabel: String? = null): MicroActionChain {
        val steps = listOf(
            MicroAction(
                name = "dismiss_modal",
                action = com.androidaiagent.ai.ActionType.TAP,
                target = closeLabel ?: modalKind,
                parameters = emptyMap()
            )
        )
        return MicroActionChain("dismiss_$modalKind", steps)
    }
}

data class MicroActionChain(
    val name: String,
    val steps: List<MicroAction>
)
