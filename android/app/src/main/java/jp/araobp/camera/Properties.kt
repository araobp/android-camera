package jp.araobp.camera

import android.content.Context

class Properties(val context: Context) {

    companion object {
        const val PREFS_NAME = "camera"
        val SCREEN_WIDTH_RATIO = 12F / 19F  // 19:9 to 4:3
        val MQTT_TOPIC_IMAGE = "image"
    }

    var mqttServer = "localhost"
    var mqttUsername = "simulator"
    var mqttPassword = "simulator"
    var remoteCamera = false

    init {
        load()
    }

    fun load() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        mqttServer = prefs.getString("mqttServer", "localhost").toString()
        mqttUsername = prefs.getString("mqttUsername", "anonymous").toString()
        mqttPassword = prefs.getString("mqttPassword", "password").toString()
        remoteCamera = prefs.getBoolean("remoteCamera", false)
    }

    fun save() {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putString("mqttServer", mqttServer)
        editor.putString("mqttUsername", mqttUsername)
        editor.putString("mqttPassword", mqttPassword)
        editor.putBoolean("remoteCamera", remoteCamera)
        editor.apply()
    }
}