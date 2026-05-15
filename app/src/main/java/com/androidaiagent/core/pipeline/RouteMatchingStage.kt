package com.androidaiagent.core.pipeline

import com.androidaiagent.ui.model.UiMap

data class RouteMatch(val routeName: String?, val confidence: Float)

class RouteMatchingStage : PipelineStage<UiMap, RouteMatch>() {
    override suspend fun process(input: UiMap): RouteMatch {
        return RouteMatch(null, 0f)
    }
}
