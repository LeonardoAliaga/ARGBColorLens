import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginSignatureTask
import java.util.Base64

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

tasks.test {
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// The unified IDEA distribution includes an Ultimate-only startup extension
// that cannot be instantiated by light fixtures. ARGB tests require platform
// and Java APIs only; keep this exclusion confined to the test sandbox.
tasks.named<PrepareSandboxTask>("prepareTestSandbox") {
    disabledPlugins.add("com.intellij.modules.ultimate")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2026.2.3")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)

        // Add plugin dependencies for compilation here:
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // IntelliJ IDEA 2026.2 / branch 262 and newer.
            sinceBuild = "262"
        }
    }

    // Optional, secure publishing configuration for the Marketplace.
    // The secrets are only available in the protected GitHub Actions job.
    // Local builds need no credentials.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}


// Gradle 9 requires an explicit producer/consumer relationship for the signed ZIP.
// The signature verifier reads signPlugin's output and must run after signing.
tasks.named<VerifyPluginSignatureTask>("verifyPluginSignature") {
    dependsOn(tasks.named("signPlugin"))

    // The signing task decodes Base64 signing secrets, but the signature
    // verifier writes certificateChain to a temporary PEM file unchanged.
    // Decode explicitly so the verification CLI receives valid X.509 PEM.
    certificateChain.set(
        providers.environmentVariable("CERTIFICATE_CHAIN").map { encoded ->
            String(Base64.getMimeDecoder().decode(encoded), Charsets.UTF_8)
        }
    )
}
