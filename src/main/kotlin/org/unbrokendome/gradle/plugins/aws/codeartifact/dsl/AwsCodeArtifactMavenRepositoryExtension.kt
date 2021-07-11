package org.unbrokendome.gradle.plugins.aws.codeartifact.dsl

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.aws.codeartifact.internal.proxy.CodeArtifactMavenProxyService
import javax.inject.Inject


/**
 * Extension for AWS CodeArtifact Maven repositories.
 *
 * This extension will be installed on all [MavenArtifactRepository] instances by the plugin.
 *
 * Note that when creating a repository with a configuration action/closure Gradle lets us install extensions only
 * _after_ the config closure is executed:
 *
 * ```
 * repositories {
 *     maven { /* extension object is not available here */ }
 * }
 * ```
 *
 * In Kotlin DSL, we can work around this by using a Kotlin extension method which installs the extension object
 * on-demand. That extension method is useful anyway because dynamically created Gradle extensions are not picked up
 * by the DSL support in IDEs.
 *
 * In Groovy DSL, we have to call `codeArtifact` _after_ the MavenArtifactRepository object is initially constructed:
 *
 * ```
 * repositories {
 *     maven { }.codeArtifact { /* configure CodeArtifact extension here */ }
 * }
 * ```
 *
 */
interface AwsCodeArtifactMavenRepositoryExtension {

    companion object {

        const val ExtensionName = "codeArtifact"
    }

    /**
     * The AWS CodeArtifact domain name.
     */
    var domain: String

    /**
     * The AWS CodeArtifact domain owner (account ID). Optional; defaults to the account ID derived from the
     * AWS credentials.
     */
    var domainOwner: String?

    /**
     * The AWS CodeArtifact repository name.
     */
    var repositoryName: String
}


internal interface AwsCodeArtifactMavenRepositoryExtensionInternal : AwsCodeArtifactMavenRepositoryExtension {

    /**
     * Inject the [CodeArtifactMavenProxyService] provider into this extension object. This cannot happen
     * at creation time because we may need to eagerly create the extension when using the Kotlin DSL extension method:
     *
     * ```
     * repositories {
     *    maven {
     *       codeArtifact { /* ... */ }
     *    }
     * }
     * ```
     *
     * Unfortunately we cannot inject the `org.gradle.api.services.BuildServiceRegistry` when creating the extension
     * object, because it is not injectable (as of Gradle 7.1).
     *
     * @param provider the [Provider] for [CodeArtifactMavenProxyService]
     */
    fun setProxyServiceProvider(provider: Provider<CodeArtifactMavenProxyService>)
}


internal open class DefaultAwsCodeArtifactMavenRepositoryExtension
@Inject constructor(
    private val mavenArtifactRepository: MavenArtifactRepository
) : AwsCodeArtifactMavenRepositoryExtension, AwsCodeArtifactMavenRepositoryExtensionInternal {

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultAwsCodeArtifactMavenRepositoryExtension::class.java)
    }

    private var _proxyServiceProvider: Provider<CodeArtifactMavenProxyService>? = null
    private var _domain: String? = null
    private var _domainOwner: String? = null
    private var _repositoryName: String? = null


    override fun setProxyServiceProvider(provider: Provider<CodeArtifactMavenProxyService>) {
        this._proxyServiceProvider = provider
        configureRepositoryIfComplete()
    }


    override var domain: String
        get() = checkNotNull(_domain) { "domain property has not been set" }
        set(value) {
            _domain = value
            configureRepositoryIfComplete()
        }


    override var domainOwner: String?
        get() = _domainOwner
        set(value) {
            _domainOwner = value
            configureRepositoryIfComplete()
        }


    override var repositoryName: String
        get() = checkNotNull(_repositoryName) { "repositoryName property has not been set" }
        set(value) {
            _repositoryName = value
            configureRepositoryIfComplete()
        }


    private fun configureRepositoryIfComplete() {

        val domain = _domain
        val repositoryName = _repositoryName
        val proxyServiceProvider = _proxyServiceProvider

        if (domain != null && repositoryName != null && proxyServiceProvider != null) {

            val domainOwner = _domainOwner

            logger.debug(
                "Configuring Maven repository \"{}\" for AWS CodeArtifact " +
                        "with domain={}, domainOwner={}, repositoryName={}",
                mavenArtifactRepository.name, domain, domainOwner, repositoryName
            )

            val mavenUrl = proxyServiceProvider.get().getMavenUrl(domain, domainOwner, repositoryName)
            mavenArtifactRepository.url = mavenUrl
            mavenArtifactRepository.isAllowInsecureProtocol = true
        }
    }
}


/**
 * Install the [AwsCodeArtifactMavenRepositoryExtension] and [AwsCodeArtifactMavenRepositoryConvention] on all
 * repositories created by this [RepositoryHandler].
 *
 * @receiver the [RepositoryHandler]
 * @param proxyServiceProvider a [Provider] for the [CodeArtifactMavenProxyService]
 *        (registered with the Gradle `BuildServiceRegistry`)
 */
internal fun RepositoryHandler.handleCodeArtifactExtensions(
    proxyServiceProvider: Provider<CodeArtifactMavenProxyService>
) {
    this.withType(MavenArtifactRepository::class.java)
        .all { repository ->
            repository as ExtensionAware
            val extension = repository.getOrCreateCodeArtifactExtension()
            (extension as AwsCodeArtifactMavenRepositoryExtensionInternal)
                .setProxyServiceProvider(proxyServiceProvider)

            repository.installCodeArtifactMavenRepositoryConvention()
        }
}


/**
 * Get the AWS CodeArtifact extension object for this repository, or create it if it does not exist.
 *
 * @return the [AwsCodeArtifactMavenRepositoryExtension]
 */
internal fun MavenArtifactRepository.getOrCreateCodeArtifactExtension(): AwsCodeArtifactMavenRepositoryExtension {
    this as ExtensionAware
    return extensions.findByType(AwsCodeArtifactMavenRepositoryExtension::class.java)
        ?: createCodeArtifactExtension()
}


/**
 * Create an AWS CodeArtifact extension object for this repository.
 *
 * @return the [AwsCodeArtifactMavenRepositoryExtension]
 */
internal fun MavenArtifactRepository.createCodeArtifactExtension(): AwsCodeArtifactMavenRepositoryExtension {
    this as ExtensionAware
    return extensions.create(
        AwsCodeArtifactMavenRepositoryExtension::class.java,
        AwsCodeArtifactMavenRepositoryExtension.ExtensionName,
        DefaultAwsCodeArtifactMavenRepositoryExtension::class.java,
        this
    )
}
