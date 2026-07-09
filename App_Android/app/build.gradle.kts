plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.doan3"
    compileSdk = 36 // Đã đổi thành 36

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.doan3"
        minSdk = 24
        targetSdk = 36 // Đã đổi thành 36
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Các thư viện thêm vào
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("com.google.android.material:material:1.11.0")

    // --- CÁC THƯ VIỆN HỖ TRỢ XỬ LÝ & ĐỒNG BỘ DỮ LIỆU SAU NÀY ---

    // 1. Coroutines: Hỗ trợ xử lý tác vụ chạy ngầm (tải dữ liệu không làm đơ màn hình)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 2. Lifecycle (ViewModel & LiveData): Quản lý vòng đời dữ liệu, giữ data không bị mất khi xoay màn hình
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // 3. Retrofit & Gson (Chuẩn bị sẵn nếu sau này cần gọi API đồng bộ với Server)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

}