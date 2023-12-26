package com.canisy.vaultfive.core.model.globaltype

sealed interface Mime {
	val complete: String

	data class Audio(override val complete: String) : Mime {
		companion object {
			const val base: String = "audio/"
		}
	}

	data class Image(override val complete: String) : Mime {
		companion object {
			const val base: String = "image/"
		}
	}

	data class Video(override val complete: String) : Mime {
		companion object {
			const val base: String = "video/"
		}
	}

	data class Dir(override val complete: String = "NULL"): Mime

	data class Other(override val complete: String = "NULL") : Mime
}