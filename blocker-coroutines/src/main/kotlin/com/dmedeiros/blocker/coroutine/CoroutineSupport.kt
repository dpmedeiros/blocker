package com.dmedeiros.blocker.coroutine

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * adapts a function taking two callbacks to the current coroutine which suspends until callback
 * completion
 *
 * first callback returns a value of type [T]
 * second callback reports an error/throwable of type [E]
 *
 */
@Suppress("TooGenericExceptionCaught")
suspend inline fun <T, E : Throwable, R> coroutineBridge(crossinline f: ((T) -> Unit, (E) -> Unit) -> R): T =
    suspendCoroutine { continuation: Continuation<T> ->
        try {
            f({
                continuation.resume(it)
            }, {
                continuation.resumeWithException(it)
            })
        } catch (throwable: Throwable) {
            continuation.resumeWithException(throwable)
        }
    }
