package com.smsalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val config = Config(context)
        if (!config.enabled) return
        val keyword = config.keyword
        val phone = config.phone
        if (keyword.isEmpty() || phone.isEmpty()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // 长短信会被拆成多段送达，按发件人拼回完整内容再匹配
        val bodiesBySender = messages
            .groupBy { it.originatingAddress.orEmpty() }
            .mapValues { (_, parts) -> parts.joinToString("") { it.messageBody.orEmpty() } }

        val sender = bodiesBySender.entries.firstOrNull { (sender, body) ->
            body.contains(keyword, ignoreCase = true) && senderMatches(sender, config.senderFilter)
        }?.key ?: return

        // 防止同一件事连发几条短信导致连环拨号
        val now = System.currentTimeMillis()
        if (now - config.lastTriggerAt < COOLDOWN_MS) {
            config.appendLog("冷却中，忽略来自 $sender 的短信")
            return
        }
        config.lastTriggerAt = now

        try {
            Caller.call(context, phone)
            config.appendLog("收到 $sender 的短信，已拨打 $phone")
        } catch (e: Exception) {
            config.appendLog("收到 $sender 的短信，拨号失败：${e.message}")
        }
    }

    /** 比较号码时只看数字并按尾号匹配，这样 +8613800000000 和 13800000000 算同一个号。 */
    private fun senderMatches(sender: String, filter: String): Boolean {
        if (filter.isBlank()) return true
        val filterDigits = filter.filter(Char::isDigit)
        val senderDigits = sender.filter(Char::isDigit)
        if (filterDigits.isEmpty() || senderDigits.isEmpty()) {
            return sender.contains(filter.trim(), ignoreCase = true)
        }
        return senderDigits.endsWith(filterDigits) || filterDigits.endsWith(senderDigits)
    }

    private companion object {
        const val COOLDOWN_MS = 60_000L
    }
}
