plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.12.0"
}


repositories {
    mavenLocal()
    mavenCentral()
}


val awsSdkVersion = project.extra["aws.sdk.version"] as String


dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation("org.unbroken-dome.aws-codeartifact-maven-proxy:aws-codeartifact-maven-proxy:0.1.0") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testImplementation(kotlin("stdlib-jdk8"))
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


gradlePlugin {

    plugins {
        create("codeArtifact") {
            id = "org.unbroken-dome.aws.codeartifact"
            implementationClass = "org.unbrokendome.gradle.plugins.aws.codeartifact.AwsCodeArtifactPlugin"
        }
    }
}


pluginBundle {
    val githubUrl = project.extra["github.url"] as String

    website = githubUrl
    vcsUrl = githubUrl
    description = "A Gradle plugin for using AWS CodeArtifact repositories"
    tags = listOf("codeartifact")

    (plugins) {
        "codeArtifact" {
            displayName = "AWS CodeArtifact plugin"
        }
    }
}
