package org.gradle.kotlin.dsl

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.plugins.aws.codeartifact.dsl.AwsCodeArtifactMavenRepositoryExtension
import org.unbrokendome.gradle.plugins.aws.codeartifact.dsl.getOrCreateCodeArtifactExtension


/**
 * Configure this Maven repository for AWS CodeArtifact.
 *
 * @param action an [Action] to configure the [AwsCodeArtifactMavenRepositoryExtension]
 */
fun MavenArtifactRepository.codeArtifact(action: Action<AwsCodeArtifactMavenRepositoryExtension>) {
    this as ExtensionAware
    getOrCreateCodeArtifactExtension()
    extensions.configure(AwsCodeArtifactMavenRepositoryExtension::class.java, action)
}


/**
 * Configure this Maven repository for AWS CodeArtifact.
 *
 * @param domain the domain name
 * @param repositoryName the repository name
 * @param domainOwner the domain owner account ID (optional; defaults to the account derived from the AWS credentials)
 */
fun MavenArtifactRepository.codeArtifact(
    domain: String, repositoryName: String, domainOwner: String? = null
) = codeArtifact { extension ->
    extension.domain = domain
    extension.domainOwner = domainOwner
    extension.repositoryName = repositoryName
}
