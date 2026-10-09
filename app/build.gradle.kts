import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 读取本地签名配置（keystore.properties 不入仓库）
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.drillbit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.drillbit"
        minSdk = 28
        targetSdk = 35
        versionCode = 28
        versionName = "0.1.0"
    }

    // 产物命名：DrillBit-v<版本名>-<versionCode>-release.apk（避免与默认 app-release.apk 混淆，传手机时可直接辨认版本）
    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "DrillBit-v${versionName}-${versionCode}-release.apk"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("drillbit.keystore")
            storePassword = keystoreProps.getProperty("STORE_PASSWORD")
            keyAlias = "drillbit"
            keyPassword = keystoreProps.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            // 骨架阶段关闭混淆，后续功能稳定后再开启
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
        // 生成 BuildConfig（设置页显示版本号用）
        buildConfig = true
    }
}

dependencies {
    // AndroidX 基础
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Room 数据库（阶段 2 使用，版本组合已验证，先接好 KSP）
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore 偏好存储（阶段 2 使用）
    implementation(libs.androidx.datastore.preferences)

    // 网络请求（阶段 2 使用）
    implementation(libs.okhttp)

    // Markdown 渲染（AI 回答与归纳稿：列表/代码块/加粗等，纯 Compose 实现）
    implementation(libs.markdown.renderer.m3)
    // recall 批次：题图加载（本地文件渲染 + 全屏缩放）
    implementation(libs.coil.compose)
}
