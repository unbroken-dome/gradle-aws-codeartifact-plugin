package org.unbrokendome.gradle.plugins.aws.codeartifact.dsl

import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.ExtensionAware


/**
 * A convention plugin that provides a [codeArtifact] method with map-style arguments for Groovy DSL.
 */
interface AwsCodeArtifactMavenRepositoryConvention {

    companion object {

        const val ConventionPluginName = "codeArtifactConvention"
    }

    /**
     * Configure this repository for AWS CodeArtifact.
     *
     * Uses map-style arguments for Groovy DSL support.
     *
     * The following arguments are supported (all of type [String]):
     *
     * * `domain`: the CodeArtifact domain name
     * * `repositoryName`: the CodeArtifact repository name
     * * `domainOwner`: the CodeArtifact domain owner (optional)
     *
     * Example:
     *
     * ```
     * repositories {
     *     maven { }.codeArtifact(domain: 'my-domain', repositoryName: 'my-repo')
     * }
     * ```
     */
    fun codeArtifact(args: Map<String, Any>)
}


internal class DefaultAwsCodeArtifactMavenRepositoryConvention(
    private val repository: MavenArtifactRepository
) : AwsCodeArtifactMavenRepositoryConvention {

    override fun codeArtifact(args: Map<String, Any>) {
        repository as ExtensionAware
        val extension = repository.extensions.getByType(AwsCodeArtifactMavenRepositoryExtension::class.java)

        args["domain"]?.toString()?.let { extension.domain = it }
        extension.domainOwner = args["domainOwner"]?.toString()
        args["repositoryName"]?.toString()?.let { extension.repositoryName = it }
    }
}


internal fun MavenArtifactRepository.installCodeArtifactMavenRepositoryConvention() {
    this as ExtensionAware
    val conventionPlugin = DefaultAwsCodeArtifactMavenRepositoryConvention(this)
    with(this.extensions as Convention) {
        plugins[AwsCodeArtifactMavenRepositoryConvention.ConventionPluginName] = conventionPlugin
    }
}
