package org.unbrokendome.gradle.plugins.aws.codeartifact

import org.gradle.api.Plugin
import org.gradle.api.invocation.Gradle
import org.unbrokendome.gradle.plugins.aws.codeartifact.dsl.handleCodeArtifactExtensions
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy.CodeArtifactMavenProxyService


@Suppress("UnstableApiUsage")
class AwsCodeArtifactInitPlugin : Plugin<Gradle> {

    override fun apply(gradle: Gradle) {

        val globalProxyServiceProvider = gradle.sharedServices.registerIfAbsent(
            CodeArtifactMavenProxyService.GlobalRegistrationName, CodeArtifactMavenProxyService::class.java
        ) { }

        gradle.beforeSettings { settings ->
            settings.buildscript.repositories.handleCodeArtifactExtensions(globalProxyServiceProvider)
            settings.pluginManagement.repositories.handleCodeArtifactExtensions(globalProxyServiceProvider)
        }
    }
}
