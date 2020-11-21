package jp.araobp.camera

import android.content.Context

class Properties(val context: Context) {

    companion object {
        const val PREFS_NAME = "camera"

        const val IMAGE_ASPECT_RATIO = 4F/3F
        const val SHIFT_IMAGE = 100

        const val MQTT_TOPIC_IMAGE = "image"
    }

    var mqttServer = "localhost"
    var mqttUsername = "simulator"
    var mqttPassword = "simulator"
    var remoteCamera = false
    var showFps = false

    init {
        load()
    }

    fun load() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        mqttServer = prefs.getString("mqttServer", "localhost").toString()
        mqttUsername = prefs.getString("mqttUsername", "anonymous").toString()
        mqttPassword = prefs.getString("mqttPassword", "password").toString()
        remoteCamera = prefs.getBoolean("remoteCamera", false)
        showFps = prefs.getBoolean("fps", false)
    }

    fun save() {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putString("mqttServer", mqttServer)
        editor.putString("mqttUsername", mqttUsername)
        editor.putString("mqttPassword", mqttPassword)
        editor.putBoolean("remoteCamera", remoteCamera)
        editor.putBoolean("fps", showFps)
        editor.apply()
    }
}