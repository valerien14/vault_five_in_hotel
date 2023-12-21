package com.canisy.vaultfive.ui.route.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun NewTabRow(
	selectedTabIndex: Int,
	getFirstTab: @Composable () -> Unit,
	getSecondTab: @Composable () -> Unit
) {
	TabRow(selectedTabIndex = selectedTabIndex) {
		getFirstTab()
		getSecondTab()
	}
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
fun NewComposer() {
	val pagerState = rememberPagerState(0, 0f) { 2 }
	NewScaffold(
		getTopBar = {
			val firstPageIsSelected by remember { derivedStateOf { pagerState.currentPage == 0 } }
			val scope = rememberCoroutineScope()
			val scroll: (Int) -> Unit = remember { { scope.launch { pagerState.animateScrollToPage(it) } } }
			NewTabRow(
				selectedTabIndex = pagerState.currentPage,
				getFirstTab = { NewTab(isSelected = firstPageIsSelected, onClick = { scroll(0) }) { Text(text = "First Tab") } },
				getSecondTab = { NewTab(isSelected = !firstPageIsSelected, onClick = { scroll(1) }) { Text(text = "Second Tab") } }
			)
		},
		getContent = {
			NewPager(
				paddingValues = it,
				pagerState = pagerState,
				getFirstPage = { NewPage { Text(text = "First Page") } },
				getSecondPage = { NewPage { Text(text = "Second Page") } }
			)
		}
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
		pageContent = { if (it == 0) getFirstPage() else getSecondPage() }
	)
}

@Composable
fun NewPage(getLabel: @Composable () -> Unit) = Box(Modifier.fillMaxSize(), Alignment.Center) { getLabel() }