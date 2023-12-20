package com.canisy.vaultfive.core.model.globaltype

sealed interface ViewMode {
  data object GridMode: ViewMode
  data object ListMode: ViewMode
}