package com.canisy.vaultfive.core.model.converter

import androidx.room.TypeConverter
import com.canisy.vaultfive.core.model.globaltype.Mime

class MimeConverter {
  @TypeConverter
  fun mimeToString(mime: Mime): String = mime.complete

  @TypeConverter
  fun stringToMime(string: String): Mime {
    return when {
      string.startsWith(Mime.Audio.base) -> Mime.Audio(string)
      string.startsWith(Mime.Image.base) -> Mime.Image(string)
      string.startsWith(Mime.Video.base) -> Mime.Video(string)
      else -> throw IllegalArgumentException("Unknown mimetype: $string")
    }
  }
}