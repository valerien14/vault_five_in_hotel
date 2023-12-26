plugins {
	id("com.android.application")
	id("com.google.dagger.hilt.android")
	id("org.jetbrains.kotlin.android")
	id("com.google.devtools.ksp")
}

android {
	namespace = "com.canisy.vaultfive"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.canisy.vaultfive"
		minSdk = 29
		targetSdk = 34
		versionCode = 1
		versionName = "1.0"
		vectorDrawables {
			useSupportLibrary = true
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
		kotlinCompilerExtensionVersion = "1.5.3"
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

dependencies {
	ksp("androidx.room:room-compiler:2.6.1")
	ksp("com.google.dagger:hilt-compiler:2.48.1")
	ksp("com.github.bumptech.glide:ksp:4.14.2")
	implementation("androidx.core:core-ktx:1.12.0")
	implementation("androidx.activity:activity-compose:1.8.2")
	implementation(platform("androidx.compose:compose-bom:2023.03.00"))
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-graphics")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.compose.material:material-icons-extended:1.6.0-beta03")
	implementation("androidx.compose.runtime:runtime-tracing:1.0.0-beta01")
	implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
	implementation("androidx.media3:media3-exoplayer:1.2.0")
	implementation("androidx.media3:media3-exoplayer-dash:1.2.0")
	implementation("androidx.media3:media3-ui:1.2.0")
	implementation("androidx.navigation:navigation-runtime-ktx:2.7.6")
	implementation("androidx.room:room-runtime:2.6.1")
	implementation("androidx.room:room-ktx:2.6.1")
	implementation("androidx.security:security-crypto:1.0.0")
	implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
	implementation("com.google.dagger:hilt-android:2.48.1")
	implementation("io.coil-kt:coil-compose:2.4.0")
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.21")
}