package jp.araobp.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.internal.utils.ImageUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import jp.araobp.camera.Properties.Companion.IMAGE_ASPECT_RATIO
import jp.araobp.camera.aicamera.ObjectDetector
import jp.araobp.camera.net.IMqttReceiver
import jp.araobp.camera.net.MqttClient
import jp.araobp.camera.opecv.DifferenceExtractor
import jp.araobp.camera.opecv.OpticalFlow
import jp.araobp.camera.opecv.colorFilter
import jp.araobp.camera.opecv.yuvToRgba
import jp.araobp.camera.util.saveImage
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    init {
        // OpenCV initialization
        OpenCVLoader.initDebug()
    }

    private lateinit var mProps: Properties

    private lateinit var mCameraExecutor: ExecutorService

    private var mRectRight = 0
    private var mRectBottom = 0

    private val mOpticalFlow = OpticalFlow()
    private lateinit var mObjectDetector: ObjectDetector
    private val mDifference = DifferenceExtractor()

    private var mShutterPressed = false

    private lateinit var mMqttClient: MqttClient

    private var handlerThread: HandlerThread = HandlerThread("analyzer").apply { start() }
    private var handler = Handler(handlerThread.looper)

    val mqttReceiver = object : IMqttReceiver {
        override fun messageArrived(topic: String?, message: MqttMessage?) {
            message?.let {
                if (mProps.remoteCamera) {
                    if (topic == Properties.MQTT_TOPIC_IMAGE) {
                        Log.d(TAG, "mqtt message received on ${Properties.MQTT_TOPIC_IMAGE}")
                        handler.post {
                            val jpegByteArray = it.payload
                            val bitmap =
                                BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.size)
                            val mat = Mat()
                            Utils.bitmapToMat(bitmap, mat)
                            val filterdBitmap = processImage(mat)
                            drawImage(filterdBitmap)
                        }
                    }
                }
            }
        }
    }

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
            mRectRight = (surfaceView.height * IMAGE_ASPECT_RATIO).roundToInt() - 1
            mRectBottom = surfaceView.height - 1
        }

        mObjectDetector = ObjectDetector(this)

        mCameraExecutor = Executors.newSingleThreadExecutor()

        mProps = Properties(this)

        // Settings dialog
        buttonSettings.setOnClickListener {

            mProps.load()

            val dialog = Dialog(this)
            dialog.setContentView(R.layout.settings)

            val editTextMqttServer = dialog.findViewById<EditText>(R.id.editTextMqttServer)
            editTextMqttServer.setText(mProps.mqttServer)

            val editTextMqttUsername = dialog.findViewById<EditText>(R.id.editTextMqttUsername)
            editTextMqttUsername.setText(mProps.mqttUsername)

            val editTextMqttPassword = dialog.findViewById<EditText>(R.id.editTextMqttPassword)
            editTextMqttPassword.setText(mProps.mqttPassword)

            val checkBoxRemoteCamera = dialog.findViewById<CheckBox>(R.id.checkBoxRemoteCamera)
            checkBoxRemoteCamera.isChecked = mProps.remoteCamera

            editTextMqttServer.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) =
                    Unit

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
                override fun afterTextChanged(p0: Editable?) {
                    mProps.mqttServer = editTextMqttServer.text.toString()
                }
            })

            editTextMqttUsername.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) =
                    Unit

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
                override fun afterTextChanged(p0: Editable?) {
                    mProps.mqttUsername = editTextMqttUsername.text.toString()
                }
            })

            editTextMqttPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) =
                    Unit

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
                override fun afterTextChanged(p0: Editable?) {
                    mProps.mqttPassword = editTextMqttPassword.text.toString()
                }
            })

            checkBoxRemoteCamera.setOnCheckedChangeListener { _, isChecked ->
                mProps.remoteCamera = isChecked
            }

            dialog.setOnDismissListener {
                mProps.save()
            }

            dialog.show()
        }

        buttonQuit.setOnClickListener {
            this@MainActivity.finish()
            exitProcess(0)
        }
    }

    override fun onResume() {
        super.onResume()
        mMqttClient = MqttClient(
            context = this,
            mqttServer = mProps.mqttServer,
            mqttUsername = mProps.mqttUsername,
            mqttPassword = mProps.mqttPassword,
            clientId = TAG,
            receiver = mqttReceiver
        )
        mMqttClient.connect(listOf(Properties.MQTT_TOPIC_IMAGE))
    }

    override fun onPause() {
        super.onPause()
        mMqttClient.destroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraExecutor.shutdown()
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
                    this as LifecycleOwner, cameraSelector, imageAnalyzer
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

    private fun drawImage(bitmap: Bitmap) {
        val src = Rect(0, 0, bitmap.width - 1, bitmap.height - 1)
        val dest = Rect(Properties.SHIFT_IMAGE, 0, mRectRight + Properties.SHIFT_IMAGE, mRectBottom)

        val canvas = surfaceView.holder.lockCanvas()
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(bitmap, src, dest, null)
        surfaceView.holder.unlockCanvasAndPost(canvas)

        if (mShutterPressed) {
            saveImage(bitmap, this@MainActivity, "android-camera")
            mShutterPressed = false
        }
    }

    // Image processing pipeline with OpenCV and TensorFlow Lite
    private fun processImage(mat: Mat): Bitmap {

        var filtered = mat.clone()

        //--- Digital signal processing with OpenCV START---//

        if (toggleButtonColorFilter.isChecked) {
            filtered = colorFilter(filtered, "yellow", "red")
        }

        if (toggleButtonOpticalFlow.isChecked) {
            filtered = mOpticalFlow.update(filtered)
        }

        if (toggleButtonMotionDetection.isChecked) {
            filtered = mDifference.update(filtered, contour = false)
        }

        if (toggleButtonContourExtraction.isChecked) {
            filtered = mDifference.update(filtered, contour = true)
        }

        //--- Digital signal processing with OpenCV END ---//

        val width = filtered.cols()
        val height = filtered.rows()

        var bitmapFiltered =
            Bitmap.createBitmap(
                width, height,
                Bitmap.Config.ARGB_8888
            )

        Utils.matToBitmap(filtered, bitmapFiltered);

        // Object detection with TensorFlow Lite START

        if (toggleButtonObjectDetection.isChecked) {
            val bitmapOriginal = Bitmap.createBitmap(
                width, height,
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(mat, bitmapOriginal)
            bitmapFiltered = mObjectDetector.detect(bitmapFiltered, bitmapOriginal)
        }

        // Object detection with TensorFlow Lite END

        return bitmapFiltered
    }

    private inner class ImageAnalyzer() : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {

            if (!mProps.remoteCamera) {
                Log.d(TAG, "width: ${imageProxy.width}, height: ${imageProxy.height}")
                val mat = imageProxy.image?.yuvToRgba()
                imageProxy.close()
                mat?.let {
                    val bitmap = processImage(it)
                    drawImage(bitmap)
                }
            }
        }
    }

    private fun makeFullscreen() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    /**
     * Press volume key (volume up) or press a button on the bluetooth remote shutter
     * to take a picture of the current Mat object
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action: Int = event.action
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    mShutterPressed = true
                }
                true
            }
            else -> super.dispatchKeyEvent(event)
        }
    }
}