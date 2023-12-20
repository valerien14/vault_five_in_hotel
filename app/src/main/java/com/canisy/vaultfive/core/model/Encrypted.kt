package com.canisy.vaultfive.core.model

import com.canisy.vaultfive.core.model.globaltype.Mime

sealed interface Encrypted {
	val date: String
	val id: Long
	val name: String
	val parentId: Long?
	val cipher: Cipher

	data class Directory(
		val childCount: Int,
		override val cipher: Cipher,
		override val date: String,
		override val name: String,
		override val id: Long,
		override val parentId: Long? = null,
	) : Encrypted

	data class Media(
		val size: String,
		val mime: Mime,
		override val cipher: Cipher,
		override val date: String,
		override val name: String,
		override val id: Long,
		override val parentId: Long? = null,
	) : Encrypted
}