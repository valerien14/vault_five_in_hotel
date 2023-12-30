//package com.canisy.vaultfive.ui.route.main
//
//import android.util.Log
//import androidx.compose.foundation.ExperimentalFoundationApi
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.pager.HorizontalPager
//import androidx.compose.foundation.pager.PagerState
//import androidx.compose.foundation.pager.rememberPagerState
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Tab
//import androidx.compose.material3.TabRow
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.derivedStateOf
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
//object TestLogger {
//	@Volatile
//	private var compositionCount = 0
//	fun log(msg: String, threadName: String) {
//		compositionCount++
//		Log.d("TAG", "Composing $msg on $threadName, so far: $compositionCount")
//	}
//}
//
//@Composable
//fun TabZero(
//	getIsSelected: () -> Boolean,
//	onClick: () -> Unit,
//	getText: @Composable () -> Unit
//) {
//	val isSelected = getIsSelected()
//	return Tab(
//		selected = isSelected,
//		onClick = { onClick() },
//		enabled = !isSelected,
//		selectedContentColor = MaterialTheme.colorScheme.background,
//		unselectedContentColor = MaterialTheme.colorScheme.inversePrimary,
//		text = { getText() }
//	)
//}
//
//@Composable
//fun TabOne(
//	getIsSelected: () -> Boolean,
//	onClick: () -> Unit,
//	getText: @Composable () -> Unit
//) {
//	val isSelected = getIsSelected()
//	return Tab(
//		selected = isSelected,
//		onClick = { onClick() },
//		enabled = !isSelected,
//		selectedContentColor = MaterialTheme.colorScheme.background,
//		unselectedContentColor = MaterialTheme.colorScheme.inversePrimary,
//		text = { getText() }
//	)
//}
//
//@Composable
//fun TestTabRow(
//	getSelectedIndex: () -> Int,
//	getTabZero: @Composable () -> Unit,
//	getTabOne: @Composable () -> Unit,
//) {
//	return TabRow(
//		selectedTabIndex = getSelectedIndex(),
//		containerColor = MaterialTheme.colorScheme.primary,
//		indicator = {},
//		divider = {}
//	) { getTabZero(); getTabOne() }
//}
//
//@Composable
//fun PageZero(getText: @Composable () -> Unit) { return Box(Modifier.fillMaxSize(), Alignment.Center) { getText() } }
//
//@Composable
//fun PageOne(getText: @Composable () -> Unit) { return Box(Modifier.fillMaxSize(), Alignment.Center) { getText() } }
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun TestContent(
//	pagerState: PagerState,
//	paddingValues: PaddingValues,
//	getPageZero: @Composable () -> Unit,
//	getPageOne: @Composable () -> Unit,
//) {
//	return HorizontalPager(
//		state = pagerState,
//		contentPadding = paddingValues,
//		beyondBoundsPageCount = 1,
//		pageContent = { if (it == 0) getPageZero() else getPageOne() }
//	)
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun TestScaffold(
//	getTestTabRow: @Composable () -> Unit,
//	getTestContent: @Composable (PaddingValues) -> Unit,
//) {
//	return Scaffold(
//		modifier = Modifier,
//		topBar = { getTestTabRow() },
//		bottomBar = {},
//		content = { getTestContent(it) }
//	)
//}
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun AllComposableInOne() {
//	val scope = rememberCoroutineScope()
//	val pagerState = rememberPagerState { 2 }
//	val selectedIndex by remember { derivedStateOf { pagerState.currentPage } }
//	val log: (String) -> Unit = remember {
//		{ val thread = Thread.currentThread().name; scope.launch(Dispatchers.IO) { TestLogger.log(it, thread) } }
//	}
//
//	TestScaffold(
//		getTestTabRow = {
//			log("getTestTabRow")
//			TestTabRow(
//				getSelectedIndex = { selectedIndex },
//				getTabZero = {
//					log("getTabZero")
//					TabZero(
//						getIsSelected = { selectedIndex == 0 },
//						onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
//						getText = {
//							log("getText")
//							Text(text = "Tab Zero")
//						}
//					)
//				},
//				getTabOne = {
//					log("getTabOne")
//					TabOne(
//						getIsSelected = { selectedIndex == 1 },
//						onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
//						getText = {
//							log("getText")
//							Text(text = "Tab One")
//						}
//					)
//				}
//			)
//		},
//		getTestContent = {
//			log("getTestContent")
//			TestContent(
//				pagerState = pagerState,
//				paddingValues = it,
//				getPageZero = {
//					log("getPageZero")
//					PageZero(
//						getText = {
//							log("getText")
//							Text(text = "Page Zero")
//						}
//					)
//				},
//				getPageOne = {
//					log("getPageOne")
//					PageOne(
//						getText = {
//							log("getText")
//							Text(text = "Page One")
//						}
//					)
//				}
//			)
//		}
//	)
//
////	@OptIn(ExperimentalMaterial3Api::class)
////	Scaffold(
////		modifier = Modifier,
////		topBar = {
////			TabRow(
////				selectedTabIndex = selectedIndex,
////				containerColor = MaterialTheme.colorScheme.primary,
////				indicator = {},
////				divider = {}
////			) {
////				Tab(
////					selected = selectedIndex == 0,
////					onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
////					enabled = selectedIndex == 1,
////					selectedContentColor = MaterialTheme.colorScheme.background,
////					unselectedContentColor = MaterialTheme.colorScheme.inversePrimary,
////					text = { Text(text = "Page 0", style = MaterialTheme.typography.labelLarge) }
////				)
////				Tab(
////					selected = selectedIndex == 1,
////					onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
////					enabled = selectedIndex == 0,
////					selectedContentColor = MaterialTheme.colorScheme.background,
////					unselectedContentColor = MaterialTheme.colorScheme.inversePrimary,
////					text = { Text(text = "Page 1", style = MaterialTheme.typography.labelLarge) }
////				)
////			}
////		},
////		bottomBar = {},
////		content = { paddingValues ->
////			HorizontalPager(
////				state = pagerState,
////				contentPadding = paddingValues,
////				pageContent = {
////					if (it == 0) Box(Modifier.fillMaxSize(), Alignment.Center) {
////						Text(text = "Page 0")
////					}
////					else Box(Modifier.fillMaxSize(), Alignment.Center) {
////						Text(text = "Page 1")
////					}
////				}
////			)
////		}
////	)
//}