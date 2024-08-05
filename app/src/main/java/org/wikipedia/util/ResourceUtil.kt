package org.wikipedia.util

import android.content.ContentResolver
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources.NotFoundException
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.TypedValue
import androidx.annotation.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap

object ResourceUtil {
    fun bitmapFromVectorDrawable(context: Context, @DrawableRes id: Int, @ColorRes tintColor: Int? = null): Bitmap {
        val vectorDrawable = AppCompatResources.getDrawable(context, id)!!.mutate()
        if (tintColor != null) {
            vectorDrawable.setTint(ContextCompat.getColor(context, tintColor))
        }
        return vectorDrawable.toBitmap()
    }

    private fun getThemedAttribute(context: Context, @AttrRes id: Int): TypedValue? {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(id, typedValue, true)) {
            typedValue
        } else null
    }

    @AnyRes
    fun getThemedAttributeId(context: Context, @AttrRes id: Int): Int {
        val typedValue = getThemedAttribute(context, id)
                ?: throw IllegalArgumentException("Attribute not found; ID=$id")
        return typedValue.resourceId
    }

    @ColorInt
    fun getThemedColor(context: Context, @AttrRes id: Int): Int {
        val typedValue = getThemedAttribute(context, id)
                ?: throw IllegalArgumentException("Attribute not found; ID=$id")
        return typedValue.data
    }

    fun getThemedColorStateList(context: Context, @AttrRes id: Int): ColorStateList {
        return ColorStateList.valueOf(getThemedColor(context, id))
    }

    @Throws(NotFoundException::class)
    fun uri(context: Context, @AnyRes id: Int): Uri {
        val res = context.resources
        return Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(res.getResourcePackageName(id))
                .appendPath(res.getResourceTypeName(id))
                .appendPath(res.getResourceEntryName(id))
                .build()
    }

    fun colorToCssString(@ColorInt color: Int): String {
        return String.format("%08x", (color shl 8) or ((color shr 24) and 0xff))
    }

    @ColorInt
    fun lightenColor(@ColorInt color: Int): Int {
        return ColorUtils.blendARGB(color, Color.WHITE, 0.3f)
    }

    @ColorInt
    fun darkenColor(@ColorInt color: Int): Int {
        return ColorUtils.blendARGB(color, Color.BLACK, 0.3f)
    }
}
