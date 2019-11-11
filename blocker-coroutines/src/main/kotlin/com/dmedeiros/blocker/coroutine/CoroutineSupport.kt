package com.dmedeiros.blocker.coroutine

import com.dmedeiros.blocker.jvm.blockUntilCallback
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
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
suspend fun <T, E : Throwable, R> coroutineBridge(f: ((T) -> Unit, (E) -> Unit) -> R): T =
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

/**
 * starts a coroutine in the given scope from outside of a coroutine, and blocks until the coroutine returns a result
 */
fun <T> CoroutineScope.awaitCoroutineResult(f: suspend () -> T): T {
    val job = async { f() }
    val invokeOnCompletion = fun(handler: CompletionHandler) = job.invokeOnCompletion(handler)

    val error = invokeOnCompletion.blockUntilCallback()
    error?.also { throw it }

    @Suppress("EXPERIMENTAL_API_USAGE")
    return job.getCompleted()
}
