import java.util.Properties

// local.properties ファイルを読み込むための準備
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}


android {
    namespace = "com.kensukeyahata.politics"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kensukeyahata.politics"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY")}\"")
        buildConfigField ("String", "GOOGLE_API_KEY", "\"${localProperties.getProperty("GOOGLE_API_KEY")}\"")
        buildConfigField ("String", "GOOGLE_SEARCH_ENGINE_ID", "\"${localProperties.getProperty("GOOGLE_SEARCH_ENGINE_ID")}\"")
    }
    buildFeatures {
        // この行を追加
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
//    kotlinOptions {
//        jvmTarget = "11"
//    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    //implementation("com.google.ai.client.generativeai.tooling:generativeai-google-search-tooling:0.1.0")
    // 正しいGemini SDKの依存関係
    implementation("com.google.ai.client.generativeai:generativeai:0.6.0")

    // コルーチン (Kotlinコード用。Javaから呼び出すために必要)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
//検索
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20231013")

    // Kotlinの標準ライブラリ（コルーチン実行に必要）
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("com.google.guava:guava:32.1.3-android")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


}