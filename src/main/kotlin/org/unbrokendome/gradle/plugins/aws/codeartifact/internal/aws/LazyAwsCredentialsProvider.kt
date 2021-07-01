package org.unbrokendome.gradle.plugins.aws.codeartifact.internal.aws

import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider


internal class LazyAwsCredentialsProvider(
    private val lazyProvider: Lazy<AwsCredentialsProvider>
) : AwsCredentialsProvider {

    constructor(providerFactory: () -> AwsCredentialsProvider)
    : this(lazy(providerFactory))

    override fun resolveCredentials(): AwsCredentials =
        lazyProvider.value.resolveCredentials()
}
