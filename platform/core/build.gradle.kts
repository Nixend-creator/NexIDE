plugins {
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  api(project(":platform:util"))
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.junit.jupiter)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
  useJUnitPlatform()
}

// JDI (com.sun.jdi) lives in the jdk.jdi module
tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(listOf("--add-modules", "jdk.jdi"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    freeCompilerArgs.add("-Xjdk-release=17")
  }
}
