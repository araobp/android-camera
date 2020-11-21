package jp.araobp.camera.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.pow
import kotlin.math.roundToInt

// Round a float value to the first decimal place
fun Float.roundToTheNth(n: Int): Float {
    val magnify = 10F.pow(n)
    return (this * magnify).roundToInt() / magnify
}

// Round a doube value to the first decimal place
fun Double.roundToTheNth(n: Int): Double {
    val magnify = 10.0.pow(n)
    return (this * magnify).roundToInt() / magnify
}
