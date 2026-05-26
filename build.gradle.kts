// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.android.legacy.kapt) apply false
  alias(libs.plugins.ktlint) apply false
}

subprojects {
  apply(plugin = "org.jlleitschuh.gradle.ktlint")
  configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    android.set(true)
    outputColorName.set("RED")
  }
}