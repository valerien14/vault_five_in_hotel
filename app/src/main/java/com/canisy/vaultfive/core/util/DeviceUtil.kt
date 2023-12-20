package com.canisy.vaultfive.core.util

import android.content.ContentResolver
import android.provider.MediaStore
import com.canisy.vaultfive.core.model.Plain
import com.canisy.vaultfive.core.model.globaltype.Mime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.stream.Collectors
import javax.inject.Inject
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.pathString
import kotlin.math.log10
import kotlin.math.pow

class DeviceUtil @Inject constructor(private val ctr: ContentResolver) {
	private data class UnfilteredMedia(val id: Long, val path: String, val name: String, val date: String, val mime: String)

	private fun countMedia(path: String): Int {
		val store = MediaStore.Files.getContentUri(EXTERNAL)
		val col = arrayOf(MediaStore.MediaColumns.DATA)
		val filter = "${col[0]} LIKE ? AND ${col[0]} NOT LIKE ?"
		val args = arrayOf("${path}/%", "${path}/%/%")
		var count = 0
		ctr.query(store, col, filter, args, null)?.use {
			it.moveToFirst()
			while (it.moveToNext()) {
				count++
			}
		}
		return count
	}

	private fun createPlainMedia(unfilteredMedia: UnfilteredMedia, mediaType: Mime): Plain.Media {
		return Plain.Media(
			size = formatSize(File(unfilteredMedia.path).length()),
			mime = mediaType,
			date = formatTimestamp(unfilteredMedia.date.toLong(), false),
			name = unfilteredMedia.name,
			path = unfilteredMedia.path,
			id = unfilteredMedia.id
		)
	}

	private fun formatSize(size: Long): String {
		if (size <= 0) return ZERO
		val digitGroups = (log10(size.toDouble()).div(log10(BYTE)).toInt())
		return String.format(
			SIZE_FORMAT, size.div(BYTE.pow(digitGroups.toDouble())), SIZE_UNITS[digitGroups]
		)
	}

	private fun formatTimestamp(timestamp: Long, isDir: Boolean): String {
		return SimpleDateFormat(TIMESTAMP_FORMAT, Locale.getDefault()).format(
			Date(if (isDir) timestamp else timestamp.times(1000))
		)
	}

	suspend fun traverse(path: String): List<Plain> = withContext(Dispatchers.IO) {
		val plainMedias = this.async {
			val store = MediaStore.Files.getContentUri(EXTERNAL)
			val col = arrayOf(
				MediaStore.MediaColumns._ID,
				MediaStore.MediaColumns.DATA,
				MediaStore.MediaColumns.DISPLAY_NAME,
				MediaStore.MediaColumns.DATE_ADDED,
				MediaStore.MediaColumns.MIME_TYPE
			)
			val filter = "${col[1]} LIKE ? AND ${col[1]} NOT LIKE ?"
			val args = arrayOf("${path}/%", "${path}/%/%")
			val unfilteredMediaFlow = flow {
				ctr.query(store, col, filter, args, null)?.use {
					it.moveToFirst()
					while (it.moveToNext()) {
						emit(UnfilteredMedia(it.getLong(0), it.getString(1), it.getString(2), it.getString(3), it.getString(4)))
					}
				}
			}.buffer()
			val audios = unfilteredMediaFlow.filter { it.mime.startsWith(Mime.Audio.base) }.map {
				createPlainMedia(it, Mime.Audio(complete = it.mime))
			}.toList()
			val images = unfilteredMediaFlow.filter { it.mime.startsWith(Mime.Image.base) }.map {
				createPlainMedia(it, Mime.Image(complete = it.mime))
			}.toList()
			val videos = unfilteredMediaFlow.filter { it.mime.startsWith(Mime.Video.base) }.map {
				createPlainMedia(it, Mime.Video(complete = it.mime))
			}.toList()
			audios.plus(images).plus(videos)
		}
		Files.list(Paths.get(path)).use { stream ->
			stream.filter { Files.isDirectory(it) }.collect(Collectors.toList())
		}.map {
			this.async {
				Plain.Directory(
					childCount = countMedia(it.pathString),
					date = formatTimestamp(it.getLastModifiedTime().toMillis(), true),
					name = it.fileName.toString(),
					path = it.pathString
				)
			}
		}.awaitAll().plus(plainMedias.await())
	}

	private companion object {
		private const val BYTE = 1024.0
		private const val EXTERNAL = "external"
		private const val TIMESTAMP_FORMAT = "M/d/yy h:mm a"
		private const val SIZE_FORMAT = "%.2f %s"
		private const val ZERO = "0B"
		private val SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB")
	}
}