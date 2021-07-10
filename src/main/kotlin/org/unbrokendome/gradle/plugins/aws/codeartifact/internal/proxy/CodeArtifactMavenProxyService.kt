package org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy

import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.unbrokendome.awscodeartifact.mavenproxy.CodeArtifactMavenProxyServer
import org.unbrokendome.awscodeartifact.mavenproxy.LogLevel
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.aws.AwsParameters
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.aws.buildCredentialsProvider
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.aws.setFromProjectProperties
import software.amazon.awssdk.regions.Region
import java.io.File
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


    interface Parameters : BuildServiceParameters, AwsParameters {
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
            awsCredentialsProvider = params.buildCredentialsProvider(),
            awsRegion = params.region.orNull?.let { Region.of(it) },
            tokenTtl = params.tokenTtl.getOrElse(Duration.ofHours(1L)),
            wiretapLogLevel = params.wiretapLogLevel.getOrElse(LogLevel.DEBUG)
        )
    }


    override fun close() {
        serverFuture.thenCompose { server ->
            server.stop()
        }.join()
    }
}


internal fun CodeArtifactMavenProxyService.Parameters.setFromProjectProperties(
    providers: ProviderFactory, rootDir: File
) {
    (this as AwsParameters).setFromProjectProperties(providers, rootDir)

    wiretapLogLevel.set(
        providers.gradleProperty("aws.codeartifact.proxy.wiretapLogLevel")
            .forUseAtConfigurationTime()
            .map { logLevelName ->
                LogLevel.valueOf(logLevelName.toUpperCase())
            }
    )
}
