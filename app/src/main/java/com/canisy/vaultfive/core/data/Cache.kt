package com.canisy.vaultfive.core.data

import javax.inject.Singleton

@Singleton
class Cache : CacheDao {
  private var container: CacheDao.CacheObject? = null
  override fun clear() = run { container = null }
  override fun get(): CacheDao.CacheObject = container!!
  override fun update(cacheObject: CacheDao.CacheObject) = run { container = cacheObject }
}