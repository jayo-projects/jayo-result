import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.tooling.GradleConnector
import kotlin.jvm.optionals.getOrNull

println("Using Gradle version: ${gradle.gradleVersion}")
println("Using Java compiler version: ${JavaVersion.current()}")

plugins {
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.release)
}

val versionCatalog: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
fun catalogVersion(lib: String) =
    versionCatalog.findVersion(lib).getOrNull()?.requiredVersion
        ?: throw GradleException("Version '$lib' is not specified in the toml version catalog")

val javaVersion = catalogVersion("java").toInt()

repositories {
    mavenCentral()
}

dependencies {
    api("org.jspecify:jspecify:${catalogVersion("jspecify")}")

    testImplementation("org.junit.jupiter:junit-jupiter:${catalogVersion("junit")}")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${catalogVersion("junit")}")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    withSourcesJar()
    withJavadocJar()
}

val testJavaVersion = System.getProperty("test.java.version", "").toIntOrNull()
tasks.test {
    if (testJavaVersion != null) {
        javaLauncher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(testJavaVersion)
        }
    }
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

publishing {
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repos/releases"))
        }
    }


    publications {
        create<MavenPublication>("Maven") {
            from(components["java"])
        }

        withType<MavenPublication> {
            pom {
                name.set(project.name)
                description.set("Jayo Result is a Java port of the Result<T> type from the Kotlin stdlib")
                url.set("https://github.com/jayo-projects/jayo-result")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        name.set("pull-vert")
                        url.set("https://github.com/pull-vert")
                    }
                }

                scm {
                    connection.set("scm:git@github.com/jayo-projects/jayo-result")
                    developerConnection.set("scm:git@github.com/jayo-projects/jayo-result.git")
                    url.set("https://github.com/jayo-projects/jayo-result.git")
                }
            }
        }
    }
}

signing {
    // Require signing.keyId, signing.password and signing.secretKeyRingFile
    sign(publishing.publications)
}

// workaround : https://github.com/researchgate/gradle-release/issues/304#issuecomment-1083692649
configure(listOf(tasks.release, tasks.runBuildTasks)) {
    configure {
        actions.clear()
        doLast {
            GradleConnector
                .newConnector()
                .forProjectDirectory(layout.projectDirectory.asFile)
                .connect()
                .use { projectConnection ->
                    val buildLauncher = projectConnection
                        .newBuild()
                        .forTasks(*tasks.toTypedArray())
                        .setStandardInput(System.`in`)
                        .setStandardOutput(System.out)
                        .setStandardError(System.err)
                    gradle.startParameter.excludedTaskNames.forEach {
                        buildLauncher.addArguments("-x", it)
                    }
                    buildLauncher.run()
                }
        }
    }
}

// when the Gradle version changes:
// -> execute ./gradlew wrapper, then remove .gradle directory, then execute ./gradlew wrapper again
tasks.wrapper {
    gradleVersion = "9.3.1"
    distributionType = Wrapper.DistributionType.ALL
}

