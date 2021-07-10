package org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy

import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.unbrokendome.awscodeartifact.mavenproxy.CodeArtifactMavenProxyServer
import org.unbrokendome.awscodeartifact.mavenproxy.LogLevel
import software.amazon.awssdk.auth.credentials.*
import software.amazon.awssdk.regions.Region
import java.net.URI
import java.time.Duration
import java.util.concurrent.CompletableFuture
import javax.inject.Inject


@Suppress("UnstableApiUsage")
internal abstract class CodeArtifactMavenProxyService
@Inject constructor(
) : BuildService<CodeArtifactMavenProxyService.Parameters>, AutoCloseable {

    companion object {
        const val RegistrationName = "codeArtifactMavenProxy"
        const val GlobalRegistrationName = "codeArtifactMavenProxyGlobal"
    }


    private val serverFuture: CompletableFuture<CodeArtifactMavenProxyServer>


    interface Parameters : BuildServiceParameters {
        val awsAccessKeyId: Property<String>
        val awsSecretAccessKey: Property<String>
        val awsProfileName: Property<String>
        val awsRegion: Property<String>
        val tokenTtl: Property<Duration>
        val wiretapLogLevel: Property<LogLevel>
    }


    init {
        val options = buildOptions()
        serverFuture = CodeArtifactMavenProxyServer.start(options)
    }


    fun getActualPort(): Int =
        serverFuture.join().actualPort


    fun getMavenUrl(domain: String, domainOwner: String?, repository: String): URI {
        val uriString = "http://localhost:${getActualPort()}/$domain/${domainOwner ?: "default"}/$repository"
        return URI(uriString)
    }


    private fun buildOptions(): CodeArtifactMavenProxyServer.Options {

        val params = this.parameters

        return CodeArtifactMavenProxyServer.Options(
            awsCredentialsProvider = buildCredentialsProvider(),
            awsRegion = params.awsRegion.orNull?.let { Region.of(it) },
            tokenTtl = params.tokenTtl.getOrElse(Duration.ofHours(1L)),
            wiretapLogLevel = params.wiretapLogLevel.getOrElse(LogLevel.DEBUG)
        )
    }


    private fun buildCredentialsProvider(): AwsCredentialsProvider {
        val params = this.parameters

        val awsAccessKeyId = params.awsAccessKeyId.orNull
        val awsSecretAccessKey = params.awsSecretAccessKey.orNull
        val accessKeyCredentialsProvider: AwsCredentialsProvider? =
            if (awsAccessKeyId != null && awsSecretAccessKey != null) {
                val credentials = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey)
                StaticCredentialsProvider.create(credentials)
            } else null

        val awsProfileName = params.awsProfileName.orNull

        val defaultCredentialsProvider = DefaultCredentialsProvider.builder()
            .also { b ->
                awsProfileName?.let { b.profileName(it) }
            }
            .build()

        return if (accessKeyCredentialsProvider != null) {
            AwsCredentialsProviderChain.builder()
                .credentialsProviders(
                    accessKeyCredentialsProvider, defaultCredentialsProvider
                )
                .build()
        } else {
            defaultCredentialsProvider
        }
    }


    override fun close() {
        serverFuture.thenCompose { server ->
            server.stop()
        }.join()
    }
}


internal fun CodeArtifactMavenProxyService.Parameters.setFromProjectProperties(providers: ProviderFactory) {
    awsAccessKeyId.set(
        providers.gradleProperty("aws.accessKeyId").forUseAtConfigurationTime()
    )
    awsSecretAccessKey.set(
        providers.gradleProperty("aws.secretAccessKey").forUseAtConfigurationTime()
    )
    awsProfileName.set(
        providers.gradleProperty("aws.profile").forUseAtConfigurationTime()
    )
    awsRegion.set(
        providers.gradleProperty("aws.region").forUseAtConfigurationTime()
    )
    wiretapLogLevel.set(
        providers.gradleProperty("aws.codeartifact.proxy.wiretapLogLevel")
            .forUseAtConfigurationTime()
            .map { logLevelName ->
                LogLevel.valueOf(logLevelName.toUpperCase())
            }
    )
}
