//package com.canisy.vaultfive.core.viewmodels
//
//import android.os.Environment
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.canisy.vaultfive.core.model.Plain
//import com.canisy.vaultfive.core.util.DeviceUtil
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import kotlinx.coroutines.withContext
//import javax.inject.Inject
//
//data class DeviceHeader(val id: Long, val isDir: Boolean)
//data class DeviceTraversal(val id: Long, val firstItem: Int, val firstOffset: Int)
//
//sealed interface DeviceState {
//	data object Error : DeviceState
//	data object Loading : DeviceState
//	@JvmInline
//	value class Ready(val data: List<DeviceHeader>) : DeviceState
//}
//
//sealed interface DeviceAction {
//	data class RefreshState(val id: Long): DeviceAction
//}
//
//@HiltViewModel
//class MainViewModel @Inject constructor(
//	private val deviceUtil: DeviceUtil
//) : ViewModel() {
//	private var deviceData: HashMap<Long, Plain> = HashMap()
//	private val deviceMutex = Mutex()
//	private val deviceInternalState = MutableStateFlow<DeviceState>(DeviceState.Loading)
//	val deviceState = deviceInternalState.asStateFlow()
//
//	init {
//		viewModelScope.launch(Dispatchers.IO) {
//			runCatching {
//				deviceData = HashMap(buildDeviceModels(null))
//				DeviceState.Ready(prepareDeviceData(deviceData))
//			}.getOrElse { DeviceState.Error }.run {
//				deviceMutex.withLock { withContext(Dispatchers.Main) { deviceInternalState.update { this@run } } }
//			}
//		}
//	}
//
//	fun fetchData(headerId: Long) = deviceData[headerId]
//
//	fun refreshState(path: String? = null) {
//		viewModelScope.launch(Dispatchers.IO) {
//			withContext(Dispatchers.Main) { deviceInternalState.update { DeviceState.Loading } }
//			runCatching {
//				deviceData = HashMap(buildDeviceModels(path))
//				DeviceState.Ready(prepareDeviceData(deviceData))
//			}.getOrElse { DeviceState.Error }.run {
//				deviceMutex.withLock { withContext(Dispatchers.Main) { deviceInternalState.update { this@run } } }
//			}
//		}
//	}
//
//	private suspend fun buildDeviceModels(path: String?): Map<Long, Plain> {
//		return deviceUtil.traverse(path ?: Environment.getExternalStorageDirectory().path).associateBy { it.id }
//	}
//
//	private fun prepareDeviceData(hashMap: HashMap<Long, Plain>): List<DeviceHeader> {
//		return hashMap.map { it.key to it.value }.sortedBy { it.second.name }.map {
//			DeviceHeader(it.first, it.second is Plain.Directory)
//		}.sortedBy { !it.isDir }
//	}
//}