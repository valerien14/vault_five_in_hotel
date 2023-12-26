package com.canisy.vaultfive.core.di

import android.app.Application
import androidx.navigation.NavHostController
import com.canisy.vaultfive.core.util.DeviceUtil
import com.canisy.vaultfive.core.util.NavUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Singletons {

	@Provides
	@Singleton
	fun provideDeviceUtil(app: Application): DeviceUtil {
		return DeviceUtil(app.contentResolver)
	}

	@Provides
	@Singleton
	fun provideNavUtil(app: Application): NavUtil = NavUtil(app)

	@Provides
	@Singleton
	fun provideNavHostController(navUtil: NavUtil): NavHostController = navUtil.navHostController

	@Provides
	@Singleton
	fun provideVaultDir(app: Application): File {
		val vaultDirectory = File(app.filesDir, "vault")
		if (!vaultDirectory.exists()) vaultDirectory.mkdirs()
		return vaultDirectory
	}
}