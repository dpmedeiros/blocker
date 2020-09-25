package com.dmedeiros.blocker.jvm

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Calls, in a blocking fashion, a referred method that accepts a callback which takes one
 * parameter of type [T]
 *
 * @param timeoutMs how long to wait for the callback to be invoked.  if this timeout expires
 * before the callback is invoked, null will be returned
 *
 * @return the value reported to the callback.
 *
 * @throws InterruptedException if the blocking thread is interrupted
 */
@Throws(InterruptedException::class)
inline fun <reified T, R> (((T) -> Unit) -> R).runBlocking(timeoutMs: Long = 500): T {
    val resultContainer: MutableSet<T> = mutableSetOf()
    val latch = CountDownLatch(1)

    invoke { result ->
        resultContainer += result
        latch.countDown()
    }

    latch.await(timeoutMs, MILLISECONDS)
    return resultContainer.single()
}

/**
 * Calls, in a blocking fashion, a referred method that accepts a callback which takes one
 * parameter of type [T].  Blocks indefinitely until the callback is invoked.
 *
 * @return the value reported to the callback.
 *
 * @throws InterruptedException if the blocking thread is interrupted
 */
inline fun <reified T, R> (((T) -> Unit) -> R).blockUntilCallback(): T {
    val resultContainer: MutableSet<T> = mutableSetOf()
    val latch = CountDownLatch(1)

    invoke { result ->
        resultContainer += result
        latch.countDown()
    }

    latch.await()
    return resultContainer.single()
}

/**
 * Calls, in a blocking fashion, a referred method that accepts a callback which takes no
 * parameters.
 *
 * @param timeoutMs how long to wait for the callback to be invoked.
 *
 * @return true if the method finished executing within [timeoutMs] milliseconds
 *
 * @throws InterruptedException if the blocking thread is interrupted
 */
@Throws(InterruptedException::class)
fun ((() -> Unit) -> Unit).runNoResultBlocking(timeoutMs: Long = 500): Boolean {
    val latch = CountDownLatch(1)

    invoke { latch.countDown() }

    return latch.await(timeoutMs, MILLISECONDS)
}

/**
 * uninterruptibly runs a block on a default thread pool and blocks until the result of the block execution is
 * available.
 *
 * this function is for executing critical tasks that must be completed before the running thread
 * may be interrupted. since the current thread will be blocked, be aware of any deadlocks that
 * may occur if the new thread tries to grab a lock the current thread has taken.
 *
 * if the current thread is interrupted by the time this function returns, the thread's interrupted
 * flag will be set. it is the caller's responsibility to throw interrupted exception if this
 * flag is set.
 */
fun <R> runUninterruptibly(block: () -> R): R =
    UninterruptableRunner(uninterruptableThreadPoolExecutor).run(block)

/**
 * returns a DSL to configure an executor on which to run a block uninterruptibly.
 */
fun <R> runUninterruptibly(): UninterruptableRunnerConfigure = UninterruptableRunnerConfigure()

class UninterruptableRunnerConfigure internal constructor() {
    fun <R> onExecutor(executor: Executor, block: () -> R): R = UninterruptableRunner(executor).run(block)
}

private class UninterruptableRunner constructor(private val executor: Executor) {
    private val lock = ReentrantLock()
    private val blockFinishedCondition = lock.newCondition()

    internal fun <R> run(block: () -> R): R {
        lock.withLock {
            val resultContainer: MutableSet<R> = mutableSetOf()
            executor.execute {
                lock.withLock {
                    val result = block()
                    resultContainer += result
                    blockFinishedCondition.signal()
                }
            }
            blockFinishedCondition.awaitUninterruptibly()
            return resultContainer.single()
        }
    }
}

private val uninterruptableThreadPoolExecutor by lazy {
    Executors.newCachedThreadPool(object : ThreadFactory {
        val atom = AtomicInteger(0)
        override fun newThread(runnable: Runnable?): Thread =
            Thread(runnable, "Uninterruptable-" + atom.incrementAndGet())
    })
}
