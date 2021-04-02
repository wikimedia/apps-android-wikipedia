package org.wikipedia.util

import android.content.ContentResolver
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources.NotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.util.TypedValue
import android.view.MenuItem
import androidx.annotation.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.MenuItemCompat
import kotlin.jvm.Throws

object ResourceUtil {
    @JvmStatic
    fun bitmapFromVectorDrawable(context: Context, @DrawableRes id: Int, @ColorRes tintColor: Int?): Bitmap {
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

    @JvmStatic
    @AnyRes
    fun getThemedAttributeId(context: Context, @AttrRes id: Int): Int {
        val typedValue = getThemedAttribute(context, id)
                ?: throw IllegalArgumentException("Attribute not found; ID=$id")
        return typedValue.resourceId
    }

    @JvmStatic
    @ColorInt
    fun getThemedColor(context: Context, @AttrRes id: Int): Int {
        val typedValue = getThemedAttribute(context, id)
                ?: throw IllegalArgumentException("Attribute not found; ID=$id")
        return typedValue.data
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

    fun setMenuItemTint(context: Context, item: MenuItem, @AttrRes colorAttr: Int) {
        MenuItemCompat.setIconTintList(item, ColorStateList.valueOf(getThemedColor(context, colorAttr)))
    }
}
