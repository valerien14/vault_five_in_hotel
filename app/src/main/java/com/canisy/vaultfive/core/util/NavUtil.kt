package com.canisy.vaultfive.core.util

import android.app.Application
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import javax.inject.Inject

class NavUtil @Inject constructor(context: Application) {
	val navHostController = NavHostController(context).apply {
		navigatorProvider.addNavigator(ComposeNavigator())
		navigatorProvider.addNavigator(DialogNavigator())
	}
	fun navigate(route: String) {
		navHostController.navigate(route) {
			popUpTo(navHostController.currentBackStackEntry?.destination?.id!!) { inclusive = true }
		}
	}
}