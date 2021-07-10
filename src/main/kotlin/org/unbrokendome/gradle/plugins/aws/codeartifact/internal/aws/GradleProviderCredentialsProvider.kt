package org.unbrokendome.gradle.plugins.aws.codeartifact.internal.aws

import org.gradle.api.provider.Provider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.core.exception.SdkClientException


/**
 * An implementation of [AwsCredentialsProvider] that loads credentials from Gradle [Provider]s.
 *
 * Similar to
 * [EnvironmentVariableCredentialsProvider][software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider]
 * or [SystemPropertyCredentialsProvider][software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider]
 * from the AWS SDK, but based on Gradle providers instead of environment variables / system properties.
 */
internal class GradleProviderCredentialsProvider(
    private val accessKeyIdProvider: Provider<String>,
    private val secretAccessKeyProvider: Provider<String>,
    private val sessionTokenProvider: Provider<String>
) : AwsCredentialsProvider {

    override fun resolveCredentials(): AwsCredentials {

        val accessKeyId = accessKeyIdProvider.orNull
            ?: throw SdkClientException.create("No value for access key ID")

        val secretAccessKey = secretAccessKeyProvider.orNull
            ?: throw SdkClientException.create("No value for secret access key")

        return sessionTokenProvider.orNull?.let { sessionToken ->
            AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken)
        } ?: AwsBasicCredentials.create(accessKeyId, secretAccessKey)
    }
}
