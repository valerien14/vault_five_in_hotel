package com.canisy.vaultfive.core.model

import androidx.security.crypto.EncryptedFile

data class Cipher (
  val id: Long,
  val encryptedFileBuilder: EncryptedFile.Builder
)