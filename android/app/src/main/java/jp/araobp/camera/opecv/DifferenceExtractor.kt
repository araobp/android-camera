package jp.araobp.camera.opecv

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.LINE_8
import org.opencv.imgproc.Imgproc.LINE_AA
import org.opencv.video.Video


class DifferenceExtractor {

    private var mBackgroundSubtractor = Video.createBackgroundSubtractorMOG2()

    public fun update(mat: Mat, contour: Boolean = false): Mat {
        val maskImg = Mat()
        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()

        mBackgroundSubtractor.apply(mat, maskImg)

        Imgproc.medianBlur(maskImg, maskImg, 11)

        Imgproc.threshold(
            maskImg, maskImg, 32.0, 255.0, Imgproc.THRESH_BINARY
        )

        Imgproc.findContours(
            maskImg, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE
        )

        lateinit var img: Mat

        if (contour) {
            img = mat.clone()
            for (i in contours.indices) {
                Imgproc.drawContours(img, contours, i, COLOR_RED, 2, LINE_AA, hierarchy, 1, Point())
            }
        } else {
            img = maskImg
        }

        return img
    }
}
