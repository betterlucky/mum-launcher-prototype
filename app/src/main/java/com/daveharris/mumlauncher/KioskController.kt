package com.daveharris.mumlauncher

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.UserManager

data class KioskState(
    val isDeviceAdminEnabled: Boolean,
    val isDeviceOwner: Boolean,
    val isLockTaskPermitted: Boolean,
    val dialerPackage: String?,
    val smsPackage: String?,
    val allowlistedPackages: List<String>,
)

object KioskController {
    fun getState(context: Context): KioskState {
        val policyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = adminComponent(context)
        val dialerPackage = resolveDialerPackage(context)
        val smsPackage = resolveSmsPackage(context)
        val allowlistedPackages = listOfNotNull(context.packageName, dialerPackage, smsPackage).distinct()
        return KioskState(
            isDeviceAdminEnabled = policyManager.isAdminActive(admin),
            isDeviceOwner = policyManager.isDeviceOwnerApp(context.packageName),
            isLockTaskPermitted = policyManager.isLockTaskPermitted(context.packageName),
            dialerPackage = dialerPackage,
            smsPackage = smsPackage,
            allowlistedPackages = allowlistedPackages,
        )
    }

    fun syncKioskPolicy(context: Context, enabled: Boolean): KioskState {
        val policyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = adminComponent(context)
        val state = getState(context)
        if (!state.isDeviceOwner) return state

        if (enabled) {
            policyManager.setLockTaskPackages(admin, state.allowlistedPackages.toTypedArray())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                policyManager.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                runCatching { policyManager.setStatusBarDisabled(admin, true) }
            }
            runCatching { policyManager.addUserRestriction(admin, UserManager.DISALLOW_CREATE_WINDOWS) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { policyManager.addUserRestriction(admin, UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS) }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                runCatching { policyManager.setStatusBarDisabled(admin, false) }
            }
            runCatching { policyManager.clearUserRestriction(admin, UserManager.DISALLOW_CREATE_WINDOWS) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { policyManager.clearUserRestriction(admin, UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS) }
            }
            policyManager.setLockTaskPackages(admin, emptyArray())
        }

        return getState(context)
    }

    private fun adminComponent(context: Context): ComponentName =
        ComponentName(context, MumDeviceAdminReceiver::class.java)

    private fun resolveDialerPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0123456789"))
        return context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
    }

    private fun resolveSmsPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:0123456789"))
        return context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
    }
}
