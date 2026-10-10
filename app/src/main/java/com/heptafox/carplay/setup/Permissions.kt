package com.heptafox.carplay.setup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/** The two one-time grants Hepta needs; both are given from the phone ([MainActivity]). */
object Permissions {

    fun hasMic(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** "Display over other apps" exempts us from Android's block on starting apps from the background. */
    fun canLaunchInBackground(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun allGranted(context: Context): Boolean = hasMic(context) && canLaunchInBackground(context)
}
