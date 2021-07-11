package org.unbrokendome.gradle.plugins.aws.codeartifact

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginAware


/**
 * Dispatcher plugin to use the correct plugin based on the target type, so the same plugin ID can be used
 * regardless of whether the target is applied to a Project, Settings or Gradle object.
 */
class AwsCodeArtifactPlugin : Plugin<PluginAware> {

    override fun apply(target: PluginAware) {
        val specificPlugin = when (target) {
            is Project -> AwsCodeArtifactProjectPlugin::class
            is Settings -> AwsCodeArtifactSettingsPlugin::class
            is Gradle -> AwsCodeArtifactInitPlugin::class
            else -> error("Invalid plugin target, must be an instance of Project, Settings or Gradle")
        }
        target.pluginManager.apply(specificPlugin.java)
    }
}
