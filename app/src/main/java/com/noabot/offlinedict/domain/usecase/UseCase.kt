package com.noabot.offlinedict.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Base interface for Use Cases
 * @param P Parameter type
 * @param R Result type
 */
interface UseCase<in P, out R> {
    operator fun invoke(param: P): Flow<R>
}