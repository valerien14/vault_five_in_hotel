package com.canisy.vaultfive.core.viewmodels

import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canisy.vaultfive.core.model.Plain
import com.canisy.vaultfive.core.util.DeviceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalFoundationApi::class)
class MainPagerState(override val pageCount: Int = 2) : PagerState(0, 0f)

@Immutable
data class MainRawState(val deleteMe: Boolean = false)

@Immutable
data class MainUiState(val deleteMe: Boolean = false)

@Immutable
sealed interface DeviceHeader {
	val id: Long

	data class Dir(override val id: Long) : DeviceHeader
	data class Media(override val id: Long) : DeviceHeader
}

@Immutable
sealed interface DeviceState {
	@Immutable
	data object Error : DeviceState

	@Immutable
	data object Loading : DeviceState

	@Immutable
	sealed interface Ready : DeviceState {
		val data: DeviceData

		data class Browsing(override val data: DeviceData) : Ready
		data class Selecting(override val data: DeviceData, val selectedIds: SelectedIds) : Ready
	}
}

sealed interface Action {
	data object TileLongClick : Action
	data object MediaTileClick : Action
	data class DirTileClick(val id: Long) : Action
	data class TabClick(val targetPage: Int) : Action
}

@JvmInline
@Immutable
value class SelectedIds(private val ids: List<Long>) {
	fun isSelected(id: Long) = id in this.ids
	fun length() = this.ids.size
	fun add(id: Long) = SelectedIds(this.ids.plus(id))
	fun remove(id: Long) = SelectedIds(this.ids.minus(id))
}

@Immutable
data class DeviceData(
	private val deviceDirData: HashMap<Long, Plain.Dir>,
	private val deviceMediaData: HashMap<Long, Plain.Media>,
	private val header: List<DeviceHeader>
) {
	fun getDir(id: Long) = this.deviceDirData[id]!!
	fun getMedia(id: Long) = this.deviceMediaData[id]!!
	fun getHeader() = this.header
}

@HiltViewModel
class MainViewModel @Inject constructor(private val deviceUtil: DeviceUtil) : ViewModel() {
	private val _mainMutex = Mutex()
	private val _mainPagerState = MutableStateFlow(MainPagerState())
	private val _mainRawState = MutableStateFlow(MainRawState())
	private val _mainUiState = MutableStateFlow(MainUiState())
	private val _deviceState = MutableStateFlow<DeviceState>(DeviceState.Loading)
	private val _deviceListState = MutableStateFlow(LazyListState())
	val mainUiState = _mainUiState.asStateFlow()
	val mainPagerState = _mainPagerState.asStateFlow()
	val deviceState = _deviceState.asStateFlow()
	val deviceListState = _deviceListState.asStateFlow()

	private suspend fun update(uiState: MainUiState) {
		_mainMutex.withLock { withContext(Dispatchers.Main) { _mainUiState.update { uiState } } }
	}

	private suspend fun update(deviceState: DeviceState) {
		withContext(Dispatchers.Main) { _deviceState.update { deviceState } }
	}

	private suspend fun buildDeviceModels(path: String?): Pair<List<Plain.Dir>, List<Plain.Media>> {
		return deviceUtil.traverse(path ?: Environment.getExternalStorageDirectory().path)
	}

	private inline fun <reified T> prepareDeviceData(hashMap: HashMap<Long, out Plain>): List<T> {
		return hashMap.map { it.key to it.value }.sortedBy { it.second.name }.map {
			when (T::class) {
				DeviceHeader.Dir::class -> DeviceHeader.Dir(it.first) as T
				DeviceHeader.Media::class -> DeviceHeader.Media(it.first) as T
				else -> throw IllegalArgumentException("Unsupported type: ${T::class.simpleName}")
			}
		}
	}

	private fun mediaTileClick(): DeviceState? {
		TODO("Not yet implemented")
	}

	private fun tileLongClick(): DeviceState? {
		TODO("Not yet implemented")
	}

//	val c = viewModelScope.async(Dispatchers.IO) { prepareDeviceData<DeviceHeader.Dir>(deviceDirData) }
//	val d = viewModelScope.async(Dispatchers.IO) { prepareDeviceData<DeviceHeader.Media>(deviceMediaData) }

