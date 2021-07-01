package org.unbrokendome.gradle.plugins.aws.codeartifact.internal.util

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import java.util.concurrent.Callable


private class MemoizedCallable<V>(
    private val callable: Callable<V>
) : Callable<V> {

    private val lazyValue = lazy { callable.call() }

    override fun call(): V =
        lazyValue.value
}


private fun <V> Callable<V>.memoize(): Callable<V> =
    if (this is MemoizedCallable) this else MemoizedCallable(this)


internal fun <T> ProviderFactory.memoized(provider: Provider<T>): Provider<T> =
    this.provider(
        Callable { provider.orNull }.memoize()
    )


internal fun <T> ProviderFactory.memoizedProvider(callable: Callable<T>): Provider<T> =
    provider(callable.memoize())
