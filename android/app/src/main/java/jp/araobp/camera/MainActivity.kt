package jp.araobp.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import jp.araobp.camera.Properties.Companion.SCREEN_WIDTH_RATIO
import jp.araobp.camera.aicamera.ObjectDetector
import jp.araobp.camera.opecv.OpticalFlow
import jp.araobp.camera.opecv.colorFilter
import jp.araobp.camera.opecv.yuvToRgba
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    init {
        // OpenCV initialization
        OpenCVLoader.initDebug()
    }

    private lateinit var mCameraExecutor: ExecutorService

    private var mRectRight = 0
    private var mRectBottom = 0

    private val mOpticalFlow = OpticalFlow()
    private lateinit var mObjectDetector: ObjectDetector

    override fun onCreate(savedInstanceState: Bundle?) {

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Prevent the sleep mode programmatically
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide the navigation bar
        makeFullscreen()

        surfaceView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            mRectRight = (surfaceView.width * SCREEN_WIDTH_RATIO).roundToInt() - 1
            mRectBottom = surfaceView.height - 1
        }

        mObjectDetector = ObjectDetector(this)

        mCameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Image Analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                //.setTargetAspectRatio(AspectRatio.RATIO_16_9)
                //.setTargetResolution(Properties.TARGET_RESOLUTION)
                .build()
                .also {
                    it.setAnalyzer(mCameraExecutor, ImageAnalyzer())
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private inner class ImageAnalyzer() : ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {

            Log.d(TAG, "width: ${imageProxy.width}, height: ${imageProxy.height}")
            val mat = imageProxy.image?.yuvToRgba()
            imageProxy.close()

            mat?.let {

                var filtered = it

                //--- Digital signal processing with OpenCV START---//
                if (toggleButtonColorFilter.isChecked) {
                    filtered = colorFilter(filtered, "yellow", "red")
                }

                if (toggleButtonOpticalFlow.isChecked) {
                    filtered = mOpticalFlow.process(filtered)
                }
                //--- Digital signal processing with OpenCV END ---//

                var bitmapFiltered =
                    Bitmap.createBitmap(
                        imageProxy.width,
                        imageProxy.height,
                        Bitmap.Config.ARGB_8888
                    )

                Utils.matToBitmap(filtered, bitmapFiltered);

                // Object detection with TensorFlow Lite
                if (toggleButtonObjectDetection.isChecked) {
                    bitmapFiltered = mObjectDetector.detect(bitmapFiltered)
                }


                val src = Rect(0, 0, imageProxy.width - 1, imageProxy.height - 1)
                val dest = Rect(0, 0, mRectRight, mRectBottom)

                val canvas = surfaceView.holder.lockCanvas()
                canvas.drawColor(0, PorterDuff.Mode.CLEAR)
                canvas.drawBitmap(bitmapFiltered, src, dest, null)
                surfaceView.holder.unlockCanvasAndPost(canvas)
            }
        }
    }


    private fun makeFullscreen() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}