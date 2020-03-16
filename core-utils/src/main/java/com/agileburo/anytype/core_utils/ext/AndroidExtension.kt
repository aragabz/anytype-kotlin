package com.agileburo.anytype.core_utils.ext

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.Annotation
import android.text.Editable
import android.text.Spanned
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.TouchDelegate
import android.view.View
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

fun Context.dimen(res: Int): Float {
    return resources
        .getDimension(res)
}

fun Uri.parsePath(context: Context): String {

    val result: String?

    val cursor = context.contentResolver.query(
        this,
        null,
        null,
        null, null
    )

    if (cursor == null) {
        result = this.path
    } else {
        cursor.moveToFirst()
        val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        result = cursor.getString(idx)
        cursor.close()
    }

    return result ?: throw IllegalStateException("Cold not get real path")
}

fun Throwable.timber() = Timber.e("Get error : ${this.message}")

const val DATE_FORMAT_MMMdYYYY = "MMM d, yyyy"
const val KEY_ROUNDED = "key"
const val VALUE_ROUNDED = "rounded"
const val MIME_VIDEO_ALL = "video/*"
const val MIME_IMAGE_ALL = "image/*"

fun Long.formatToDateString(pattern: String, locale: Locale): String {
    val formatter = SimpleDateFormat(pattern, locale)
    return formatter.format(Date(this))
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.INVISIBLE
}

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Context.dp(value: Float) =
    TypedValue.applyDimension(COMPLEX_UNIT_DIP, value, resources.displayMetrics)

val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

private fun expandViewHitArea(parent: View, child: View) {
    parent.post {
        val parentRect = Rect()
        val childRect = Rect()
        parent.getHitRect(parentRect)
        child.getHitRect(childRect)
        childRect.left = 0
        childRect.top = 0
        childRect.right = parentRect.width()
        childRect.bottom = parentRect.height()
        parent.touchDelegate = TouchDelegate(childRect, child)
    }
}

fun <T> hasSpan(spanned: Spanned, clazz: Class<T>): Boolean {
    val limit = spanned.length
    return spanned.nextSpanTransition(0, limit, clazz) < limit
}

inline fun <reified T> Editable.removeSpans() {
    val allSpans = getSpans(0, length, T::class.java)
    for (span in allSpans) {
        removeSpan(span)
    }
}

fun Editable.removeRoundedSpans(): Editable {
    this.getSpans(0, length, Annotation::class.java).forEach { span ->
        if (span.key == KEY_ROUNDED && span.value == VALUE_ROUNDED) removeSpan(span)
    }
    return this
}

fun getVideoFileIntent(mediaType: String): Intent {
    val intent =
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        } else {
            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.INTERNAL_CONTENT_URI)
        }
    return intent.apply {
        type = mediaType
        action = Intent.ACTION_GET_CONTENT
        putExtra("return-data", true)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}