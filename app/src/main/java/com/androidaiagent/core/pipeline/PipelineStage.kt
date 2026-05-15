package com.androidaiagent.core.pipeline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class PipelineStage<TInput, TOutput> {
    abstract suspend fun process(input: TInput): TOutput
    
    fun createFlow(inputFlow: Flow<TInput>): Flow<TOutput> = flow {
        inputFlow.collect { input ->
            try {
                emit(process(input))
            } catch (e: Exception) {
                throw PipelineException("Stage ${this::class.simpleName} failed", e)
            }
        }
    }
}

class PipelineException(message: String, cause: Throwable? = null) : Exception(message, cause)
