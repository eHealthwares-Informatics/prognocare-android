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
    alias(libs.plugins.sentry.android)
}
android {
    namespace = "com.ehealthwares.prognocare"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ehealthwares.prognocare"
        minSdk = 32
        targetSdk = 36
        versionCode = 4
        versionName = "0.0.2"

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
            "DEFAULT_CONCEPTS_URL",
            "\"${System.getenv("RXSOFT_CONCEPTS_URL") ?: "http://10.0.2.2:3011/api/v1/"}\""
        )
        buildConfigField(
            "String",
            "DEFAULT_RXSOFT_URL",
            "\"${System.getenv("RXSOFT_BACKEND_URL") ?: "http://10.0.2.2:8080/api/"}\""
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
            // Release builds ship pointing at production; the 10.0.2.2 defaults
            // in defaultConfig are only for debug builds.
            buildConfigField("String", "DEFAULT_EMR_URL", "\"https://api.ehealthwares.com/emr/\"")
            buildConfigField("String", "DEFAULT_CONVERSATION_URL", "\"https://api.ehealthwares.com/conversation/\"")
            buildConfigField("String", "DEFAULT_CONCEPTS_URL", "\"https://api.ehealthwares.com/concepts/v1/\"")
            buildConfigField("String", "DEFAULT_RXSOFT_URL", "\"https://api.ehealthwares.com/\"")
        }
    }

    // Name the built APK with app-specific prefix, e.g. prognocare-release-1.0.3.apk
    applicationVariants.all {
        val variantName = name
        val vName = versionName
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val ext = output.outputFile?.extension?.ifBlank { "apk" } ?: "apk"
            output.outputFileName = "prognocare-$variantName-$vName.$ext"
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

// ── Release signing (configured after android block for Kotlin DSL compatibility) ──
val releaseSigningConfig = android.signingConfigs.create("release")
releaseSigningConfig.setStoreFile(file(localProperties.getProperty("RELEASE_KEYSTORE_PATH") ?: ""))
releaseSigningConfig.setStorePassword(localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD") ?: "")
releaseSigningConfig.setKeyAlias(localProperties.getProperty("RELEASE_KEY_ALIAS") ?: "release")
releaseSigningConfig.setKeyPassword(localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: "")
android.buildTypes.getByName("release").setSigningConfig(releaseSigningConfig)

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
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

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
    // Sentry
    implementation(libs.sentry.android)

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

// ── Sentry (org/project shared by the ehealthwares Android apps) ──
// Must live in the app module: the plugin ignores the configuration when it is
// declared on the root project ("only supported for the app module" warning),
// which made release builds fail with "A project ID or slug is required".
sentry {
    org.set("ehealthwares")
    projectName.set("ehealthwares_android")

    // Upload sources so stack traces show code in Sentry.
    includeSourceContext.set(true)
}
