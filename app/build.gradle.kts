import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// CI signing: secrets provide the keystore (base64) + passwords.
// Local builds fall back to the same release key via .secrets/ (gitignored),
// so a locally built APK installs as an in-place update of the GitHub release.
val ciKeystoreB64 = System.getenv("KEYSTORE_BASE64")
val ciStorePass = System.getenv("KEYSTORE_PASSWORD")
val ciKeyAlias = System.getenv("KEY_ALIAS")
val ciKeyPass = System.getenv("KEY_PASSWORD")
val hasCiSigning = !ciKeystoreB64.isNullOrBlank() && !ciStorePass.isNullOrBlank()

// local fallback: the SAME keystore CI uses, checked in as .secrets/ (gitignored)
val localKs = rootProject.file(".secrets/qanvas-release.jks")
val localSecrets = rootProject.file(".secrets")
fun secret(name: String): String? =
    runCatching {
        val f = localSecrets.resolve("$name.txt")
        if (f.exists()) f.readText().trim().ifBlank { null } else null
    }.getOrNull()
val hasLocalSigning = !hasCiSigning && localKs.exists() && secret("KEYSTORE_PASSWORD") != null
val useLocalSigning = hasLocalSigning

android {
    namespace = "com.samge.qanvas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.samge.qanvas"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "1.1.12"
        ndk {
            // arm64 only: the MNN runtime ships arm64-v8a .so files
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (hasCiSigning) {
            create("ci") {
                val tmp = File.createTempFile("qanvas", ".jks")
                tmp.writeBytes(Base64.getDecoder().decode(ciKeystoreB64))
                storeFile = tmp
                storePassword = ciStorePass
                keyAlias = ciKeyAlias
                keyPassword = ciKeyPass
            }
        } else if (useLocalSigning) {
            create("localRelease") {
                storeFile = localKs
                storePassword = secret("KEYSTORE_PASSWORD")
                keyAlias = secret("KEY_ALIAS") ?: "qanvas"
                keyPassword = secret("KEY_PASSWORD") ?: secret("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = when {
                hasCiSigning -> signingConfigs.getByName("ci")
                useLocalSigning -> signingConfigs.getByName("localRelease")
                else -> null
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // local test builds also use the release key when present — same
            // signature as GitHub releases, so they install as updates
            if (useLocalSigning) {
                signingConfig = signingConfigs.getByName("localRelease")
            }
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Qwen-Image-2.1 MNN runtime (official AAR v0.2.2, prebuilt libMNN.so + JNI)
    implementation(files("libs/qwenimage21.aar"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Room: generation history
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")
}
