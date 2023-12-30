package com.canisy.vaultfive.ui.route.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalFoundationApi::class)
@Singleton
class ContentPagerState(override val pageCount: Int = 2) : PagerState(0, 0f)

@Stable
data class TestState(
	val hello: String = "",
	val world: String = "",
	val increasing: String = "",
	val decreasing: String = "",
)

data class TestInternalState(
	val isHello: Boolean = true,
	val isWorld: Boolean = true,
	val incrementCounter: Int = 0,
	val decrementCounter: Int = 3000,
)

sealed interface TestAction {
	data object SetHello : TestAction
	data object SetWorld : TestAction
	data object Increment : TestAction
	data object Decrement : TestAction
	data class ScrollToPage(val targetPage: Int) : TestAction
}


@HiltViewModel
class TestViewModel @Inject constructor() : ViewModel() {
	private val testMutex = Mutex()
	private val internalContentPagerState = MutableStateFlow(ContentPagerState())
	private val internalState = MutableStateFlow(TestInternalState())
	private val internalUiState = MutableStateFlow(TestState())

	private suspend fun update(newState: TestState) = withContext(Dispatchers.IO) {
		testMutex.withLock { withContext(Dispatchers.Main) { internalUiState.update { newState } } }
	}

	init {
		viewModelScope.launch(Dispatchers.IO) {
			internalState.collect { internal ->
				TestState(
					hello = if (internal.isHello) "Hello World" else "Nope",
					world = if (internal.isWorld) "World Hello" else "Nah",
					increasing = internal.incrementCounter.toString(),
					decreasing = internal.decrementCounter.toString(),
				).let { update(it) }
			}
		}
	}

	private fun decrement() {
		internalState.update { it.copy(decrementCounter = internalState.value.decrementCounter.minus(1)) }
	}

	private fun increment() {
		internalState.update { it.copy(incrementCounter = internalState.value.incrementCounter.plus(1)) }
	}

	private fun setHello() {
		internalState.update { it.copy(isHello = !internalState.value.isHello) }
	}

	private fun setWorld() {
		internalState.update { it.copy(isWorld = !internalState.value.isWorld) }
	}

	private suspend fun scrollToPage(targetPage: Int) {
		withContext(Dispatchers.Main) { internalContentPagerState.value.scrollToPage(targetPage) }
	}

	val uiState = internalUiState.asStateFlow()
	val contentPagerState = internalContentPagerState.asStateFlow()

	fun dispatch(testAction: TestAction) {
		viewModelScope.launch(Dispatchers.IO) {
			when (testAction) {
				TestAction.Decrement -> decrement()
				TestAction.Increment -> increment()
				TestAction.SetHello -> setHello()
				TestAction.SetWorld -> setWorld()
				is TestAction.ScrollToPage -> scrollToPage(testAction.targetPage)
			}
		}
	}
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TestComposer(vm: TestViewModel = hiltViewModel()) {
	val state by vm.uiState.collectAsStateWithLifecycle()
	val contentPagerState by vm.contentPagerState.collectAsStateWithLifecycle()
	Scaffold(
		topBar = {
			val selectedTabIndex = contentPagerState.currentPage
			val firstPageSelected = selectedTabIndex == 0
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
					Tab(
						selected = firstPageSelected,
						onClick = { vm.dispatch(TestAction.ScrollToPage(0)) },
						enabled = !firstPageSelected,
						text = {
							Text(
								text = "Page One",
								color = MaterialTheme.colorScheme.run { if (firstPageSelected) inverseOnSurface else inversePrimary }
							)
						}
					)
					Tab(
						selected = !firstPageSelected,
						onClick = { vm.dispatch(TestAction.ScrollToPage(1)) },
						enabled = firstPageSelected,
						text = {
							Text(
								text = "Page Two",
								color = MaterialTheme.colorScheme.run { if (!firstPageSelected) inverseOnSurface else inversePrimary }
							)
						}
					)
				}
			)
		},
		content = { padding ->
			HorizontalPager(
				state = contentPagerState,
				contentPadding = padding,
				beyondBoundsPageCount = 1,
				pageContent = {
					when (it) {
						0 -> FirstTestPage(
							getFirstLabel = { Text(text = state.hello) },
							getSecondLabel = { Text(text = state.increasing) },
							getFirstButton = { ElevatedButton(onClick = { vm.dispatch(TestAction.SetHello) }, content = { Text(text = "Set") }) },
							getSecondButton = {
								ElevatedButton(onClick = { vm.dispatch(TestAction.Increment) }, content = { Text(text = "Increment") })
							}
						)

						else -> SecondTestPage(
							getFirstLabel = { Text(text = state.world) },
							getSecondLabel = { Text(text = state.decreasing) },
							getFirstButton = { ElevatedButton(onClick = { vm.dispatch(TestAction.SetWorld) }, content = { Text(text = "Set") }) },
							getSecondButton = {
								ElevatedButton(onClick = { vm.dispatch(TestAction.Decrement) }, content = { Text(text = "Decrement") })
							}
						)
					}
				}
			)
		}
	)
}

@Composable
fun SecondTestPage(
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
fun FirstTestPage(
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