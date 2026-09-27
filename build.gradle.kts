import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.compose.multiplatform) apply false
  alias(libs.plugins.compose.compiler) apply false
}

allprojects {
  group = "com.nexide"
  version = "0.1.0-SNAPSHOT"
}

subprojects {
  plugins.withId("org.jetbrains.kotlin.jvm") {
    // Ensure Java plugin is present for toolchain support
    apply(plugin = "java")
    extensions.configure<JavaPluginExtension> {
      toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
      }
    }
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
      freeCompilerArgs.add("-Xcontext-receivers")
    }
  }
}
