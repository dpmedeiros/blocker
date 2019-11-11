package com.dmedeiros.blocker.rxjava1

import rx.Completable
import rx.Single
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException

/**
 * Calls a Single in a blocking fashion, returning the result synchronously.
 *
 * Note that if the single emits a null value this will throw an [ExecutionException]
 *
 * Unlike BlockingObservable, this is meant to be used in production code.
 *
 * @throws ExecutionException when the Single terminates with an exception.  This exception wraps
 * the exception via [ExecutionException.cause]
 * @throws InterruptedException if the thread is interrupted while the Single is running
 */
@Throws(InterruptedException::class, ExecutionException::class)
fun <T> Single<T>.runBlocking(): T {
    val latch = CountDownLatch(1)
    var outsideResult: T? = null
    var outsideThrowable: Throwable? = null

    val subscription = subscribe(
        { result ->
            outsideResult = result
            latch.countDown()
        },
        { throwable ->
            outsideThrowable = throwable
            latch.countDown()
        }
    )

    try {
        latch.await()
    } catch (e: InterruptedException) {
        subscription.unsubscribe()
        outsideThrowable = e
    }

    outsideThrowable.let { // capture var outsideThrowable
        if (it is InterruptedException) {
            Thread.currentThread().interrupt()
            throw it
        }

        return outsideResult ?: throw ExecutionException(it ?: Throwable("null returned"))
    }
}

/**
 * Calls a Completable in a blocking fashion.
 *
 * Unlike BlockingObservable, this is meant to be used in production code.  If any errors that
 * occur, or if the current thread is interrupted, [ExecutionException] is thrown
 *
 * @throws ExecutionException when the completable terminates with an exception
 * @throws InterruptedException if the thread is interrupted while the Completable is running
 */
@Throws(InterruptedException::class, ExecutionException::class)
fun Completable.runBlocking() {
    val latch = CountDownLatch(1)
    var outsideThrowable: Throwable? = null

    val subscription = subscribe(
        // onCompleted
        {
            latch.countDown()
        },
        // onError
        { throwable ->
            outsideThrowable = throwable
            latch.countDown()
        }
    )

    try {
        latch.await()
    } catch (e: InterruptedException) {
        subscription.unsubscribe()
        outsideThrowable = e
    }

    outsideThrowable.let {
        if (it is InterruptedException) {
            throw it
        }

        if (it != null) {
            throw ExecutionException(it)
        }
    }
}
