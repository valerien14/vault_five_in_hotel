package com.canisy.vaultfive.core.util

import android.content.ContentResolver
import android.provider.MediaStore
import android.util.Log
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
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.stream.Collectors
import javax.inject.Inject
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.math.log10
import kotlin.math.pow


sealed interface Kind {
	data object Dir : Kind
	data object Audio : Kind
	data object Image : Kind
	data object Video : Kind
	data object Other : Kind
}

class DeviceUtil @Inject constructor(private val ctr: ContentResolver) {

	//TODO: USE NIO TO TRAVERSE
	private data class UnfilteredMedia(val id: Long, val path: String, val name: String, val date: String, val mime: String)

	private fun countMedia(path: String): Int {
		val store = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
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
		return String.format(SIZE_FORMAT, size.div(BYTE.pow(digitGroups.toDouble())), SIZE_UNITS[digitGroups])
	}

	private fun formatTimestamp(timestamp: Long, isDir: Boolean): String {
		return SimpleDateFormat(TIMESTAMP_FORMAT, Locale.getDefault()).format(
			Date(if (isDir) timestamp else timestamp.times(1000))
		)
	}

//	private fun count(path: Path): Int {
//		val b = { i: String -> i.startsWith(Mime.Audio.base) || i.startsWith(Mime.Image.base) || i.startsWith(Mime.Video.base) }
//		val detect = { i: Path -> tika.detect(i).run { b(this) } }
//		return Files.find(path, 1, { i, _ -> detect(i) }).use { stream -> stream.count() }.toInt()
//	}

//	fun getMimeTypesForFiles(ctr: ContentResolver, files: List<File>): Map<File, String> {
//		val fileToMimeTypeMap = mutableMapOf<File, String>()
//
//		// Construct the selection criteria for MediaStore query
//		val selection = "${MediaStore.Files.FileColumns.DATA} IN (${files.joinToString(",") { "'${it.absolutePath}'" }})"
//
//		// Define the columns to retrieve from the query
//		val projection = arrayOf(MediaStore.Files.FileColumns.MIME_TYPE)
//
//		// Perform the query using MediaStore
//		ctr.query(
//			MediaStore.Files.getContentUri("external"), // or other appropriate URI
//			projection,
//			selection,
//			null,
//			null
//		)?.use { cursor ->
//			// Iterate through the cursor results to populate the map
//			while (cursor.moveToNext()) {
//				val filePath = File(cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)))
//				val mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE))
//				fileToMimeTypeMap[filePath] = mimeType
//			}
//		}
//
//		return fileToMimeTypeMap
//	}

	suspend fun traverse(path: String) = withContext(Dispatchers.IO) {
		val startTimer = System.currentTimeMillis()
		val partition = Files.list(Paths.get(path)).use { s -> s.collect(Collectors.partitioningBy { it.isDirectory() }) }
		val dirs = this.async {
			run { partition[true]?.toList() ?: emptyList() }.map {
				async(Dispatchers.IO) {
					Plain.Directory(
						date = formatTimestamp(it.getLastModifiedTime().toMillis(), false),
						name = it.name,
						path = it.pathString,
						id = UUID.randomUUID().mostSignificantBits,
						childCount = Files.list(it).filter { file ->
							Files.probeContentType(file)?.let { mime ->
								mime.startsWith(Mime.Audio.base) || mime.startsWith(Mime.Image.base) || mime.startsWith(Mime.Video.base)
							} ?: false
						}.count().toInt()
					)
				}
			}.awaitAll()
		}
		val medias = this.async {
			run { partition[false]?.toList() ?: emptyList() }.map {
				async(Dispatchers.IO) {
					val probe = Files.probeContentType(it)
					when {
						probe.startsWith(Mime.Audio.base) -> Mime.Audio(probe)
						probe.startsWith(Mime.Image.base) -> Mime.Image(probe)
						probe.startsWith(Mime.Video.base) -> Mime.Video(probe)
						else -> null
					}?.let { mime ->
						Plain.Media(
							size = formatSize(it.fileSize()),
							mime = mime,
							date = formatTimestamp(it.getLastModifiedTime().toMillis(), false),
							name = it.name,
							path = it.pathString,
							id = UUID.randomUUID().mostSignificantBits
						)
					}
				}
			}.awaitAll().requireNoNulls()
		}
		awaitAll(dirs, medias).flatten()
	}

