//@file:JvmName("MainComposerKt")
//
//package com.canisy.vaultfive.ui.route.main
//
//import android.app.Activity
//import androidx.activity.compose.BackHandler
//import androidx.compose.foundation.ExperimentalFoundationApi
//import androidx.compose.foundation.combinedClickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyListState
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.pager.HorizontalPager
//import androidx.compose.foundation.pager.PagerState
//import androidx.compose.foundation.pager.rememberPagerState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.outlined.NavigateNext
//import androidx.compose.material.icons.outlined.CheckBox
//import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
//import androidx.compose.material.icons.rounded.Folder
//import androidx.compose.material3.ElevatedButton
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.ListItem
//import androidx.compose.material3.LocalTextStyle
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Tab
//import androidx.compose.material3.TabRow
//import androidx.compose.material3.TabRowDefaults
//import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.bumptech.glide.integration.compose.CrossFade
//import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
//import com.bumptech.glide.integration.compose.GlideImage
//import com.canisy.vaultfive.core.model.Plain
//import com.canisy.vaultfive.core.viewmodels.DeviceHeader
//import com.canisy.vaultfive.core.viewmodels.DeviceState
//import com.canisy.vaultfive.core.viewmodels.MainViewModel
//import kotlinx.coroutines.launch
//
//@Composable
//fun MainTabRow(
//	selectedTabIndex: Int,
//	getFirstTab: @Composable () -> Unit,
//	getSecondTab: @Composable () -> Unit
//) {
//	TabRow(
//		selectedTabIndex = selectedTabIndex,
//		containerColor = MaterialTheme.colorScheme.primary,
//		indicator = {
//			TabRowDefaults.Indicator(
//				Modifier.tabIndicatorOffset(it[selectedTabIndex]),
//				color = MaterialTheme.colorScheme.background
//			)
//		}
//	) { getFirstTab(); getSecondTab() }
//}
//
//@Composable
//fun MainTab(isSelected: Boolean, onClick: () -> Unit, getLabel: @Composable () -> Unit) {
//	Tab(selected = isSelected, onClick = { onClick() }, enabled = !isSelected, text = { getLabel() })
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MainScaffold(getTopBar: @Composable () -> Unit, getContent: @Composable (PaddingValues) -> Unit) {
//	Scaffold(topBar = { getTopBar() }, content = { getContent(it) })
//}
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun MainComposer(vm: MainViewModel = hiltViewModel()) {
//	val pagerState = rememberPagerState(0, 0f) { 2 }
//	MainScaffold(
//		getTopBar = {
//			val firstPageIsSelected by remember { derivedStateOf { pagerState.currentPage == 0 } }
//			val scope = rememberCoroutineScope()
//			val scroll: (Int) -> Unit = remember { { scope.launch { pagerState.animateScrollToPage(it) } } }
//			MainTabRow(
//				selectedTabIndex = pagerState.currentPage,
//				getFirstTab = {
//					MainTab(
//						isSelected = firstPageIsSelected,
//						onClick = { scroll(0) },
//						getLabel = {
//							MainTabLabel(
//								text = "Vault",
//								color = MaterialTheme.colorScheme.run { if (firstPageIsSelected) inverseOnSurface else inversePrimary }
//							)
//						}
//					)
//				},
//				getSecondTab = {
//					MainTab(
//						isSelected = !firstPageIsSelected,
//						onClick = { scroll(1) },
//						getLabel = {
//							MainTabLabel(
//								text = "Device",
//								color = MaterialTheme.colorScheme.run { if (firstPageIsSelected) inversePrimary else inverseOnSurface }
//							)
//						}
//					)
//				}
//			)
//		},
//		getContent = { paddingValues ->
//			MainContent(
//				paddingValues = paddingValues,
//				pagerState = pagerState,
//				getFirstPage = {
//					BackHandler(pagerState.currentPage == 0) {}
//					var counter by remember { mutableIntStateOf(0) }
//					var isHello by remember { mutableStateOf(true) }
//					VaultPage(
//						getFirstLabel = { MainTabLabel(text = counter.toString()) },
//						getSecondLabel = { MainTabLabel(text = if (isHello) "Hello World" else "Nope") },
//						getFirstButton = { ButtonLeaf(onClick = { counter++ }, text = "Increment") },
//						getSecondButton = { ButtonLeaf(onClick = { isHello = !isHello }, text = "SecondButton") },
//					)
//				},
//				getSecondPage = {
//					val deviceState by vm.deviceState.collectAsStateWithLifecycle()
//					var selecting by remember { mutableStateOf(false) }
//					var currentPath by remember { mutableStateOf<String?>(null) }
//					val traversals = remember { mutableStateListOf<Triple<String?, Int, Int>>() }
//					val selected = remember { mutableStateListOf<Long>() }
//					val context = remember { LocalContext }.current
//					val lazyListState = rememberLazyListState()
//					val scope = rememberCoroutineScope()
//					when (val s = deviceState) {
//						DeviceState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(text = "Error...") }
//						DeviceState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(text = "Loading...") }
//						is DeviceState.Ready -> {
//							BackHandler(pagerState.currentPage == 1) {
//								when {
//									selecting -> selecting = false
//									traversals.isEmpty() -> (context as Activity).moveTaskToBack(true)
//									traversals.isNotEmpty() -> {
//										traversals.removeLast().also { lastTraversed ->
//											scope.launch { vm.refreshState(lastTraversed.first) }.invokeOnCompletion {
//												scope.launch { lazyListState.scrollToItem(lastTraversed.second, lastTraversed.third) }
//												currentPath = lastTraversed.first
//											}
//										}
//									}
//								}
//							}
//							DevicePage(
//								lazyListState = lazyListState,
//								header = s.data,
//								getDirCard = {
//									val item = vm.fetchData(it) as Plain.Directory
//									DirCard(
//										name = item.name,
//										date = item.date,
//										items = item.childCount.toString(),
//										onClick = {
//											if (selecting) selected.add(it) else scope.launch {
//												val path = (vm.fetchData(it) as Plain).path
//												traversals.add(
//													Triple(
//														currentPath,
//														lazyListState.firstVisibleItemIndex,
//														lazyListState.firstVisibleItemScrollOffset
//													)
//												)
//												vm.refreshState(path)
//												currentPath = path
//											}.invokeOnCompletion { scope.launch { lazyListState.scrollToItem(0, 0) } }
//										},
//										onLongClick = { selecting = true },
//										getTrailing = { DirTrailing(isSelecting = selecting, isSelected = it in selected) }
//									)
//								},
//								getMediaCard = {
//									val item = vm.fetchData(it) as Plain.Media
//									MediaCard(
//										name = item.name,
//										date = item.date,
//										size = item.size,
//										path = item.path,
//										onClick = { },
//										onLongClick = { selecting = true },
//										getTrailing = { MediaTrailing(isSelecting = selecting, isSelected = it in selected) }
//									)
//								}
//							)
//						}
//					}
//				}
//			)
//		}
//	)
//}
//
//@Composable
//fun DirTrailing(isSelecting: Boolean, isSelected: Boolean) {
//	Icon(
//		when {
//			isSelecting && isSelected -> Icons.Outlined.CheckBox
//			isSelecting -> Icons.Outlined.CheckBoxOutlineBlank
//			else -> Icons.AutoMirrored.Outlined.NavigateNext
//		}, null
//	)
//}
//
//@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
//@Composable
//fun DirCard(
//	name: String,
//	date: String,
//	items: String,
//	onClick: () -> Unit,
//	onLongClick: () -> Unit,
//	getTrailing: @Composable () -> Unit,
//) {
//	ListItem(
//		modifier = Modifier.height(64.dp).combinedClickable(onLongClick = { onLongClick() }, onClick = { onClick() }),
//		headlineText = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
//		supportingText = { Text(text = "$date | Items: $items", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
//		leadingContent = {
//			Icon(
//				imageVector = Icons.Rounded.Folder,
//				contentDescription = null,
//				modifier = Modifier.size(48.dp),
//				tint = MaterialTheme.colorScheme.primary
//			)
//		},
//		trailingContent = getTrailing,
//		shadowElevation = 4.dp
//	)
//}
//
//@Composable
//fun MediaTrailing(isSelecting: Boolean, isSelected: Boolean) {
//	when {
//		isSelecting && isSelected -> Icon(Icons.Outlined.CheckBox, contentDescription = null)
//		isSelecting -> Icon(Icons.Outlined.CheckBoxOutlineBlank, contentDescription = null)
//		else -> {}
//	}
//}
//
//@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
//@Composable
//fun MediaCard(
//	name: String,
//	date: String,
//	size: String,
//	path: String,
//	onClick: () -> Unit,
//	onLongClick: () -> Unit,
//	getTrailing: @Composable () -> Unit,
//) {
//	ListItem(
//		modifier = Modifier.height(64.dp).combinedClickable(onLongClick = { onLongClick() }, onClick = { onClick() }),
//		headlineText = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
//		supportingText = { Text(text = "$date | Size: $size", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
//		leadingContent = {
//			@OptIn(ExperimentalGlideComposeApi::class)
//			GlideImage(
//				model = path,
//				contentDescription = name,
//				modifier = (Modifier.size(48.dp)).clip(RoundedCornerShape(4.dp)),
//				contentScale = ContentScale.Crop,
//				transition = CrossFade
//			)
//		},
//		trailingContent = getTrailing,
//		shadowElevation = 4.dp
//	)
//}
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun MainContent(
//	paddingValues: PaddingValues,
//	pagerState: PagerState,
//	getFirstPage: @Composable () -> Unit,
//	getSecondPage: @Composable () -> Unit
//) {
//	HorizontalPager(
//		state = pagerState,
//		contentPadding = paddingValues,
//		beyondBoundsPageCount = 1,
//		pageContent = { if (it == 0) getFirstPage() else getSecondPage() }
//	)
//}
//
//@Composable
//fun VaultPage(
//	getFirstLabel: @Composable () -> Unit,
//	getSecondLabel: @Composable () -> Unit,
//	getFirstButton: @Composable () -> Unit,
//	getSecondButton: @Composable () -> Unit,
//) {
//	Column(
//		modifier = Modifier.fillMaxSize(),
//		verticalArrangement = Arrangement.Center,
//		horizontalAlignment = Alignment.CenterHorizontally
//	) {
//		getFirstLabel()
//		Spacer(modifier = Modifier.height(16.dp))
//		getSecondLabel()
//		Spacer(modifier = Modifier.height(64.dp))
//		getFirstButton()
//		Spacer(modifier = Modifier.height(16.dp))
//		getSecondButton()
//	}
//}
//
//@Composable
//fun DevicePage(
//	lazyListState: LazyListState,
//	header: List<DeviceHeader>,
//	getDirCard: @Composable (Long) -> Unit,
//	getMediaCard: @Composable (Long) -> Unit
//) {
//	LazyColumn(
//		state = lazyListState,
//		content = {
//			items(
//				items = header,
//				key = { it.id },
//				contentType = { it.isDir },
//				itemContent = { if (it.isDir) getDirCard(it.id) else getMediaCard(it.id) }
//			)
//		}
//	)
//}
//
//@Composable
//fun MainTabLabel(text: String, color: Color? = null, style: TextStyle? = null) {
//	Text(
//		text = text,
//		maxLines = 1,
//		overflow = TextOverflow.Ellipsis,
//		color = color ?: Color.Unspecified,
//		style = style ?: LocalTextStyle.current
//	)
//}
//
//@Composable
//fun ButtonLeaf(onClick: () -> Unit, text: String) = ElevatedButton(onClick = { onClick() }) { Text(text = text) }