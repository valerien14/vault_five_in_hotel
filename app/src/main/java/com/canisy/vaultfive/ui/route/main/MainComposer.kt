package com.canisy.vaultfive.ui.route.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.canisy.vaultfive.core.viewmodels.Action
import com.canisy.vaultfive.core.viewmodels.DeviceHeader
import com.canisy.vaultfive.core.viewmodels.DeviceState
import com.canisy.vaultfive.core.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainComposer(vm: MainViewModel = hiltViewModel()) {
	val contentPagerState by vm.mainPagerState.collectAsStateWithLifecycle()
	Scaffold(
		topBar = {
			val selectedTabIndex = contentPagerState.currentPage
			TabRow(
				selectedTabIndex = selectedTabIndex,
				containerColor = MaterialTheme.colorScheme.primary,
				indicator = {
					TabRowDefaults.Indicator(
						Modifier.tabIndicatorOffset(it[selectedTabIndex]),
						color = MaterialTheme.colorScheme.background
					)
				},
				tabs = {
					val firstPageSelected = selectedTabIndex == 0
					Tab(
						selected = firstPageSelected,
						onClick = { vm.dispatch(Action.TabClick(0)) },
						enabled = !firstPageSelected,
						text = {
							Text(
								text = "Vault",
								color = MaterialTheme.colorScheme.run { if (firstPageSelected) inverseOnSurface else inversePrimary }
							)
						}
					)
					Tab(
						selected = !firstPageSelected,
						onClick = { vm.dispatch(Action.TabClick(1)) },
						enabled = firstPageSelected,
						text = {
							Text(
								text = "Device",
								color = MaterialTheme.colorScheme.run { if (!firstPageSelected) inverseOnSurface else inversePrimary }
							)
						}
					)
				}
			)
		},
		content = { paddingValues ->
			HorizontalPager(
				state = contentPagerState,
				contentPadding = paddingValues,
				beyondBoundsPageCount = 1,
				pageContent = { page ->
					if (page == 0) Box(Modifier.fillMaxSize(), Alignment.Center) { Text(text = "Vault Page") }
					else {
						val deviceState by vm.deviceState.collectAsStateWithLifecycle()
						val deviceListState by vm.deviceListState.collectAsStateWithLifecycle()
						when (val state = deviceState) {
							DeviceState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(text = "Error...") }
							DeviceState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(text = "Loading...") }
							is DeviceState.Ready -> {
								LazyColumn(
									state = deviceListState,
									verticalArrangement = Arrangement.Top,
									horizontalAlignment = Alignment.Start,
									content = {
										items(
											items = state.data.getHeader(),
											key = { it.id },
											contentType = { it is DeviceHeader.Dir },
											itemContent = {
												if (it is DeviceHeader.Dir) {
													val item = state.data.getDir(it.id)
													ListItem(
														modifier = Modifier.height(64.dp).combinedClickable(
															onLongClick = { vm.dispatch(Action.TileLongClick) },
															onClick = { vm.dispatch(Action.DirTileClick(it.id)) }
														),
														headlineText = {
															Text(
																text = item.name,
																maxLines = 1,
																overflow = TextOverflow.Ellipsis
															)
														},
														supportingText = {
															Text(
																text = "${item.date} | Items: ${item.childCount}",
																color = Color.Gray,
																style = MaterialTheme.typography.bodySmall
															)
														},
														leadingContent = {
															Icon(
																imageVector = Icons.Rounded.Folder,
																contentDescription = null,
																modifier = Modifier.size(48.dp),
																tint = MaterialTheme.colorScheme.primary
															)
														},
														trailingContent = {
															Icon(
																if (state is DeviceState.Ready.Selecting) {
																	if (state.selectedIds.isSelected(it.id)) Icons.Outlined.CheckBox
																	else Icons.Outlined.CheckBoxOutlineBlank
																} else Icons.AutoMirrored.Outlined.NavigateNext,
																null
															)
														},
														shadowElevation = 4.dp
													)
												} else {
													val item = state.data.getMedia(it.id)
													ListItem(
														headlineText = { Text(text = item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
														shadowElevation = 4.dp,
														modifier = Modifier.height(64.dp).combinedClickable(
															onLongClick = { vm.dispatch(Action.TileLongClick) },
															onClick = { vm.dispatch(Action.MediaTileClick) }
														),
														supportingText = {
															Text(
																text = "${item.date} | Size: ${item.size}",
																color = Color.Gray,
																style = MaterialTheme.typography.bodySmall
															)
														},
														leadingContent = {
															@OptIn(ExperimentalGlideComposeApi::class)
															GlideImage(
																model = item.path,
																contentDescription = item.name,
																modifier = (Modifier.size(48.dp)).clip(RoundedCornerShape(4.dp)),
																contentScale = ContentScale.Crop,
																transition = CrossFade
															)
														},
														trailingContent = {
															if (state is DeviceState.Ready.Selecting) {
																if (state.selectedIds.isSelected(it.id)) Icon(Icons.Outlined.CheckBox, null)
																else Icon(Icons.Outlined.CheckBoxOutlineBlank, contentDescription = null)
															} else Unit
														}
													)
												}
											}
										)
									}
								)
							}
						}
					}
				}
			)
		}
	)
}