package org.unbrokendome.gradle.plugins.aws.codeartifact.internal.aws

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import software.amazon.awssdk.auth.credentials.*
import software.amazon.awssdk.core.SdkSystemSetting
import software.amazon.awssdk.profiles.ProfileFile
import software.amazon.awssdk.profiles.ProfileFileLocation
import software.amazon.awssdk.profiles.ProfileFileSystemSetting
import software.amazon.awssdk.utils.SystemSetting
import java.io.File


internal interface AwsParameters {

    val accessKeyId: Property<String>
    val secretAccessKey: Property<String>
    val sessionToken: Property<String>
    val configFile: Property<File>
    val sharedCredentialsFile: Property<File>
    val profile: Property<String>
    val region: Property<String>
}


internal fun AwsParameters.buildCredentialsProvider(): AwsCredentialsProvider {

    return AwsCredentialsProviderChain.builder()
        .reuseLastProviderEnabled(true)
        .credentialsProviders(
            GradleProviderCredentialsProvider(accessKeyId, secretAccessKey, sessionToken),
            buildProfileCredentialsProvider(),
            ContainerCredentialsProvider.builder().build(),
            InstanceProfileCredentialsProvider.builder().build()
        )
        .build()
}


private fun AwsParameters.buildProfileCredentialsProvider(): AwsCredentialsProvider {

    val profileFile = ProfileFile.aggregator()
        .addFile(configFile.getAsProfileFile(ProfileFile.Type.CONFIGURATION))
        .addFile(sharedCredentialsFile.getAsProfileFile(ProfileFile.Type.CREDENTIALS))
        .build()
    val profileName = profile.getOrElse("default")

    return ProfileCredentialsProvider.builder()
        .profileFile(profileFile)
        .profileName(profileName)
        .build()
}


private fun Provider<File>.getAsProfileFile(type: ProfileFile.Type): ProfileFile =
    ProfileFile.builder()
        .type(type)
        .content(this.get().toPath())
        .build()


internal fun AwsParameters.setFromProjectProperties(
    providers: ProviderFactory, rootDir: File
) {
    fun <T : Any> Property<T>.initProperty(
        setting: SystemSetting, converter: (String) -> T,
        defaultValueProvider: Provider<T> = providers.provider { setting.defaultValue() }.map(converter)
    ) {
        val valueProvider = listOfNotNull(
            setting.property()?.let { providers.gradleProperty(it).forUseAtConfigurationTime() },
            setting.property()?.let { providers.systemProperty(it).forUseAtConfigurationTime() },
            providers.environmentVariable(setting.environmentVariable()).forUseAtConfigurationTime(),
        ).reduceOrNull { p1, p2 -> p1.orElse(p2) }
            ?.map(converter)

        if (valueProvider != null) {
            this.set(valueProvider.orElse(defaultValueProvider))
        } else {
            this.set(defaultValueProvider)
        }
    }

    fun Property<String>.initProperty(
        setting: SystemSetting,
        defaultValueProvider: Provider<String> = providers.provider { setting.defaultValue() }
    ) = initProperty(setting, { it }, defaultValueProvider)

    fun resolveToFile(path: String): File =
        rootDir.resolve(path)

    accessKeyId.initProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID)
    secretAccessKey.initProperty(SdkSystemSetting.AWS_SECRET_ACCESS_KEY)
    sessionToken.initProperty(SdkSystemSetting.AWS_SESSION_TOKEN)
    configFile.initProperty(
        ProfileFileSystemSetting.AWS_CONFIG_FILE, ::resolveToFile,
        providers.provider { ProfileFileLocation.configurationFilePath().toFile() }
    )
    sharedCredentialsFile.initProperty(
        ProfileFileSystemSetting.AWS_SHARED_CREDENTIALS_FILE, ::resolveToFile,
        providers.provider { ProfileFileLocation.credentialsFilePath().toFile() }
    )
    profile.initProperty(ProfileFileSystemSetting.AWS_PROFILE)
    region.initProperty(SdkSystemSetting.AWS_REGION)
}
