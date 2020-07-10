# Blocker
Blocker is a set of Kotlin utility functions to accomodate easy blocking on asynchronous operations. It provides
extensions for raw java callbacks, rxjava, and coroutines (this last one is a stretch, since coroutines don't block).

# Why this library?
In general, if you have a use case where you feel you need to block a thread on any asynchronous construct, you may want
to rethink your design.  Asynchronous functions are _meant to be asynchronous_, often because they support long-running
functionality such as network communication or intense calculation, and it would just be tying up system resources to
block a thread in these scenarios.

However, there is such a thing out there as legacy code, and even poorly written code that every poor developer
eventually (quite quickly) learns they must maintain.  Sometimes, until such code can find the budget to be
re-architected or rewritten, one must resort to "ugly" solutions in order to solve issues or add new features.

Hence, this library.  I recommend you don't use it (unless you really do have a good reason).  But if you have to,
it's here.

(Oh, and when writing asynchronous Kotlin code, I recommend looking into coroutines if you haven't already.  They're a
godsend.)

# Package com.dmedeiros.blocker.rxjava1
RxJava has its own blocking mechanisms: BlockingObservable, BlockingSingle, and Completable.await().  You might then
wonder why this library is needed.

First off, the documentation for BlockingObservable states that use of this class is in general ok for writing tests,
but is not recommended for real production code.  Indeed, if you need to block on Observable or any of its kin you might
want to reconsider your design.

However, maintaining legacy code sometimes calls for drastic measures until such time as a redesign and rewrite warrants
doing things "correctly".  Therefore, use of such a mechanism may be reasonable, and often is indeed your only option.

Secondly, the blocking implementations in RxJava throw RuntimeException when errors occur, in order to wrap a possible
checked exception that needs to be caught by regular Java code.  Therefore, when calling these methods from java,
you will always have to remember to do it like so, or risk unexpected runtime exceptions (crashes):

    try {
        blockingObservable.single(); // or next()
    } catch (RuntimeException rte) {
        Throwable realException = rte.getCause()
        // handle the real exception
    }

This in general looks...odd. Some developers favor a fail-fast approach, and catching a RuntimeException is therefore
frowned upon.  They are meant to bubble all the way up to the VM and cause a crash, as generally these indicate real
issues that a developer needs to fix.  As a result, for this implementation I've decided to throw ExecutionException
instead, which does the same thing (wraps another exception), but is a checked exception and therefore forces developers
to write code to handle it.

Finally, you might argue that the above point doesn't matter in Kotlin. After all, Kotlin does not support checked
exceptions.  Still, in this developer's opinion, having to catch RuntimeException sux. :)