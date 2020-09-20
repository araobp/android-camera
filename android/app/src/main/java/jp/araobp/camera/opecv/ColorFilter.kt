package jp.araobp.camera.opecv

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

// Reference: https://stackoverflow.com/questions/51229126/how-to-find-the-red-color-regions-using-opencv
val COLOR_RANGES = mapOf(
    "red" to listOf(Scalar(0.0, 180.0, 120.0), Scalar(15.0, 255.0, 255.0)),
    "pink" to listOf(Scalar(145.0, 120.0, 120.0), Scalar(180.0, 255.0, 255.0)),
    "yellow" to listOf(Scalar(22.0, 180.0, 120.0), Scalar(33.0, 255.0, 255.0)),
    "orange" to listOf(Scalar(15.0, 120.0, 120.0), Scalar(22.0, 255.0, 255.0)),
    "green" to listOf(Scalar(33.0, 80.0, 60.0), Scalar(70.0, 255.0, 255.0)),
    "blue" to listOf(Scalar(90.0, 80.0, 60.0), Scalar(130.0, 255.0, 255.0)),
    "black" to listOf(Scalar(0.0, 0.0, 0.0), Scalar(360.0, 255.0, 20.0)),
    "white" to listOf(Scalar(0.0, 0.0, 80.0), Scalar(360.0, 20.0, 255.0))
)

/**
 * Color filter
 *
 * @param bitmap
 * @param colorRangeId Example: a list of "yellow" and "pink"
 */
fun colorFilter(src: Mat, vararg colorRangeId: String): Mat {
    for (id in colorRangeId) {
        check(COLOR_RANGES.containsKey(id))
    }

    val dst = Mat()  // RGB
    val srcHsv = Mat()  // HSV
    var mask: Mat? = null

    Imgproc.cvtColor(src, srcHsv, Imgproc.COLOR_RGB2HSV)

    for (id in colorRangeId) {
        val tempMask = Mat()
        Core.inRange(
            srcHsv,
            COLOR_RANGES.getValue(id)[0],
            COLOR_RANGES.getValue(id)[1],
            tempMask
        )
        if (mask == null) {
            mask = tempMask
        } else {
            Core.add(mask, tempMask, mask)
        }
    }

    src.copyTo(dst, mask)
    return dst
}
