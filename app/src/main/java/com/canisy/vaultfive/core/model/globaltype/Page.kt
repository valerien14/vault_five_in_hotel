package com.canisy.vaultfive.core.model.globaltype

sealed interface Page {
  data object Vault : Page
  data object Device : Page
}