import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

// Version derived from -PappVersion=vX.Y.Z passed by CI (git tag), falls back to dev snapshot.
val appVersion = (findProperty("appVersion") as? String)?.removePrefix("v") ?: "0.0.0-dev"
val appVersionCode = run {
    val parts = appVersion.split("-")[0].split(".")
    try { maxOf(1, parts[0].toInt() * 10000 + parts[1].toInt() * 100 + (parts.getOrNull(2)?.toInt() ?: 0)) }
    catch (_: Exception) { 1 }
}
// packageVersion must be strictly numeric (X.Y.Z) for native distributions.
val packageVersion = appVersion.split("-")[0].takeIf { it.matches(Regex("\\d+\\.\\d+\\.\\d+")) } ?: "1.0.0"

// Signing: reads from env vars (CI) or local.properties (local dev). Both are optional;
// without them the release build is produced unsigned.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use(::load)
}
fun signingProp(key: String): String? = System.getenv(key) ?: localProps.getProperty(key)

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.okio)
            implementation(libs.kodein)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = "com.eloi.retrolightgb"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.eloi.retrolightgb"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersion
    }

    signingConfigs {
        create("release") {
            val ks = signingProp("KEYSTORE_PATH")?.let { rootProject.file(it) }?.takeIf { it.exists() }
            if (ks != null) {
                storeFile = ks
                storePassword = signingProp("STORE_PASSWORD")
                keyAlias = signingProp("KEY_ALIAS")
                keyPassword = signingProp("KEY_PASSWORD")
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null) signingConfig = releaseConfig
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.eloi.retrolightgb.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "RetrolightGB"
            packageVersion = packageVersion
        }
    }
}