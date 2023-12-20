package com.canisy.vaultfive.core.viewmodels

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canisy.vaultfive.core.model.Encrypted
import com.canisy.vaultfive.core.model.Plain
import com.canisy.vaultfive.core.util.DeviceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface VaultState {
	data object Loading : VaultState

	sealed interface Error : VaultState {
		data class DataUnavailable(val reason: String) : Error
	}

	sealed interface Ready : VaultState {
		val vaultModels: Map<Long, Encrypted>

		data class Browsing(override val vaultModels: Map<Long, Encrypted>) : Ready
		data class Selecting(val selectedIds: List<Long>, override val vaultModels: Map<Long, Encrypted>) : Ready
	}
}

sealed interface DeviceState {
	data object Loading : DeviceState

	sealed interface Error : DeviceState {
		data class DataUnavailable(val reason: String) : Error
	}

	sealed interface Ready : DeviceState {
		val deviceModels: Map<Long, Plain>

		data class Browsing(override val deviceModels: Map<Long, Plain>) : Ready
		data class Selecting(override val deviceModels: Map<Long, Plain>) : Ready
	}
}

sealed interface VaultAction {
	data object TileLongClick : VaultAction
	data class BackButtonClick(val getParentId: () -> Long)
	sealed interface TileClick : VaultAction {
		data class Device(val path: String) : TileClick
		data class Vault(val id: Long) : TileClick
	}
}

sealed interface DeviceAction {
	data object TileLongClick : DeviceAction
	data class BackButtonClick(val getPath: () -> String): DeviceAction
	sealed interface TileClick : DeviceAction {
		data class Device(val path: String) : TileClick
		data class Vault(val id: Long) : TileClick
	}
}

@HiltViewModel
class MainViewModel @Inject constructor(
	private val deviceUtil: DeviceUtil,
) : ViewModel() {
	private val _deviceState = MutableStateFlow<DeviceState>(DeviceState.Loading)
	private val _vaultState = MutableStateFlow<VaultState>(VaultState.Loading)

	private val deviceMutex = Mutex()
	private val vaultMutex = Mutex()

	val deviceState = _deviceState.asStateFlow()
	val vaultState = _vaultState.asStateFlow()

	init {
		viewModelScope.launch(Dispatchers.IO) {
			runCatching { VaultState.Ready.Browsing(buildVaultModels(null)) }.getOrElse {
				VaultState.Error.DataUnavailable(reason = "Can't access: Vault root directory.")
			}.run { vaultMutex.withLock { withContext(Dispatchers.Main) { _vaultState.update { this@run } } } }
		}
		viewModelScope.launch(Dispatchers.IO) {
			runCatching { DeviceState.Ready.Browsing(buildDeviceModels(null)) }.getOrElse {
				DeviceState.Error.DataUnavailable(reason = "Can't access: Device root directory.")
			}.run { deviceMutex.withLock { withContext(Dispatchers.Main) { _deviceState.update { this@run } } } }
		}
	}

	fun dispatch(vaultAction: VaultAction) {
		viewModelScope.launch(Dispatchers.IO) {
			vaultMutex.withLock {
				this@MainViewModel._vaultState.value.let {
					when (it) {
						VaultState.Loading -> it
						is VaultState.Error.DataUnavailable -> {
							when (vaultAction) {
								VaultAction.TileLongClick -> it
								is VaultAction.TileClick.Device -> it
								is VaultAction.TileClick.Vault -> it
							}
						}

						is VaultState.Ready.Browsing -> {
							when (vaultAction) {
								VaultAction.TileLongClick -> it
								is VaultAction.TileClick.Device -> it
								is VaultAction.TileClick.Vault -> it
							}
						}

						is VaultState.Ready.Selecting -> {
							when (vaultAction) {
								VaultAction.TileLongClick -> it
								is VaultAction.TileClick.Device -> it
								is VaultAction.TileClick.Vault -> it
							}
						}
					}.run { withContext(Dispatchers.Main) { _vaultState.update { this@run } } }
				}
			}
		}
	}

	fun dispatch(deviceAction: DeviceAction) {
		viewModelScope.launch(Dispatchers.IO) {
			vaultMutex.withLock {
				this@MainViewModel._deviceState.value.let {
					when (it) {
						DeviceState.Loading -> null
						is DeviceState.Error.DataUnavailable -> {
							when (deviceAction) {
								DeviceAction.TileLongClick -> null
								is DeviceAction.BackButtonClick -> null
								is DeviceAction.TileClick.Device -> null
								is DeviceAction.TileClick.
								Vault -> null
							}
						}

						is DeviceState.Ready.Browsing -> {
							when (deviceAction) {
								DeviceAction.TileLongClick -> DeviceState.Ready.Selecting(it.deviceModels)
								is DeviceAction.BackButtonClick -> null
								is DeviceAction.TileClick.Device -> {
									withContext(Dispatchers.Main) { _deviceState.update { DeviceState.Loading } }
									kotlin.runCatching { DeviceState.Ready.Browsing(buildDeviceModels(deviceAction.path)) }.getOrElse {
										DeviceState.Error.DataUnavailable(reason = "Can't access: ${deviceAction.path}")
									}
								}
								is DeviceAction.TileClick.Vault -> null
							}
						}

						is DeviceState.Ready.Selecting -> {
							when (deviceAction) {
								DeviceAction.TileLongClick -> null
								is DeviceAction.BackButtonClick -> DeviceState.Ready.Browsing(it.deviceModels)
								is DeviceAction.TileClick.Device -> null
								is DeviceAction.TileClick.Vault -> null
							}
						}
					}?.let { state -> withContext(Dispatchers.Main) { _deviceState.update { state } } }
				}
			}
		}
	}

	fun dispatch(action: Action) {
		viewModelScope.launch(Dispatchers.IO) {
			val result = when (val s = this@MainViewModel._state.value) {
				MainState.Error.DataUnavailable.NoPermission -> s
				MainState.Loading -> s
				is MainState.Ready.Browsing -> {
					when (action) {
						Action.TileLongClick -> MainState.Ready.Selecting(emptyList(), s.deviceModels, s.vaultModels)
						is Action.TileClick.Device -> {
							val cached = this.async { s.copy(deviceModels = buildDeviceModels(action.path)) }
							withContext(Dispatchers.Main) { _state.update { MainState.Loading } }
							cached.await()
						}

						is Action.TileClick.Vault -> s.copy(vaultModels = buildVaultModels(action.id))
					}
				}

				is MainState.Ready.Selecting -> {
					when (action) {
						Action.TileLongClick -> MainState.Ready.Browsing(s.deviceModels, s.vaultModels)
						is Action.TileClick.Device -> TODO()
						is Action.TileClick.Vault -> TODO()
					}
				}
			}
			withContext(Dispatchers.Main) { _state.update { result } }
		}
	}

	private suspend fun buildDeviceModels(path: String?): Map<Long, Plain> {
		return deviceUtil.traverse(path ?: Environment.getExternalStorageDirectory().path).associateBy { it.id }
	}

	private suspend fun buildVaultModels(parentId: Long?): Map<Long, Encrypted> {
		return emptyMap()
	}
}