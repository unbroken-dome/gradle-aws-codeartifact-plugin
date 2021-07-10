package org.unbrokendome.gradle.plugins.aws.codeartifact

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.unbrokendome.gradle.plugins.aws.codeartifact.dsl.handleCodeArtifactExtensions
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy.CodeArtifactMavenProxyService
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy.setFromProjectProperties


@Suppress("UnstableApiUsage")
class AwsCodeArtifactSettingsPlugin : Plugin<Settings> {

    override fun apply(settings: Settings) {

        val proxyServiceProvider = settings.gradle.sharedServices.registerIfAbsent(
            CodeArtifactMavenProxyService.RegistrationName, CodeArtifactMavenProxyService::class.java
        ) { spec ->
            spec.parameters.setFromProjectProperties(settings.providers, settings.rootDir)
        }

        settings.dependencyResolutionManagement.repositories.handleCodeArtifactExtensions(proxyServiceProvider)

        settings.gradle.beforeProject { project ->
            project.buildscript.repositories.handleCodeArtifactExtensions(proxyServiceProvider)
            project.pluginManager.apply(AwsCodeArtifactProjectPlugin::class.java)
        }
    }
}
