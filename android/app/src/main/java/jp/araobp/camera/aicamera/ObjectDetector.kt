package jp.araobp.camera.aicamera

import android.content.Context
import android.graphics.*
import jp.araobp.camera.tflite.Classifier
import jp.araobp.camera.tflite.SsdMobileNetV2
import jp.araobp.camera.util.roundToTheNth
import kotlin.math.abs

class ObjectDetector(context: Context) {

    companion object {
        val TAG: String = this::class.java.simpleName

        const val MAX_NUM_RECOGNITIONS = 5
        const val CONFIDENCE_THRES = 40

        const val OBJECT_OFFSET_LEFT = 30  // 30 pixels
        const val OBJECT_OFFSET_RIGHT = SsdMobileNetV2.INPUT_SIZE - 30  // 30 pixels
        const val OBJECT_OFFSET_TOP = 30  // 30 pixels
        const val OBJECT_OFFSET_BOTTOM = SsdMobileNetV2.INPUT_SIZE - 30  // 30 pixels
        const val MAX_OBJECT_AREA = SsdMobileNetV2.INPUT_SIZE * SsdMobileNetV2.INPUT_SIZE * 3 / 4
        const val MIN_OBJECT_AREA = SsdMobileNetV2.INPUT_SIZE * SsdMobileNetV2.INPUT_SIZE * 1 / 500
    }

    private var results = ArrayList<Classifier.Recognition>()

    // Object detector: SSD Mobilenet
    private val ssdMobileNetV2 = SsdMobileNetV2(
        context, numThreads = 2
    )

    fun detect(bitmapFiltered: Bitmap,
               bitmapOriginal: Bitmap,
               confidenceThres: Int = CONFIDENCE_THRES,
               personOnly: Boolean = false): Bitmap? {
        // Scale down to 300x300 tensor as input for SSD MobileNetv2
        val inputBitmap = Bitmap.createScaledBitmap(
            bitmapOriginal,
            SsdMobileNetV2.INPUT_SIZE,
            SsdMobileNetV2.INPUT_SIZE,
            false
        )

        // Execute object detection by SSD MobileNetv2
        val result = ssdMobileNetV2.recognizeImage(inputBitmap)
        //Log.d(TAG, "Result: $result")

        bitmapOriginal.recycle()

        var newBitmap: Bitmap? = null

        try {
            result?.let {
                // Immutable bitmap to mutable bitmap
                newBitmap = bitmapFiltered.copy(Bitmap.Config.ARGB_8888 ,true)
                bitmapFiltered.recycle()
                val canvas = Canvas(newBitmap!!)
                results.clear()
                for (i in 0 until if (result.size < MAX_NUM_RECOGNITIONS) result.size else MAX_NUM_RECOGNITIONS) {

                    val r = result[i]
                    val location = r!!.getLocation()
                    val confidence = r.confidence
                    val title = r.title

                    if (!(personOnly && title != "person")) {
                        if (confidence!! > confidenceThres / 100F) {

                            // Rectangle location on 300x300 input tensor
                            val w = location.right - location.left
                            val h = location.bottom - location.top
                            val s = abs(w * h)

                            // Check if the recognized object fits in frame of each input bitmap
                            if (location.left > OBJECT_OFFSET_LEFT && location.right < OBJECT_OFFSET_RIGHT
                                && location.top > OBJECT_OFFSET_TOP && location.bottom < OBJECT_OFFSET_BOTTOM &&
                                s < MAX_OBJECT_AREA && s > MIN_OBJECT_AREA
                            ) {

                                val xRatio = canvas.width.toFloat() / SsdMobileNetV2.INPUT_SIZE
                                val yRatio = canvas.height.toFloat() / SsdMobileNetV2.INPUT_SIZE
                                val rectF = RectF(
                                    location.left * xRatio,
                                    location.top * yRatio,
                                    location.right * xRatio,
                                    location.bottom * yRatio
                                )

                                // Draw text
                                val paint = paintBoundingBox(title!!)
                                canvas.drawRoundRect(rectF, 8F, 8F, paint)

                                val confidenceInPercent = (confidence * 100F).roundToTheNth(1)
                                val text = "$title ${confidenceInPercent}%"
                                canvas.drawText(
                                    text,
                                    rectF.left,
                                    rectF.top - 10F,
                                    paintTitleBox(paint.color)
                                )

                                results.add(r)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return newBitmap
    }
}