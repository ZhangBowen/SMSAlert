package com.smsalert

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager

object Caller {

    /**
     * 直接拨出电话（不经过拨号盘确认）。
     *
     * 优先用 TelecomManager.placeCall：它不是启动 Activity，不受 Android 10+
     * “禁止后台启动界面”的限制，在短信广播里调用也能拨出。
     * 个别 ROM 上失败时退回 ACTION_CALL，这条路在后台需要悬浮窗权限。
     */
    @SuppressLint("MissingPermission")
    fun call(context: Context, number: String) {
        if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("没有拨号权限 CALL_PHONE")
        }
        val uri = Uri.fromParts("tel", number, null)
        try {
            context.getSystemService(TelecomManager::class.java).placeCall(uri, Bundle())
        } catch (e: Exception) {
            context.startActivity(
                Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
