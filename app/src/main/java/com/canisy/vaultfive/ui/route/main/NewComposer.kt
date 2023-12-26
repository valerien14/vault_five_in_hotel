package com.canisy.vaultfive.ui.route.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.canisy.vaultfive.core.model.Plain
import com.canisy.vaultfive.core.viewmodels.DeviceCardHeader
import com.canisy.vaultfive.core.viewmodels.NewDeviceAction
import com.canisy.vaultfive.core.viewmodels.NewDeviceState
import com.canisy.vaultfive.core.viewmodels.NewViewModel
import kotlinx.coroutines.launch

@Composable
fun NewTabRow(
	selectedTabIndex: Int,
	getFirstTab: @Composable () -> Unit,
	getSecondTab: @Composable () -> Unit
) {
	TabRow(
		selectedTabIndex = selectedTabIndex,
		containerColor = MaterialTheme.colorScheme.primary,
		indicator = {
			TabRowDefaults.Indicator(
				Modifier.tabIndicatorOffset(it[selectedTabIndex]),
				color = MaterialTheme.colorScheme.background
			)
		}
	) { getFirstTab(); getSecondTab() }
}

@Composable
fun NewTab(isSelected: Boolean, onClick: () -> Unit, getLabel: @Composable () -> Unit) {
	Tab(selected = isSelected, onClick = { onClick() }, enabled = !isSelected, text = { getLabel() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScaffold(getTopBar: @Composable () -> Unit, getContent: @Composable (PaddingValues) -> Unit) {
	Scaffold(topBar = { getTopBar() }, content = { getContent(it) })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewComposer(vm: NewViewModel = hiltViewModel()) {
	val pagerState = rememberPagerState(0, 0f) { 2 }
	NewScaffold(
		getTopBar = {
			val firstPageIsSelected by remember { derivedStateOf { pagerState.currentPage == 0 } }
			val scope = rememberCoroutineScope()
			val scroll: (Int) -> Unit = remember { { scope.launch { pagerState.animateScrollToPage(it) } } }
			NewTabRow(
				selectedTabIndex = pagerState.currentPage,
				getFirstTab = {
					NewTab(
						isSelected = firstPageIsSelected,
						onClick = { scroll(0) },
						getLabel = {
							LabelLeaf(
								text = "Vault",
								color = MaterialTheme.colorScheme.run { if (firstPageIsSelected) inverseOnSurface else inversePrimary }
							)
						}
					)
				},
				getSecondTab = {
					NewTab(
						isSelected = !firstPageIsSelected,
						onClick = { scroll(1) },
						getLabel = {
							LabelLeaf(
								text = "Device",
								color = MaterialTheme.colorScheme.run { if (firstPageIsSelected) inversePrimary else inverseOnSurface }
							)
						}
					)
				}
			)
		},
		getContent = { paddingValues ->
			NewPager(
				paddingValues = paddingValues,
				pagerState = pagerState,
				getFirstPage = {
					BackHandler(pagerState.currentPage == 0) {}
					var counter by remember { mutableIntStateOf(0) }
					var isHello by remember { mutableStateOf(true) }
					NewFirstPage(
						getFirstLabel = { LabelLeaf(text = counter.toString()) },
						getSecondLabel = { LabelLeaf(text = if (isHello) "Hello World" else "Nope") },
						getFirstButton = { ButtonLeaf(onClick = { counter++ }, text = "Increment") },
						getSecondButton = { ButtonLeaf(onClick = { isHello = !isHello }, text = "SecondButton") },
					)
				},
				getSecondPage = {
					val deviceState by vm.deviceState.collectAsStateWithLifecycle()
					BackHandler(pagerState.currentPage == 1) { vm.dispatch(NewDeviceAction.BackButtonPress) }
					when (val s = deviceState) {
						NewDeviceState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(text = "Error...") }
						NewDeviceState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(text = "Loading...") }
						is NewDeviceState.Ready -> {
							val lazyListState = rememberLazyListState()
							LaunchedEffect(Unit) { s.machineScroll?.let { lazyListState.scrollToItem(it.first, it.second) } }
							NewSecondPage(
								lazyListState = lazyListState,
								header = s.data,
								getThisCard = {
									val item = s.getEntity(it) as Plain.Directory
									DirCardLeaf(
										name = item.name,
										date = item.date,
										items = item.childCount.toString(),
										onClick = {
											vm.dispatch(
												NewDeviceAction.CardClick(
													scrollOffset = lazyListState.run { firstVisibleItemIndex to firstVisibleItemScrollOffset },
													id = it,
												)
											)
										},
										onLongClick = {

										}
									)
								},
								getThatCard = {
									val item = s.getEntity(it) as Plain.Media
									MediaCardLeaf(
										name = item.name,
										date = item.date,
										size = item.size,
										path = item.path,
										onClick = { },
										onLongClick = { }
									)
								}
							)
						}
					}
				}
			)
		}
	)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DirCardLeaf(
	name: String,
	date: String,
	items: String,
	onClick: () -> Unit,
	onLongClick: () -> Unit,
) {
	ListItem(
		modifier = Modifier.height(64.dp).combinedClickable(onLongClick = { onLongClick() }, onClick = { onClick() }),
		headlineText = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
		supportingText = { Text(text = "$date | Items: $items", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
		leadingContent = {
			Icon(
				imageVector = Icons.Rounded.Folder,
				contentDescription = null,
				modifier = Modifier.size(48.dp),
				tint = MaterialTheme.colorScheme.primary
			)
		},
		trailingContent = { Icon(Icons.AutoMirrored.Filled.NavigateNext, null) },
		shadowElevation = 4.dp
	)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaCardLeaf(
	name: String,
	date: String,
	size: String,
	path: String,
	onClick: () -> Unit,
	onLongClick: () -> Unit,
) {
	ListItem(
		modifier = Modifier.height(64.dp).combinedClickable(onLongClick = { onLongClick() }, onClick = { onClick() }),
		headlineText = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
		supportingText = { Text(text = "$date | Size: $size", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
		leadingContent = {
			@OptIn(ExperimentalGlideComposeApi::class)
			GlideImage(
				model = path,
				contentDescription = name,
				modifier = (Modifier.size(48.dp)).clip(RoundedCornerShape(4.dp)),
				contentScale = ContentScale.Crop,
				transition = CrossFade
			)
		},
		trailingContent = { Icon(Icons.AutoMirrored.Filled.NavigateNext, null) },
		shadowElevation = 4.dp
	)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewPager(
	paddingValues: PaddingValues,
	pagerState: PagerState,
	getFirstPage: @Composable () -> Unit,
	getSecondPage: @Composable () -> Unit
) {
	HorizontalPager(
		state = pagerState,
		contentPadding = paddingValues,
		beyondBoundsPageCount = 1,
		pageContent = { if (it == 0) getFirstPage() else getSecondPage() }
	)
}

@Composable
fun NewFirstPage(
	getFirstLabel: @Composable () -> Unit,
	getSecondLabel: @Composable () -> Unit,
	getFirstButton: @Composable () -> Unit,
	getSecondButton: @Composable () -> Unit,
) {
	Column(
		modifier = Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		getFirstLabel()
		Spacer(modifier = Modifier.height(16.dp))
		getSecondLabel()
		Spacer(modifier = Modifier.height(64.dp))
		getFirstButton()
		Spacer(modifier = Modifier.height(16.dp))
		getSecondButton()
	}
}

@Composable
fun NewSecondPage(
	lazyListState: LazyListState,
	header: List<DeviceCardHeader>,
	getThisCard: @Composable (Long) -> Unit,
	getThatCard: @Composable (Long) -> Unit
) {
	LazyColumn(
		state = lazyListState,
		content = {
			items(
				items = header,
				key = { it.id },
				contentType = { it.isDir },
				itemContent = { if (it.isDir) getThisCard(it.id) else getThatCard(it.id) }
			)
		}
	)
}

@Composable
fun LabelLeaf(text: String, color: Color? = null, style: TextStyle? = null) {
	Text(
		text = text,
		maxLines = 1,
		overflow = TextOverflow.Ellipsis,
		color = color ?: Color.Unspecified,
		style = style ?: LocalTextStyle.current
	)
}

@Composable
fun ButtonLeaf(onClick: () -> Unit, text: String) = ElevatedButton(onClick = { onClick() }) { Text(text = text) }