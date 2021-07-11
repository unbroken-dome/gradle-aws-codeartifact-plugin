package org.unbrokendome.gradle.plugins.aws.codeartifact

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginAware
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.plugins.PublishingPlugin
import org.unbrokendome.gradle.plugins.aws.codeartifact.dsl.handleCodeArtifactExtensions
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy.CodeArtifactMavenProxyService
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy.setFromProjectProperties
import java.io.File


@Suppress("UnstableApiUsage")
class AwsCodeArtifactPlugin : Plugin<PluginAware> {

    override fun apply(target: PluginAware) {
        when (target) {
            is Project -> applyToProject(target)
            is Settings -> applyToSettings(target)
            is Gradle -> applyToGradle(target)
            else -> error("Invalid plugin target, must be an instance of Project, Settings or Gradle")
        }
    }


    private fun applyToSettings(settings: Settings) {

        val proxyServiceProvider = settings.gradle.registerProxyService(
            settings.providers, settings.rootDir
        )

        settings.dependencyResolutionManagement.repositories.handleCodeArtifactExtensions(proxyServiceProvider)

        settings.gradle.beforeProject { project ->
            project.buildscript.repositories.handleCodeArtifactExtensions(proxyServiceProvider)
            project.pluginManager.apply(AwsCodeArtifactPlugin::class.java)
        }
    }


    private fun applyToProject(project: Project) {

        val proxyServiceProvider = project.gradle.registerProxyService(
            project.rootProject.providers, project.rootDir
        )

        project.repositories.handleCodeArtifactExtensions(proxyServiceProvider)

        project.plugins.withType(PublishingPlugin::class.java) {
            val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
            publishingExtension.repositories.handleCodeArtifactExtensions(proxyServiceProvider)
        }
    }


    private fun applyToGradle(gradle: Gradle) {
        val globalProxyServiceProvider = gradle.sharedServices.registerIfAbsent(
            CodeArtifactMavenProxyService.GlobalRegistrationName, CodeArtifactMavenProxyService::class.java
        ) { }

        gradle.beforeSettings { settings ->
            settings.buildscript.repositories.handleCodeArtifactExtensions(globalProxyServiceProvider)
            settings.pluginManagement.repositories.handleCodeArtifactExtensions(globalProxyServiceProvider)
        }
    }


    private fun Gradle.registerProxyService(
        providers: ProviderFactory, rootDir: File
    ): Provider<CodeArtifactMavenProxyService> {
        return sharedServices.registerIfAbsent(
            CodeArtifactMavenProxyService.RegistrationName, CodeArtifactMavenProxyService::class.java
        ) { spec ->
            spec.parameters.setFromProjectProperties(providers, rootDir)
        }
    }
}
