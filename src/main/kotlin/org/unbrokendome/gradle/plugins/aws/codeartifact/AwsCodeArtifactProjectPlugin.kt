package org.unbrokendome.gradle.plugins.aws.codeartifact

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.plugins.PublishingPlugin
import org.unbrokendome.gradle.plugins.aws.codeartifact.dsl.handleCodeArtifactExtensions
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy.CodeArtifactMavenProxyService
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy.setFromProjectProperties


@Suppress("UnstableApiUsage")
class AwsCodeArtifactProjectPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val proxyServiceProvider = project.gradle.sharedServices.registerIfAbsent(
            CodeArtifactMavenProxyService.RegistrationName, CodeArtifactMavenProxyService::class.java
        ) { spec ->
            spec.parameters.setFromProjectProperties(project.rootProject.providers, project.rootDir)
        }

        project.repositories.handleCodeArtifactExtensions(proxyServiceProvider)

        project.plugins.withType(PublishingPlugin::class.java) {
            val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
            publishingExtension.repositories.handleCodeArtifactExtensions(proxyServiceProvider)
        }
    }
}
