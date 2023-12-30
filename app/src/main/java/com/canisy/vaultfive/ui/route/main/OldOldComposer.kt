//package com.canisy.vaultfive.ui.route.main
//
//import android.util.Log
//import androidx.activity.compose.BackHandler
//import androidx.compose.foundation.ExperimentalFoundationApi
//import androidx.compose.foundation.combinedClickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.pager.HorizontalPager
//import androidx.compose.foundation.pager.PagerState
//import androidx.compose.foundation.pager.rememberPagerState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.NavigateNext
//import androidx.compose.material.icons.filled.CheckBox
//import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
//import androidx.compose.material.icons.rounded.Folder
//import androidx.compose.material3.ElevatedButton
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.ListItem
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Tab
//import androidx.compose.material3.TabRow
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.derivedStateOf
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableIntStateOf
//import androidx.compose.runtime.mutableStateListOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.bumptech.glide.integration.compose.CrossFade
//import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
//import com.bumptech.glide.integration.compose.GlideImage
//import com.canisy.vaultfive.core.model.Plain
//import com.canisy.vaultfive.core.viewmodels.Action
//import com.canisy.vaultfive.core.viewmodels.DeviceAction
//import com.canisy.vaultfive.core.viewmodels.DeviceState
//import com.canisy.vaultfive.core.viewmodels.MainState
//import com.canisy.vaultfive.core.viewmodels.MainViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
//object IsolatedSideEffect {
//	@Volatile
//	private var compositionCount = 0
//	fun log(msg: String, threadName: String) {
//		compositionCount++
//		Log.d("TAG", "Composing $msg on $threadName, so far: $compositionCount")
//	}
//}
//
//@Composable
//@OptIn(ExperimentalFoundationApi::class)
//fun MainComposer(vm: MainViewModel = hiltViewModel()) {
//	val scope = rememberCoroutineScope()
//	var counter by remember { mutableIntStateOf(0) }
//	var isHello by remember { mutableStateOf(true) }
//	val devicePath = remember { mutableStateListOf<Triple<Float, Int, Long>>() }
//	val vaultPath = remember { mutableStateListOf<Triple<Float, Int, Long>>() }
//	val isOnSelectMode by remember { derivedStateOf { state is MainState.Ready.Selecting } }
//	val pagerState = rememberPagerState { 2 }
//	val vaultSelected by remember { derivedStateOf { pagerState.currentPage == 0 } }
//	val log: (String) -> Unit = remember {
//		{ val thread = Thread.currentThread().name; scope.launch(Dispatchers.IO) { IsolatedSideEffect.log(it, thread) } }
//	}
//	MainScreen(
//		getMainScaffold = {
//			log("getMainScaffold")
//			MainScaffold(
//				getMainTopBar = {
//					log("getMainTopBar")
//					MainTopBar(
//						getMainTabRow = {
//							log("getMainTabRow")
//							MainTabRow(
//								getSelectedTabIndex = { pagerState.currentPage },
//								getVaultTab = {
//									log("getVaultTab")
//									VaultTab(
//										getSelectedFlag = { vaultSelected },
//										getEnabledFlag = { !vaultSelected },
//										getOnClickLambda = { scope.launch { pagerState.animateScrollToPage(0) } },
//										getTabLabel = {
//											log("getVaultTabLabel")
//											Text(text = "Vault", style = MaterialTheme.typography.labelLarge)
//										}
//									)
//								},
//								getDeviceTab = {
//									log("getDeviceTab")
//									DeviceTab(
//										getSelectedFlag = { !vaultSelected },
//										getEnabledFlag = { vaultSelected },
//										getOnClickLambda = { scope.launch { pagerState.animateScrollToPage(1) } },
//										getTabLabel = {
//											log("getDeviceTabLabel")
//											Text(text = "Device", style = MaterialTheme.typography.labelLarge)
//										}
//									)
//								}
//							)
//						}
//					)
//				}, getMainContent = { paddingValues ->
//					MainContent(
//						paddingValues = paddingValues,
//						pagerState = pagerState,
//						getVaultView = {
//							log("getVaultView")
//							BackHandler(true) {
//								vm.dispatch(BackButtonClick)
//							}
//							VaultView(
//								getIntComposable = {
//									log("getIntComposable")
//									Text(text = counter.toString())
//								},
//								getStrComposable = {
//									log("getStrComposable")
//									Text(text = if (isHello) "Hello World" else "Nope")
//								},
//								getIntButton = {
//									log("getIntButton")
//									ElevatedButton(
//										onClick = {
//											Log.d("TAG", "Invoking counter++ callback!")
//											counter++
//										},
//										content = {
//											log("content")
//											Text(text = "Increment")
//										}
//									)
//								}, getStrButton = {
//									log("getStrButton")
//									ElevatedButton(
//										onClick = {
//											Log.d("TAG", "Invoking Action.StrButtonClick callback!")
//											isHello = !isHello
//										},
//										content = {
//											log("content")
//											Text(text = "Set Text")
//										}
//									)
//								}
//							)
//						}, getDeviceView = {
//							log("getDeviceView")
//							val deviceState by vm.deviceState.collectAsStateWithLifecycle()
//							BackHandler(true) {  }
//							when (deviceState) {
//								DeviceState.Loading -> TODO()
//								is DeviceState.Error.DataUnavailable -> TODO()
//								is DeviceState.Ready -> {
//									DeviceView(
//										getListOfDeviceModels = { listOfDeviceModelHeader },
//										getDirectoryTile = {
//											val name = remember { getName(it) }
//											val date = remember { getDate(it) }
//											val kids = remember { getKids(it) }
//											log("getDirectoryTile")
//											DirTile(
//												onLongClick = { vm.dispatch(Action.TileLongClick) },
//												onClick = { vm.dispatch(Action.TileClick.Device(getPath(it))) },
//												getHeadlineText = {
//													log("getHeadlineText: $name")
//													Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
//												},
//												getDirSupportingText = {
//													log("getSupportingText: $name")
//													Text(
//														text = "$date | Items: $kids",
//														color = Color.Gray,
//														style = MaterialTheme.typography.bodySmall
//													)
//												},
//												getDirLeadingContent = {
//													log("getLeadingContent: $name")
//													Icon(
//														imageVector = Icons.Rounded.Folder,
//														contentDescription = null,
//														modifier = Modifier.size(48.dp),
//														tint = MaterialTheme.colorScheme.primary
//													)
//												},
//												getDirTrailingContent = {
//													log("getTrailingContent: $name")
//													Icon(Icons.AutoMirrored.Filled.NavigateNext, null)
//												}
//											)
//										},
//										getMediaTile = {
//											val name = remember { getName(it) }
//											val date = remember { getDate(it) }
//											val size = remember { getSize(it) }
//											log("getDirectoryTile")
//											MediaTile(
//												onLongClick = { vm.dispatch(Action.TileLongClick) },
//												onClick = {
//													Log.d("TAG", "MediaTile Invoking onClick callback: isVault? ${false}, $name")
//												},
//												getHeadlineText = {
//													log("getHeadlineText: $name")
//													Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
//												},
//												getMediaSupportingText = {
//													log("getSupportingText: $name")
//													Text(
//														text = "$date | Size: $size",
//														color = Color.Gray,
//														style = MaterialTheme.typography.bodySmall
//													)
//												},
//												getMediaLeadingContent = {
//													log("getLeadingContent: $name")
//													@OptIn(ExperimentalGlideComposeApi::class)
//													GlideImage(
//														model = getPath(it),
//														contentDescription = name,
//														modifier = (Modifier.size(48.dp)).clip(RoundedCornerShape(4.dp)),
//														contentScale = ContentScale.Crop,
//														transition = CrossFade
//													)
//												},
//												getMediaTrailingContent = {
//													log("getMediaTrailingContent: $name")
//													MediaTrailingContent(
//														getOnSelectedModeFlag = { isOnSelectMode },
//														getIsIdSelectedFlag = { it in (state as MainState.Ready.Selecting).selectedIds },
//														getMediaTrailingContentOnSelectMode = {
//															log("getMediaTrailingContentOnSelectMode: $name")
//															Icon(Icons.Filled.CheckBoxOutlineBlank, null)
//														},
//														getMediaTrailingContentOnSelectModeIsSelected = {
//															log("getMediaTrailingContentOnSelectModeIsSelected: $name")
//															Icon(Icons.Filled.CheckBox, null)
//														}
//													)
//												}
//											)
//										}
//									)
//								}
//							}
//						}
//					)
//				}
//			)
//		}
//	)
//}
//
//@Composable
//private fun MainScreen(getMainScaffold: @Composable () -> Unit) {
//	return getMainScaffold()
//}
//
//@Composable
//@OptIn(ExperimentalMaterial3Api::class)
//private fun MainScaffold(
//	getMainTopBar: @Composable () -> Unit,
//	getMainContent: @Composable (PaddingValues) -> Unit
//) {
//	return Scaffold(
//		modifier = Modifier,
//		topBar = { getMainTopBar() },
//		bottomBar = {},
//		content = { getMainContent(it) }
//	)
//}
//
//@Composable
//fun MainTabRow(
//	getSelectedTabIndex: () -> Int,
//	getVaultTab: @Composable () -> Unit,
//	getDeviceTab: @Composable () -> Unit
//) {
//	return TabRow(
//		selectedTabIndex = getSelectedTabIndex(),
//		containerColor = MaterialTheme.colorScheme.primary,
//		indicator = {},
//		divider = {}
//	) {
//		getVaultTab()
//		getDeviceTab()
//	}
//}
//
//@Composable
//fun VaultTab(
//	getSelectedFlag: () -> Boolean,
//	getEnabledFlag: () -> Boolean,
//	getOnClickLambda: () -> Unit,
//	getTabLabel: @Composable () -> Unit
//) {
//	return Tab(
//		selected = getSelectedFlag(),
//		onClick = { getOnClickLambda() },
//		enabled = getEnabledFlag(),
//		selectedContentColor = MaterialTheme.colorScheme.background,
//		unselectedContentColor = MaterialTheme.colorScheme.inversePrimary,
//		text = { getTabLabel() }
//	)
//}
//
//@Composable
//fun DeviceTab(
//	getSelectedFlag: () -> Boolean,
//	getEnabledFlag: () -> Boolean,
//	getOnClickLambda: () -> Unit,
//	getTabLabel: @Composable () -> Unit
//) {
//	return Tab(
//		selected = getSelectedFlag(),
//		onClick = { getOnClickLambda() },
//		enabled = getEnabledFlag(),
//		selectedContentColor = MaterialTheme.colorScheme.background,
//		unselectedContentColor = MaterialTheme.colorScheme.inversePrimary,
//		text = { getTabLabel() }
//	)
//}
//
//@Composable
//fun MainTopBar(getMainTabRow: @Composable () -> Unit) {
//	getMainTabRow()
//}
//
//@Composable
//@OptIn(ExperimentalFoundationApi::class)
//private fun MainContent(
//	paddingValues: PaddingValues,
//	pagerState: PagerState,
//	getVaultView: @Composable () -> Unit,
//	getDeviceView: @Composable () -> Unit
//) {
//	return HorizontalPager(state = pagerState, contentPadding = paddingValues, beyondBoundsPageCount = 1) {
//		when (it) {
//			0 -> getVaultView()
//			else -> getDeviceView()
//		}
//	}
//}
//
//@Composable
//fun VaultView(
//	getIntComposable: @Composable () -> Unit,
//	getStrComposable: @Composable () -> Unit,
//	getIntButton: @Composable () -> Unit,
//	getStrButton: @Composable () -> Unit,
//) {
//	return Column(Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
//		getIntComposable()
//		getStrComposable()
//		getIntButton()
//		getStrButton()
//	}
//}
//
//@Composable
//fun DeviceView(
//	getListOfDeviceModels: () -> List<Plain.Companion.TileHeader>,
//	getDirectoryTile: @Composable (Long) -> Unit,
//	getMediaTile: @Composable (Long) -> Unit,
//) {
//	return LazyColumn(
//		modifier = Modifier.fillMaxSize(),
//		state = rememberLazyListState(),
//		verticalArrangement = Arrangement.SpaceEvenly,
//		horizontalAlignment = Alignment.CenterHorizontally
//	) {
//		items(
//			items = getListOfDeviceModels(),
//			key = { it.id },
//			contentType = { it.isDirectory }
//		) { if (it.isDirectory) getDirectoryTile(it.id) else getMediaTile(it.id) }
//	}
//}
//
//@Composable
//fun MediaTrailingContent(
//	getOnSelectedModeFlag: () -> Boolean,
//	getIsIdSelectedFlag: () -> Boolean,
//	getMediaTrailingContentOnSelectMode: @Composable () -> Unit,
//	getMediaTrailingContentOnSelectModeIsSelected: @Composable () -> Unit
//) {
//	return when {
//		getOnSelectedModeFlag() && getIsIdSelectedFlag() -> getMediaTrailingContentOnSelectModeIsSelected()
//		getOnSelectedModeFlag() -> getMediaTrailingContentOnSelectMode()
//		else -> Unit
//	}
//}
//
//@Composable
//fun MediaTile(
//	onLongClick: () -> Unit,
//	onClick: () -> Unit,
//	getHeadlineText: @Composable () -> Unit,
//	getMediaSupportingText: @Composable () -> Unit,
//	getMediaLeadingContent: @Composable () -> Unit,
//	getMediaTrailingContent: @Composable () -> Unit
//) {
//	@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
//	ListItem(
//		modifier = Modifier.height(64.dp).combinedClickable(onLongClick = { onLongClick() }, onClick = { onClick() }),
//		overlineText = null,
//		headlineText = { getHeadlineText() },
//		supportingText = { getMediaSupportingText() },
//		leadingContent = { getMediaLeadingContent() },
//		trailingContent = { getMediaTrailingContent() },
//		shadowElevation = 4.dp
//	)
//}
//
//@Composable
//fun DirTile(
//	onLongClick: () -> Unit,
//	onClick: () -> Unit,
//	getHeadlineText: @Composable () -> Unit,
//	getDirSupportingText: @Composable () -> Unit,
//	getDirLeadingContent: @Composable () -> Unit,
//	getDirTrailingContent: @Composable () -> Unit
//) {
//	@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
//	ListItem(
//		modifier = Modifier.height(64.dp).combinedClickable(onLongClick = { onLongClick() }, onClick = { onClick() }),
//		overlineText = null,
//		headlineText = { getHeadlineText() },
//		supportingText = { getDirSupportingText() },
//		leadingContent = { getDirLeadingContent() },
//		trailingContent = { getDirTrailingContent() },
//		shadowElevation = 4.dp
//	)
//}
