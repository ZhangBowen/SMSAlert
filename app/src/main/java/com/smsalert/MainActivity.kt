package com.smsalert

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var config: Config
    private lateinit var enabledSwitch: Switch
    private lateinit var keywordInput: EditText
    private lateinit var senderInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var statusText: TextView
    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        config = Config(this)

        enabledSwitch = findViewById(R.id.enabled)
        keywordInput = findViewById(R.id.keyword)
        senderInput = findViewById(R.id.sender)
        phoneInput = findViewById(R.id.phone)
        statusText = findViewById(R.id.status)
        logText = findViewById(R.id.log)

        enabledSwitch.isChecked = config.enabled
        keywordInput.setText(config.keyword)
        senderInput.setText(config.senderFilter)
        phoneInput.setText(config.phone)

        findViewById<Button>(R.id.save).setOnClickListener { save() }
        findViewById<Button>(R.id.testCall).setOnClickListener { testCall() }
        findViewById<Button>(R.id.grantPermissions).setOnClickListener {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS)
        }
        findViewById<Button>(R.id.overlay).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri()))
        }
        findViewById<Button>(R.id.battery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()))
        }
        findViewById<Button>(R.id.appSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshStatus()
    }

    private fun save() {
        val keyword = keywordInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        if (enabledSwitch.isChecked && (keyword.isEmpty() || phone.isEmpty())) {
            toast("启用前请填写关键字和要拨打的号码")
            return
        }
        config.keyword = keyword
        config.senderFilter = senderInput.text.toString().trim()
        config.phone = phone
        config.enabled = enabledSwitch.isChecked
        toast("已保存")

        val missing = missingPermissions()
        if (config.enabled && missing.isNotEmpty()) {
            requestPermissions(missing, REQUEST_PERMISSIONS)
        }
        refreshStatus()
    }

    private fun testCall() {
        val phone = phoneInput.text.toString().trim()
        if (phone.isEmpty()) {
            toast("请先填写要拨打的号码")
            return
        }
        try {
            Caller.call(this, phone)
        } catch (e: SecurityException) {
            toast("缺少拨号权限")
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS)
        }
    }

    private fun refreshStatus() {
        val power = getSystemService(PowerManager::class.java)
        statusText.text = listOf(
            "短信和拨号权限：" + if (missingPermissions().isEmpty()) "✅ 已授予" else "❌ 未授予",
            "显示在其他应用上层：" + if (Settings.canDrawOverlays(this)) "✅" else "❌（部分手机后台拨号需要）",
            "忽略电池优化：" + if (power.isIgnoringBatteryOptimizations(packageName)) "✅" else "❌（建议开启）",
        ).joinToString("\n")
        logText.text = config.log.ifEmpty { "暂无记录" }
    }

    private fun missingPermissions(): Array<String> =
        REQUIRED_PERMISSIONS.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }.toTypedArray()

    private fun packageUri() = Uri.parse("package:$packageName")

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private companion object {
        const val REQUEST_PERMISSIONS = 1
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.CALL_PHONE)
    }
}