	private suspend fun traverseTwo(path: String) = withContext(Dispatchers.IO) {
		val test = Files.list(Paths.get(path)).use { s ->
			s.collect(Collectors.partitioningBy { it.isDirectory() })
		}.toMap()
		val directories = test[true]?.toList() ?: emptyList()
		val files: List<Path> = test[false]?.toList() ?: emptyList()
		val dirs = this.async {
			directories.map {
				async {
					Plain.Directory(
						childCount = 0,
						date = formatTimestamp(it.getLastModifiedTime().toMillis(), false),
						name = it.name,
						path = it.pathString,
						id = 0,
					)
				}
			}.awaitAll()
		}
		val medias = this.async {
			files.map {
				async(Dispatchers.IO) { it to Files.probeContentType(it) }
			}.awaitAll().toMap().also { a -> Log.d("TAG", "traverseTwo: ${a.mapKeys { i -> i.key.name }}") }
		}
//		val medias = this.async {
//			//query the mime
//			val mimes = flow<Pair<Path, String>> {
//				val absolutePaths = files.joinToString(",") { "'${it.absolutePathString()}'" }
//				ctr.query(
////					MediaStore.Files.getContentUri("external"),
//					MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//					arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.MIME_TYPE),
//					"${MediaStore.Files.FileColumns.DATA} IN ($absolutePaths)",
//					null,
//					null
//				)?.use {
//					it.moveToFirst()
//					while (it.moveToNext()) {
//						emit(Paths.get(it.getString(0)) to it.getString(1))
//					}
//				}
//			}.buffer().flowOn(Dispatchers.Main).toList().toMap()
//			files.map {
//				async {
//					Mime.Other().let { mime ->
//						Plain.Media(
//							size = formatSize(it.fileSize()),
//							mime = mime,
//							date = formatTimestamp(it.getLastModifiedTime().toMillis(), false),
//							name = it.name,
//							path = it.pathString,
//							id = 0
//						)
//					}
//				}
//			}.awaitAll().requireNoNulls()
//			Log.d("TAG", "traverseTwo: ${mimes.keys.joinToString { it.name }}")
//		}
		val result = dirs.await().plus(medias.await())
		Log.d("TAG", "traverseTwo result done")
		return@withContext
	}

//	private suspend fun traverseTwo(path: String) = withContext(Dispatchers.IO) {
//		Log.d("TAG", "traverseTwo:  starting")
//		Files.list(Paths.get(path)).use { s -> s.parallel().collect(Collectors.partitioningBy { Files.isDirectory(it) }) }.let {
//			val dirs = this.async {
//				Log.d("TAG", "traverseTwo: listing dirs")
//					it[true]?.toList()?.map {
//					Plain.Directory(
//						childCount = count(it),
//						date = formatTimestamp(it.getLastModifiedTime().toMillis(), false),
//						name = it.name,
//						path = it.pathString,
//						id = UUID.randomUUID().mostSignificantBits,
//					)
//				}.also {
//					Log.d("TAG", "traverseTwo: finished dir: ${it?.joinToString { i -> i.name }}")
//				} ?: emptyList<Plain.Directory>().also { Log.d("TAG", "traverseTwo: dir is empty...") }
//			}
//			val medias = this.async {
//				it[false]?.toList()?.map {
//					tika.detect(it).run {
//						when {
//							startsWith(Mime.Audio.base) -> Mime.Audio(this)
//							startsWith(Mime.Image.base) -> Mime.Image(this)
//							startsWith(Mime.Video.base) -> Mime.Video(this)
//							else -> null
//						}
//					}?.let { mime ->
//						Plain.Media(
//							size = formatSize(it.fileSize()),
//							mime = mime,
//							date = formatTimestamp(it.getLastModifiedTime().toMillis(), false),
//							name = it.name,
//							path = it.pathString,
//							id = UUID.randomUUID().mostSignificantBits
//						)
//					}
//				}?.requireNoNulls() ?: emptyList()
//			}
//			return@withContext dirs.await().plus(medias.await()).also { Log.d("TAG", "traverseTwo: ${it.joinToString { i -> i.name }}") }
//		}
//	}

	suspend fun traverseOld(path: String): List<Plain> = withContext(Dispatchers.IO) {
		val startTimer = System.currentTimeMillis()
//			traverseThree(path)

//		this.launch { Log.d("TAG", "listDir: ${File(path).listFiles()?.joinToString { it.name }}") }
		val plainMedias = this.async {
//			val store = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
			val store = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
//			val store = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

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
						emit(
							UnfilteredMedia(
								it.getLong(0),
								it.getString(1),
								it.getString(2),
								it.getString(3),
								it.getString(4)
							)
						)
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
		}.awaitAll().plus(plainMedias.await()).also {
			Log.d("TAG", "traverse: ${System.currentTimeMillis().minus(startTimer)}")
		}
	}

	private companion object {
		private const val BYTE = 1024.0
		private const val TIMESTAMP_FORMAT = "M/d/yy h:mm a"
		private const val SIZE_FORMAT = "%.2f %s"
		private const val ZERO = "0B"
		private val SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB")
	}
}