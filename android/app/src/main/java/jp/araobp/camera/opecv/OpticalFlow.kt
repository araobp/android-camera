package jp.araobp.camera.opecv

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*
import org.opencv.video.Video

// [Reference] http://me10.sblo.jp/article/88289624.html
class OpticalFlow {

    private var mPrevMat: Mat? = null
    private lateinit var mCurrentMat: Mat

    fun process(src: Mat): Mat {

        val pt1 = Point()
        val pt2 = Point()

        if (mPrevMat == null) {
            mCurrentMat = src.clone()
            Imgproc.cvtColor(mCurrentMat, mCurrentMat, Imgproc.COLOR_RGBA2GRAY)
            mPrevMat = mCurrentMat.clone()
        } else {
            mPrevMat = mCurrentMat
            mCurrentMat = src.clone()
            Imgproc.cvtColor(mCurrentMat, mCurrentMat, Imgproc.COLOR_RGBA2GRAY)
        }

        val flow = Mat(mCurrentMat.size(), CvType.CV_32FC2)
        Video.calcOpticalFlowFarneback(
            mPrevMat, mCurrentMat,
            flow, 0.5, 3, 15, 3, 5, 1.1, 0
        )

        val dst = src.clone()

        var i = 0
        while (i < mCurrentMat.size().height) {
            var j = 0
            while (j < mCurrentMat.size().width) {
                pt1.x = j.toDouble()
                pt1.y = i.toDouble()
                pt2.x = j + flow[i, j][0]
                pt2.y = i + flow[i, j][1]
                val color = Scalar(255.0, 0.0, 0.0, 255.0)
                arrowedLine(
                    dst,
                    pt1,
                    pt2,
                    color, 2, LINE_8, 0, 0.4
                )
                j += 20
            }
            i += 20
        }

        return dst
    }
}