	private suspend inline fun dirTileClick(
		state: DeviceState,
		scope: CoroutineScope,
		crossinline getClickedId: () -> Long,
		crossinline setLoadingState: suspend () -> Unit,
		crossinline deviceModelBuilderBuild: suspend (String) -> Pair<List<Plain.Dir>, List<Plain.Media>>,
		crossinline prepareDeviceDirData: (HashMap<Long, Plain.Dir>) -> List<DeviceHeader.Dir>,
		crossinline prepareDeviceMediaData: (HashMap<Long, Plain.Media>) -> List<DeviceHeader.Media>,
		crossinline machineScroll: suspend () -> Unit
	): DeviceState? {
		return when (state) {
			DeviceState.Error -> null
			DeviceState.Loading -> null
			is DeviceState.Ready.Browsing -> {
				run {
					setLoadingState()
					deviceModelBuilderBuild(state.data.getDir(getClickedId()).path).let {
						val a = scope.async { HashMap(it.first.associateBy { i -> i.id }) }
						val b = scope.async { HashMap(it.second.associateBy { i -> i.id }) }
						val c = scope.async { prepareDeviceDirData(a.await()) }
						val d = scope.async { prepareDeviceMediaData(b.await()) }
						DeviceState.Ready.Browsing(DeviceData(a.await(), b.await(), awaitAll(c, d).flatten()))
					}
				}
			}

			is DeviceState.Ready.Selecting -> {
				state.copy(selectedIds = state.selectedIds.run { getClickedId().let { if (isSelected(it)) remove(it) else add(it) } })
			}
		}.also { machineScroll() }
	}

//	private suspend inline fun dirTileClick(
//		state: DeviceState,
//		crossinline getDeviceDirData: () ->  HashMap<Long, Plain.Directory>,
//		crossinline getDeviceMediaData: () ->  HashMap<Long, Plain.Media>,
//		crossinline getClickedId: () -> Long,
//		crossinline setLoadingState: suspend () -> Unit,
//		crossinline deviceModelBuilderBuild: suspend (Long) -> Pair<List<Plain.Directory>, List<Plain.Media>>,
//		crossinline setDeviceDirData: (HashMap<Long, Plain.Directory>) -> Unit,
//		crossinline setDeviceMediaData: (HashMap<Long, Plain.Media>) -> Unit,
//		crossinline prepareDeviceDirData: (HashMap<Long, Plain.Directory>) -> List<DeviceHeader.Dir>,
//		crossinline prepareDeviceMediaData: (HashMap<Long, Plain.Media>) -> List<DeviceHeader.Media>
//	): DeviceState {
//		return when (state) {
//			DeviceState.Error -> state
//			DeviceState.Loading -> state
//			is DeviceState.Ready.Browsing -> {
//				runCatching {
//					setLoadingState()
//					deviceModelBuilderBuild(getClickedId()).let {
//						val a = viewModelScope.launch(Dispatchers.IO) { setDeviceDirData(HashMap(it.first.associateBy { i -> i.id })) }
//						val b = viewModelScope.launch(Dispatchers.IO) { setDeviceMediaData(HashMap(it.second.associateBy { i -> i.id })) }
//						joinAll(a, b)
//						val c = viewModelScope.async(Dispatchers.IO) { prepareDeviceDirData(getDeviceDirData()) }
//						val d = viewModelScope.async(Dispatchers.IO) { prepareDeviceMediaData(getDeviceMediaData()) }
//						Pair(c.await(), d.await())
//					}.let { pair ->
//						DeviceState.Ready.Browsing(
//							data = DeviceData(
//								deviceDirData =, deviceMediaData =, header = listOf()
//
//							),
//							getDirName = { deviceDirData[it]!!.name },
//							getDirDate = { deviceDirData[it]!!.date },
//							getDirItem = { deviceDirData[it]!!.childCount.toString() },
//							getDirPath = { deviceDirData[it]!!.path },
//							getMediaName = { deviceMediaData[it]!!.name },
//							getMediaDate = { deviceMediaData[it]!!.date },
//							getMediaSize = { deviceMediaData[it]!!.size },
//							getMediaPath = { deviceMediaData[it]!!.path }
//						)
//					}
//				}.getOrElse { DeviceState.Error }
//			}
//
//			is DeviceState.Ready.Selecting -> {
//				state.copy(selected = state.selected.run { getClickedId().let { if (it in this) minus(it) else plus(it) } })
//			}
//		}
//	}

