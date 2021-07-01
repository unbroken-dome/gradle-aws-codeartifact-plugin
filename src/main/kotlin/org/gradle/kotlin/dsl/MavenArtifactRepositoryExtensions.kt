package org.gradle.kotlin.dsl

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.plugins.aws.codeartifact.dsl.AwsCodeArtifactMavenRepositoryExtension
import org.unbrokendome.gradle.plugins.aws.codeartifact.dsl.DefaultAwsCodeArtifactMavenRepositoryExtension


fun MavenArtifactRepository.codeArtifact(action: Action<AwsCodeArtifactMavenRepositoryExtension>) {
    this as ExtensionAware
    val extension = extensions.findByType(AwsCodeArtifactMavenRepositoryExtension::class.java)
        ?: extensions.create(
            AwsCodeArtifactMavenRepositoryExtension::class.java,
            AwsCodeArtifactMavenRepositoryExtension.ExtensionName,
            DefaultAwsCodeArtifactMavenRepositoryExtension::class.java,
            this
        )
    action.execute(extension)
}


fun MavenArtifactRepository.codeArtifact(domain: String, domainOwner: String?, repositoryName: String) {
    codeArtifact { extension ->
        extension.domain = domain
        extension.domainOwner = domainOwner
        extension.repositoryName = repositoryName
    }
}
