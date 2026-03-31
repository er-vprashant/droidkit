package com.prashant.droidkit.core.init

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.prashant.droidkit.DroidKit
import com.prashant.droidkit.DroidKitConfig

internal class DroidKitInitializer : ContentProvider() {

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        val config = DroidKitConfig.readFromManifest(ctx)
        if (config.autoInit) {
            DroidKit.initInternal(ctx, config)
        }
        return true
    }

    override fun query(uri: Uri, proj: Array<out String>?, sel: String?, selArgs: Array<out String>?, sort: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, sel: String?, selArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, sel: String?, selArgs: Array<out String>?): Int = 0
}
