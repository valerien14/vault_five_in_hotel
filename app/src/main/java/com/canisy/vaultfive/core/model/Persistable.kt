package com.canisy.vaultfive.core.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.canisy.vaultfive.core.model.globaltype.Mime

@Entity(
	indices = [Index("parentId")],
	inheritSuperIndices = false,
	foreignKeys = [ForeignKey(Persistable::class, ["id"], ["parentId"], ForeignKey.CASCADE)]
)
data class Persistable(
	@PrimaryKey(true) val id: Int,
	val date: String,
	val isDir: Boolean,
	val mime: Mime,
	val name: String,
	val path: String,
	val parentId: Int?,
	val size: String? = null,
)