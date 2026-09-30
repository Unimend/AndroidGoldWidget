package com.unimend.goldwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机自启：手机重启后自动启动刷新服务（无需手动打开应用）。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            GoldRefreshService.ensureRunning(context)
        }
    }
}
