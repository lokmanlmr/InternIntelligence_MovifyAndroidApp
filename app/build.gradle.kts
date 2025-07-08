plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.navigation.safe.args)
}

android {
    namespace = "com.example.Movify"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.Movify"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.database.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)



    implementation("com.google.firebase:firebase-analytics")
    // MVVM - Android Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0") // For lifecycleScope
    // Android KTX for ViewModel and LiveData (if not already present)
    implementation("androidx.fragment:fragment-ktx:1.3.6") // For viewModels() delegate

    // Kotlin Coroutines - For asynchronous operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Firebase Authentication & Firestore
    // Import the Firebase BoM (Bill of Materials) for consistent versions
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))


    // Firebase Authentication library
    implementation("com.google.firebase:firebase-auth-ktx")
    // Firebase Firestore (optional for auth, but essential for user reviews later)
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson converter for Retrofit to parse JSON
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp logging interceptor (useful for debugging network requests)
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    // Glide for efficient image loading
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    // ViewPager2 (for the sliding banner)
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // For ViewPager2
    implementation("com.tbuonomo:dotsindicator:5.1.0")

    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
}
