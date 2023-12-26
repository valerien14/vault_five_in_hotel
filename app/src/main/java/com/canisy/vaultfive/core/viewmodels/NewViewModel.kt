package com.canisy.vaultfive.core.viewmodels

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canisy.vaultfive.core.model.Plain
import com.canisy.vaultfive.core.util.DeviceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DeviceCardHeader(val id: Long, val isDir: Boolean)
data class DeviceTraversal(val id: Long, val firstItem: Int, val firstOffset: Int)

sealed interface NewDeviceState {
	data object Error : NewDeviceState
	data object Loading : NewDeviceState
	sealed interface Ready : NewDeviceState {
		val data: List<DeviceCardHeader>
		val traversed: List<Long>
		val getEntity: (Long) -> Plain
		val machineScroll: Pair<Int, Int>?

		data class Browsing(
			override val data: List<DeviceCardHeader>,
			override val traversed: List<Long>,
			override val getEntity: (Long) -> Plain,
			override val machineScroll: Pair<Int, Int>?,
		) : Ready

		data class Selecting(
			override val data: List<DeviceCardHeader>,
			override val traversed: List<Long>,
			override val getEntity: (Long) -> Plain,
			override val machineScroll: Pair<Int, Int>?,
			val selectedId: List<Long>
		) : Ready
	}
}

sealed interface NewDeviceAction {
	data class CardClick(val scrollOffset: Pair<Int, Int>, val id: Long) : NewDeviceAction
	data object CardLongClick : NewDeviceAction
	data object BackButtonPress : NewDeviceAction
}

@HiltViewModel
class NewViewModel @Inject constructor(
	private val deviceUtil: DeviceUtil
) : ViewModel() {
	private var deviceData: HashMap<Long, Plain> = HashMap()
	private var deviceTraversals: List<DeviceTraversal> = emptyList()
	private val deviceMutex = Mutex()
	private val deviceInternalState = MutableStateFlow<NewDeviceState>(NewDeviceState.Loading)
	val deviceState = deviceInternalState.asStateFlow()

	init {
		viewModelScope.launch(Dispatchers.IO) {
			delay(1000)
			runCatching {
				deviceData = HashMap(buildDeviceModels(null))
				NewDeviceState.Ready.Browsing(prepareDeviceData(deviceData), emptyList(), { deviceData[it]!! }, null)
			}.getOrElse { NewDeviceState.Error }.run {
				deviceMutex.withLock { withContext(Dispatchers.Main) { deviceInternalState.update { this@run } } }
			}
		}
	}

	fun dispatch(action: NewDeviceAction) {
		viewModelScope.launch(Dispatchers.IO) {
			deviceMutex.withLock {
				this@NewViewModel.deviceInternalState.value.run {
					when (action) {
						is NewDeviceAction.BackButtonPress -> when (this) {
							NewDeviceState.Error -> null

							NewDeviceState.Loading -> null

							is NewDeviceState.Ready.Browsing -> {
								withContext(Dispatchers.Main) { deviceInternalState.update { NewDeviceState.Loading } }
								runCatching {
									val lastTraversed = deviceTraversals.last()
									deviceData = HashMap(buildDeviceModels(deviceData[lastTraversed.id]!!.path))
									deviceTraversals = deviceTraversals.dropLast(1)
									NewDeviceState.Ready.Browsing(
										data = prepareDeviceData(deviceData),
										traversed = deviceTraversals.map { it.id },
										getEntity = { deviceData[it]!! },
										machineScroll = lastTraversed.firstItem to lastTraversed.firstOffset
									)
								}.getOrElse { NewDeviceState.Error }
							}

							is NewDeviceState.Ready.Selecting -> NewDeviceState.Ready.Browsing(data, traversed, getEntity, null)
						}

						NewDeviceAction.CardLongClick -> when (this) {
							NewDeviceState.Error -> null

							NewDeviceState.Loading -> null

							is NewDeviceState.Ready.Browsing -> NewDeviceState.Ready.Selecting(data, traversed, getEntity, null, emptyList())

							is NewDeviceState.Ready.Selecting -> null
						}

						is NewDeviceAction.CardClick -> when (this) {
							NewDeviceState.Error -> null

							NewDeviceState.Loading -> null

							is NewDeviceState.Ready.Browsing -> {
								withContext(Dispatchers.Main) { deviceInternalState.update { NewDeviceState.Loading } }
								runCatching {
									deviceData = HashMap(buildDeviceModels(deviceData[action.id]!!.path))
									deviceTraversals = deviceTraversals.plus(
										DeviceTraversal(
											id = action.id,
											firstItem = action.scrollOffset.first,
											firstOffset = action.scrollOffset.second
										)
									)
									NewDeviceState.Ready.Browsing(
										data = prepareDeviceData(deviceData),
										traversed = deviceTraversals.map { it.id },
										getEntity = { deviceData[it]!! },
										machineScroll = null
									)
								}.getOrElse { NewDeviceState.Error }
							}

							is NewDeviceState.Ready.Selecting -> {
								copy(selectedId = selectedId.plus(action.id))
							}
						}
					}
				}?.let { state: NewDeviceState -> withContext(Dispatchers.Main) { deviceInternalState.update { state } } }
			}
		}
	}


	private suspend fun buildDeviceState() {}

	private suspend fun buildDeviceModels(path: String?): Map<Long, Plain> {
		return deviceUtil.traverse(path ?: Environment.getExternalStorageDirectory().path).associateBy { it.id }
	}

	private fun prepareDeviceData(hashMap: HashMap<Long, Plain>): List<DeviceCardHeader> {
		return hashMap.map { it.key to it.value }.sortedBy { it.second.name }.map {
			DeviceCardHeader(it.first, it.second is Plain.Directory)
		}.sortedBy { !it.isDir }
	}
}