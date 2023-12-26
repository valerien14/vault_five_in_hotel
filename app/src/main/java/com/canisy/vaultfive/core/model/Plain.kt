package com.canisy.vaultfive.core.model

import com.canisy.vaultfive.core.model.globaltype.Mime
import java.util.UUID

sealed interface Plain {
	val date: String
	val id: Long
	val name: String
	val path: String

	data class Directory(
		val childCount: Int,
		override val date: String,
		override val name: String,
		override val path: String,
		override val id: Long = UUID.randomUUID().mostSignificantBits,
	) : Plain

	data class Media(
		val size: String,
		val mime: Mime,
		override val date: String,
		override val name: String,
		override val path: String,
		override val id: Long,
	) : Plain
}
