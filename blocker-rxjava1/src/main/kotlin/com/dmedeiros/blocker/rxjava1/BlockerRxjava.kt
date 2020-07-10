package com.dmedeiros.blocker.rxjava1

import rx.Completable
import rx.Observable
import rx.Single
import rx.Subscription
import rx.observables.BlockingObservable
import java.lang.NullPointerException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException

/**
 * Calls an Observable<T> in a blocking fashion, collecting items emitted by the observable and returning the collected
 * list.  This will of course eat up memory to collect everything and will not return until the observable completes.
 * Make sure you really want to block on the observable!
 *
 * Unlike [BlockingObservable], this is meant to be used in production code. BlockingObservable propagates errors via
 * [RuntimeException], which you might not want.
 *
 * If any errors occur, [ExecutionException] is thrown.  If the current
 * thread is interrupted, [InterruptedException] is thrown.
 *
 * @throws ExecutionException when the Single terminates with an exception.  This exception wraps
 * the exception and is accessible from [ExecutionException.cause]
 * @throws InterruptedException if the thread is interrupted while the Completable is running
 * @return List<T> of items returned by the [Observable]
 */
@Throws(InterruptedException::class, ExecutionException::class)
fun <T> Observable<T>.runBlocking(): List<T> {
    val latch = CountDownLatch(1)
    val mutableList = mutableListOf<T>()
    var outsideThrowable: Throwable? = null

    val subscription = subscribe(
        // onNext
        {
            mutableList += it
        },
        // onError
        { throwable ->
            outsideThrowable = throwable
            latch.countDown()
        },
        // onCompleted
        {
            latch.countDown()
        }
    )

    latch.awaitLatchOrInterrupt(subscription)

    outsideThrowable?.let { throw ExecutionException(it) }

    return mutableList.toList()
}

//  TODO: Observable<T>.blockingIterable(): Iterable<T>

/**
 * Calls a Single in a blocking fashion, returning the result synchronously.
 *
 * Note that if the single emits a null value this will throw an [ExecutionException]
 *
 * Unlike [BlockingObservable], this is meant to be used in production code. BlockingObservable propagates errors via
 * [RuntimeException], which you might not want.
 *
 * If any errors occur, [ExecutionException] is thrown.  If the current
 * thread is interrupted, [InterruptedException] is thrown.
 *
 * @throws ExecutionException when the Single terminates with an exception.  This exception wraps
 * the exception and is accessible from [ExecutionException.cause]
 * @throws InterruptedException if the thread is interrupted while the Single is running
 * @return T the item returned by the [Single]
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

    latch.awaitLatchOrInterrupt(subscription)

    outsideThrowable?.let { throw ExecutionException(it) }

    return outsideResult ?: throw ExecutionException(NullPointerException("Single returned null"))
}

/**
 * Calls a Completable in a blocking fashion.
 *
 * Unlike [BlockingObservable], this is meant to be used in production code. BlockingObservable propagates errors via
 * [RuntimeException], which you might not want.
 *
 * If any errors occur, [ExecutionException] is thrown.  If the current thread is interrupted,
 * [InterruptedException] is thrown.
 *
 * @throws ExecutionException when the Single terminates with an exception.  This exception wraps
 * the exception and is accessible from [ExecutionException.cause]
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

    latch.awaitLatchOrInterrupt(subscription)

    outsideThrowable?.let { throw ExecutionException(it) }
}

private fun CountDownLatch.awaitLatchOrInterrupt(subscription: Subscription) {
    try {
        await()
    } catch (e: InterruptedException) {
        subscription.unsubscribe()
        Thread.currentThread().interrupt()
        throw e
    }
}
