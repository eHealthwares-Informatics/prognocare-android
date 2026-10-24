import java.util.Properties

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}
android {
    namespace = "com.ehealthwares.prognocare"
    compileSdk = 35

    signingConfigs {
        release {
            storeFile.set(file(localProperties.getProperty("RELEASE_KEYSTORE_PATH") ?: ""))
            storePassword.set(localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD") ?: "")
            keyAlias.set(localProperties.getProperty("RELEASE_KEY_ALIAS") ?: "release")
            keyPassword.set(localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: "")
        }
    }

    defaultConfig {
        applicationId = "com.ehealthwares.prognocare"
        minSdk = 32
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "DEFAULT_EMR_URL",
            "\"${System.getenv("RXSOFT_EMR_URL") ?: "http://10.0.2.2:8093/api/"}\""
        )
        buildConfigField(
            "String",
            "DEFAULT_CONVERSATION_URL",
            "\"${System.getenv("RXSOFT_CONVERSATION_URL") ?: "http://10.0.2.2:8090/api/"}\""
        )
        buildConfigField(
            "String",
            "DEFAULT_WEB_CHANNEL_CODE",
            "\"PROGNOCARE_MESSAGING\""
        )

        // ── Social sign-in ────────────────────────────────────────────
        // Web OAuth client ID — audience of the Credential Manager ID token;
        // the identity service verifies `aud` against the same value.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"763036387173-jml5o3iauj2ms5269dqf7qq24gplh6mq.apps.googleusercontent.com\""
        )
        // Facebook app id + client token are public; the app secret stays in
        // the identity service env only. Overridable via git-ignored
        // local.properties or env at build time.
        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        manifestPlaceholders["FACEBOOK_APP_ID"] =
            (localProps.getProperty("FACEBOOK_APP_ID") as String?)
                ?: System.getenv("FACEBOOK_APP_ID") ?: "28296926366638041"
        manifestPlaceholders["FACEBOOK_CLIENT_TOKEN"] =
            (localProps.getProperty("FACEBOOK_CLIENT_TOKEN") as String?)
                ?: System.getenv("FACEBOOK_CLIENT_TOKEN") ?: ""
        resValue("string", "fb_login_protocol_scheme", "fb${manifestPlaceholders["FACEBOOK_APP_ID"]}")
        buildConfigField(
            "String",
            "FACEBOOK_APP_ID",
            "\"${manifestPlaceholders["FACEBOOK_APP_ID"]}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig signingConfigs.release
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    debugImplementation(libs.compose.ui.tooling)

    // Activity & Lifecycle
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.facebook.login)

    // Socket.IO
    implementation(libs.socketio.client)

    // Social sign-in (Google Credential Manager + Facebook Login)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.facebook.login)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