	private suspend fun tabClick(targetPageIndex: Int, machineScroll: suspend (Int) -> Unit): DeviceState? {
		machineScroll(targetPageIndex)
		return null
	}

	init {
		viewModelScope.launch(Dispatchers.IO) {
			runCatching {
				buildDeviceModels(null).let {
					val a = viewModelScope.async(Dispatchers.IO) { HashMap(it.first.associateBy { i -> i.id }) }
					val b = viewModelScope.async(Dispatchers.IO) { HashMap(it.second.associateBy { i -> i.id }) }
					val c = viewModelScope.async(Dispatchers.IO) { prepareDeviceData<DeviceHeader.Dir>(a.await()) }
					val d = viewModelScope.async(Dispatchers.IO) { prepareDeviceData<DeviceHeader.Media>(b.await()) }
					DeviceState.Ready.Browsing(DeviceData(a.await(), b.await(), awaitAll(c, d).flatten()))
				}
			}.getOrElse { DeviceState.Error }.let { update(it) }
		}
	}

//	init {
//		viewModelScope.launch(Dispatchers.IO) {
//			runCatching {
//				buildDeviceModels(null).let {
//					val a = viewModelScope.launch(Dispatchers.IO) { deviceDirData = HashMap(it.first.associateBy { i -> i.id }) }
//					val b = viewModelScope.launch(Dispatchers.IO) { deviceMediaData = HashMap(it.second.associateBy { i -> i.id }) }
//					joinAll(a, b)
//					val c = viewModelScope.async(Dispatchers.IO) { prepareDeviceData<DeviceHeader.Dir>(deviceDirData) }
//					val d = viewModelScope.async(Dispatchers.IO) { prepareDeviceData<DeviceHeader.Media>(deviceMediaData) }
//					Pair(c.await(), d.await())
//				}.let { pair ->
//					DeviceState.Ready.Browsing(
//						data = pair.first.plus(pair.second),
//						getDirName = { deviceDirData[it]!!.name },
//						getDirDate = { deviceDirData[it]!!.date },
//						getDirItem = { deviceDirData[it]!!.childCount.toString() },
//						getDirPath = { deviceDirData[it]!!.path },
//						getMediaName = { deviceMediaData[it]!!.name },
//						getMediaDate = { deviceMediaData[it]!!.date },
//						getMediaSize = { deviceMediaData[it]!!.size },
//						getMediaPath = { deviceMediaData[it]!!.path }
//					)
//				}
//			}.getOrElse { DeviceState.Error }.run { update(this) }
//		}
//	}

	fun dispatch(action: Action) {
		viewModelScope.launch(Dispatchers.IO) {
			_mainMutex.withLock {
				val state: DeviceState? = when (action) {
					Action.MediaTileClick -> mediaTileClick()
					Action.TileLongClick -> tileLongClick()
					is Action.DirTileClick -> dirTileClick(
						state = _deviceState.value,
						scope = this,
						getClickedId = { action.id },
						setLoadingState = { update(DeviceState.Loading) },
						deviceModelBuilderBuild = { buildDeviceModels(it) },
						prepareDeviceDirData = { prepareDeviceData<DeviceHeader.Dir>(it) },
						prepareDeviceMediaData = { prepareDeviceData<DeviceHeader.Media>(it) },
						machineScroll = { viewModelScope.launch(Dispatchers.Main) { _deviceListState.value.scrollToItem(0, 0) } }
					)

					is Action.TabClick -> tabClick(
						targetPageIndex = action.targetPage,
						machineScroll = { withContext(Dispatchers.Main) { _mainPagerState.value.scrollToPage(it) } }
					)
				}
				state?.let { update(state) }
			}
		}
	}
}