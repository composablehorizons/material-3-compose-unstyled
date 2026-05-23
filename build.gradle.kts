@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

val injectWasmPreloads by tasks.registering {
  description = "Injects preload links for generated Wasm distribution artifacts."

  doLast {
    val distDir = layout.buildDirectory.dir("dist/wasmJs/productionExecutable").get().asFile
    val indexFile = distDir.resolve("index.html")
    if (!indexFile.isFile) return@doLast

    val scriptPreloads = distDir
      .listFiles { file -> file.isFile && file.extension == "js" }
      .orEmpty()
      .sortedBy { it.name }
      .map { """  <link rel="preload" href="${it.name}" as="script">""" }

    val wasmPreloads = distDir
      .listFiles { file -> file.isFile && file.extension == "wasm" }
      .orEmpty()
      .sortedBy { it.name }
      .map { """  <link rel="preload" href="${it.name}" as="fetch" type="application/wasm" crossorigin>""" }

    val preloadBlock = (scriptPreloads + wasmPreloads).joinToString(
      separator = "\n",
      prefix = "  <!-- wasm-preloads:start -->\n",
      postfix = "\n  <!-- wasm-preloads:end -->",
    )

    val existingPreloadBlock = Regex(
      pattern = """\n?  <!-- wasm-preloads:start -->.*?  <!-- wasm-preloads:end -->\n?""",
      options = setOf(RegexOption.DOT_MATCHES_ALL),
    )
    val indexHtml = indexFile.readText().replace(existingPreloadBlock, "\n")
    val updatedIndexHtml = indexHtml.replaceFirst("</title>", "</title>\n$preloadBlock")
    indexFile.writeText(updatedIndexHtml)
  }
}

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

  wasmJs {
    browser {
      val rootDirPath = project.rootDir.path
      val projectDirPath = project.projectDir.path
      commonWebpackConfig {
        outputFileName = "composeApp.js"
        devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
          static = (static ?: mutableListOf()).apply {
            add(rootDirPath)
            add(projectDirPath)
          }
        }
      }
    }
    binaries.executable()
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

tasks.named("wasmJsBrowserDistribution") {
  finalizedBy(injectWasmPreloads)
}
