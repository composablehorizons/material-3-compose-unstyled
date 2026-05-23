import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.compose)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.application)
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

kotlin {
  jvmToolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }

  jvm {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_17
    }
  }

  androidTarget {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_17
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(libs.androidx.annotation)
      implementation(libs.compose.material3)
      implementation(libs.composeunstyled)
      implementation(libs.composeunstyled.build.modifier)
      implementation(libs.composeunstyled.window.container.size)
      implementation(libs.icons.material.symbols.rounded.cmp)
      implementation(libs.navigation.compose)
    }

    jvmMain.dependencies {
      implementation(compose.desktop.currentOs) {
        exclude("org.jetbrains.compose.material")
        exclude("org.jetbrains.compose.material3")
      }
    }

    androidMain.dependencies {
      implementation(libs.androidx.activitycompose)
    }
  }
}

compose.desktop {
  application {
    mainClass = "com.composeunstyled.demo.materialimpl.MainKt"
  }
}

android {
  namespace = "com.composeunstyled.demo.materialimpl"
  compileSdk = libs.versions.android.compileSDK.get().toInt()

  defaultConfig {
    applicationId = "com.composeunstyled.demo.materialimpl"
    minSdk = libs.versions.android.minSDK.get().toInt()
    targetSdk = libs.versions.android.compileSDK.get().toInt()
    versionCode = 1
    versionName = "1.0.0"
  }
}

androidComponents {
  beforeVariants(selector().withBuildType("release")) { variantBuilder ->
    variantBuilder.enable = false
  }
}
