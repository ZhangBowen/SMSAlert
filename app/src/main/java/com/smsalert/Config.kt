package com.smsalert

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 配置和运行记录，存在 SharedPreferences 里，Activity 和 Receiver 共用。 */
class Config(context: Context) {
    private val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** 短信里包含这个字符串就触发 */
    var keyword: String
        get() = prefs.getString(KEY_KEYWORD, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_KEYWORD, value).apply()

    /** 只处理这个号码发来的短信；空 = 任何人 */
    var senderFilter: String
        get() = prefs.getString(KEY_SENDER, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SENDER, value).apply()

    /** 要拨打的号码 */
    var phone: String
        get() = prefs.getString(KEY_PHONE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_PHONE, value).apply()

    var lastTriggerAt: Long
        get() = prefs.getLong(KEY_LAST_TRIGGER, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_TRIGGER, value).apply()

    val log: String
        get() = prefs.getString(KEY_LOG, "").orEmpty()

    fun appendLog(message: String) {
        val time = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val lines = (listOf("$time  $message") + log.lines().filter { it.isNotBlank() })
            .take(MAX_LOG_LINES)
        prefs.edit().putString(KEY_LOG, lines.joinToString("\n")).apply()
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_KEYWORD = "keyword"
        const val KEY_SENDER = "sender"
        const val KEY_PHONE = "phone"
        const val KEY_LAST_TRIGGER = "last_trigger"
        const val KEY_LOG = "log"
        const val MAX_LOG_LINES = 30
    }
}
