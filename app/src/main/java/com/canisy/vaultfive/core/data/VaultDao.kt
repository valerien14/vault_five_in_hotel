package com.canisy.vaultfive.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.canisy.vaultfive.core.model.Persistable

@Dao
interface VaultDao {
	@Query("SELECT COUNT(*) FROM Persistable WHERE parentId = :id AND isDir = 0")
	suspend fun countChildren(id: Int): Int

	@Query("SELECT * FROM Persistable WHERE parentId IS NULL")
	fun getRootPersistable(): List<Persistable>

	@Query("SELECT * FROM Persistable WHERE parentId = :id")
	fun getPersistable(id: Int): List<Persistable>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertPersistable(persistable: Persistable)
}