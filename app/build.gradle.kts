plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.argon.ph03n1x"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.argon.ph03n1x"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "4.8.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.register("generate100MBPayload") {
    group = "build"
    description = "Writes \~98 MB of uncompressible random bytes into assets/argon_blob.bin"

    doLast {
        val assetsDir = file("src/main/assets")
        assetsDir.mkdirs()
        val outFile = file("${assetsDir}/argon_blob.bin")

        val targetBytes = 98L * 1024 * 1024
        val buffer = ByteArray(1024 * 1024)
        val random = java.security.SecureRandom()

        outFile.outputStream().use { fos ->
            var written = 0L
            while (written < targetBytes) {
                random.nextBytes(buffer)
                val toWrite = minOf(buffer.size.toLong(), targetBytes - written).toInt()
                fos.write(buffer, 0, toWrite)
                written += toWrite
            }
        }
        println("Generated ${outFile.length()} bytes → ${outFile.absolutePath}")
    }
}

tasks.named("preBuild").configure {
    dependsOn("generate100MBPayload")
}