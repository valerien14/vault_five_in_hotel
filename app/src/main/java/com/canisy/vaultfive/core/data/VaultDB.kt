package com.canisy.vaultfive.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.canisy.vaultfive.core.model.Persistable
import com.canisy.vaultfive.core.model.converter.MimeConverter

@Database([Persistable::class], version = 1, exportSchema = false)
@TypeConverters(MimeConverter::class)
abstract class VaultDB : RoomDatabase() {
  abstract val dao: VaultDao
}