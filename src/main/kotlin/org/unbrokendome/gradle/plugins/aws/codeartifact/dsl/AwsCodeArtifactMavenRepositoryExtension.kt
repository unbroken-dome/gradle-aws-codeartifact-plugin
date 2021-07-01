package org.unbrokendome.gradle.plugins.aws.codeartifact.dsl

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy.CodeArtifactMavenProxyService
import javax.inject.Inject
import kotlin.properties.Delegates


interface AwsCodeArtifactMavenRepositoryExtension {

    companion object {

        const val ExtensionName = "codeArtifact"
    }

    var domain: String
    var domainOwner: String?
    var repositoryName: String
}


internal interface AwsCodeArtifactMavenRepositoryExtensionInternal : AwsCodeArtifactMavenRepositoryExtension {

    fun validate()

    fun configureMavenRepository(proxyServiceProvider: Provider<CodeArtifactMavenProxyService>)
}


internal open class DefaultAwsCodeArtifactMavenRepositoryExtension
@Inject constructor(
    private val mavenArtifactRepository: MavenArtifactRepository
) : AwsCodeArtifactMavenRepositoryExtensionInternal {

    override var domain: String by Delegates.notNull()
    override var domainOwner: String? = null
    override var repositoryName: String by Delegates.notNull()


    override fun validate() {
        val missingProps = mutableListOf<String>()
        try {
            domain
        } catch (ex: IllegalStateException) {
            missingProps.add("domain")
        }
        try {
            repositoryName
        } catch (ex: IllegalStateException) {
            missingProps.add("repositoryName")
        }

        if (missingProps.isNotEmpty()) {
            throw IllegalStateException(
                "Required properties must be set on CodeArtifactRepositoryConfig: " +
                        missingProps.joinToString(", ")
            )
        }
    }


    override fun configureMavenRepository(proxyServiceProvider: Provider<CodeArtifactMavenProxyService>) {
        val mavenUrl = proxyServiceProvider.get().getMavenUrl(domain, domainOwner, repositoryName)
        mavenArtifactRepository.url = mavenUrl
        mavenArtifactRepository.isAllowInsecureProtocol = true
    }
}


internal fun RepositoryHandler.handleCodeArtifactExtensions(
    proxyServiceProvider: Provider<CodeArtifactMavenProxyService>
) {
    val logger = Logging.getLogger(AwsCodeArtifactMavenRepositoryExtension::class.java)

    this.withType(MavenArtifactRepository::class.java)
        .all { repository ->
            repository as ExtensionAware
            val extension = repository.extensions.findByType(AwsCodeArtifactMavenRepositoryExtension::class.java)
            if (extension != null) {
                logger.info(
                    "Configuring MavenArtifactRepository \"{}\" from AwsCodeArtifactMavenRepositoryExtension",
                    repository.name
                )
                (extension as AwsCodeArtifactMavenRepositoryExtensionInternal)
                    .configureMavenRepository(proxyServiceProvider)
            } else {
                logger.info(
                    "MavenArtifactRepository \"{}\" does not have an AwsCodeArtifactMavenRepositoryExtension; skipping",
                    repository.name
                )
            }
        }
}
