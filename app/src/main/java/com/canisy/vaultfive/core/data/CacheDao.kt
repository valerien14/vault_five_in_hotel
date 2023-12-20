package com.canisy.vaultfive.core.data

import com.canisy.vaultfive.core.model.Persistable
import com.canisy.vaultfive.core.model.globaltype.Page

interface CacheDao {
  data class CacheObject(val index: Int, val persistables: List<Persistable>, val sender: Page)
  fun clear()
  fun get(): CacheObject
  fun update(cacheObject: CacheObject)
}