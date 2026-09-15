import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.isFile) {
        propertiesFile.inputStream().use(::load)
    }
}

fun loadOptionalProperties(fileName: String): Properties = Properties().apply {
    val propertiesFile = rootProject.file(fileName)
    if (propertiesFile.isFile) propertiesFile.inputStream().use(::load)
}

val releaseProperties = loadOptionalProperties("release.properties")
val signingProperties = loadOptionalProperties("signing.properties")

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")}\""

fun Properties.normalizedUrl(primaryKey: String, fallbackKey: String? = null): String? =
    (getProperty(primaryKey) ?: fallbackKey?.let(::getProperty))
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?.let { if (it.endsWith('/')) it else "$it/" }

val devCrmBaseUrl = localProperties
    .normalizedUrl("OPONEXIS_DEV_CRM_BASE_URL")
    ?: "https://invalid.local/"
val devCrmApiToken = localProperties
    .getProperty("OPONEXIS_DEV_CRM_API_TOKEN")
    ?.trim()
    .orEmpty()
val prodCrmBaseUrl = localProperties
    .normalizedUrl("OPONEXIS_PROD_CRM_BASE_URL", "OPONEXIS_CRM_BASE_URL")
    ?: "https://invalid.local/"
val prodCrmApiToken = (localProperties
    .getProperty("OPONEXIS_PROD_CRM_API_TOKEN")
    ?: localProperties.getProperty("OPONEXIS_CRM_API_TOKEN"))
    ?.trim()
    .orEmpty()
val releaseCrmBaseUrl = releaseProperties
    .getProperty("crm.baseUrl")
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?.let { if (it.endsWith('/')) it else "$it/" }
    ?: "https://invalid.local/"
val releaseAuthMode = releaseProperties
    .getProperty("crm.authMode")
    ?.trim()
    .orEmpty()
val signingValues = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .associateWith { signingProperties.getProperty(it)?.trim().orEmpty() }
val signingStoreFile = signingValues.getValue("storeFile")
    .takeIf(String::isNotEmpty)
    ?.let(rootProject::file)
val signingReady = signingValues.values.all { it.isNotEmpty() && !it.startsWith("REPLACE_") } &&
    signingStoreFile?.isFile == true

// Add a mode here only after its runtime implementation and security tests exist.
val implementedProductionAuthModes = emptySet<String>()

android {
    namespace = "com.oponexis.companion"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oponexis.companion"
        minSdk = 29
        targetSdk = 36
        versionCode = 18
        versionName = "0.9.0-direct-sms"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "CRM_BASE_URL", "https://invalid.local/".asBuildConfigString())
        buildConfigField("String", "CRM_API_TOKEN", "".asBuildConfigString())
        manifestPlaceholders["usesCleartextTraffic"] = "false"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "CRM_BASE_URL", devCrmBaseUrl.asBuildConfigString())
            buildConfigField("String", "CRM_API_TOKEN", devCrmApiToken.asBuildConfigString())
            manifestPlaceholders["usesCleartextTraffic"] = devCrmBaseUrl.startsWith("http://").toString()
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "CRM_BASE_URL", prodCrmBaseUrl.asBuildConfigString())
            buildConfigField("String", "CRM_API_TOKEN", prodCrmApiToken.asBuildConfigString())
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
    }

    signingConfigs {
        if (signingReady) {
            create("internalRelease") {
                storeFile = signingStoreFile
                storePassword = signingValues.getValue("storePassword")
                keyAlias = signingValues.getValue("keyAlias")
                keyPassword = signingValues.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
        }
        release {
            buildConfigField("String", "CRM_BASE_URL", releaseCrmBaseUrl.asBuildConfigString())
            buildConfigField("String", "CRM_API_TOKEN", "".asBuildConfigString())
            signingConfig = signingConfigs.findByName("internalRelease")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = true
        disable += setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "KaptUsageInsteadOfKsp",
            "NewerVersionAvailable",
        )
    }
}

tasks.register("verifyM9ReleaseInputs") {
    group = "verification"
    description = "Validates non-secret M9 release inputs and signing configuration."
    doLast {
        val errors = mutableListOf<String>()
        if (!releaseCrmBaseUrl.startsWith("https://") ||
            releaseCrmBaseUrl.contains("invalid.local") ||
            releaseCrmBaseUrl.contains("ngrok") ||
            releaseCrmBaseUrl.contains(".example")
        ) {
            errors += "crm.baseUrl must be a permanent production HTTPS URL"
        }
        if (releaseAuthMode !in setOf("oidc", "device_enrollment")) {
            errors += "crm.authMode must be oidc or device_enrollment"
        } else if (releaseAuthMode !in implementedProductionAuthModes) {
            errors += "selected crm.authMode is not implemented yet"
        }
        listOf(
            "distribution.channel",
            "release.owner",
            "support.contact",
            "privacy.owner",
            "security.owner",
        ).forEach { key ->
            val value = releaseProperties.getProperty(key)?.trim().orEmpty()
            if (value.isEmpty() || value.startsWith("REPLACE_")) errors += "$key is not configured"
        }
        val distributionChannel = releaseProperties
            .getProperty("distribution.channel")
            ?.trim()
            .orEmpty()
        if (distributionChannel.isNotEmpty() &&
            !distributionChannel.startsWith("REPLACE_") &&
            distributionChannel !in setOf("managed_google_play", "mdm", "managed_direct")
        ) {
            errors += "distribution.channel must be managed_google_play, mdm, or managed_direct"
        }
        if (!signingReady) errors += "signing.properties is missing, incomplete, or points to no keystore"
        if (errors.isNotEmpty()) {
            throw GradleException("M9 release inputs are incomplete:\n- ${errors.joinToString("\n- ")}")
        }
    }
}

tasks.register("prepareM9Release") {
    group = "build"
    description = "Runs the M9 input gate, lint, tests, and signed release build."
    dependsOn("verifyM9ReleaseInputs", "lintProdRelease", "testProdReleaseUnitTest", "assembleProdRelease")
}

kapt {
    correctErrorTypes = true
}

hilt {
    enableAggregatingTask = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.dagger.hilt.android)
    kapt(libs.dagger.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.retrofit.core)
    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
