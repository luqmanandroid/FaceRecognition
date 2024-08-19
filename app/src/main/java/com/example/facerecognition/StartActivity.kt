package com.example.facerecognition

/*import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.VideoView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class StartActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var selectVideoButton: Button
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var selectVideoLauncher: ActivityResultLauncher<Intent>
    private var isDrawing = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        videoView = findViewById(R.id.videoView)
        selectVideoButton = findViewById(R.id.selectVideoButton)
        surfaceView = findViewById(R.id.surfaceView)
        surfaceHolder = surfaceView.holder

        // Register the ActivityResultLauncher in onCreate
        selectVideoLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val videoUri: Uri? = result.data?.data
                videoUri?.let {
                    videoView.setVideoURI(it)
                    videoView.setOnPreparedListener { mediaPlayer ->
                        processVideo(it)
                        mediaPlayer.start()
                    }
                }
            }
        }

        selectVideoButton.setOnClickListener {
            selectVideoFromGallery()
        }

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // Initialize or start processing when the surface is created
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // Handle changes in surface dimensions or format
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // Clean up or stop processing when the surface is destroyed
                stopDrawing()
            }
        })
    }

    private fun startDrawing() {
        isDrawing = true
        // Optionally start a repeating draw task
    }

    private fun stopDrawing() {
        isDrawing = false
        // Optionally stop the repeating draw task
    }
    private fun selectVideoFromGallery() {
        // Use the already registered selectVideoLauncher
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        selectVideoLauncher.launch(intent)
    }

    private fun processVideo(videoUri: Uri) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, videoUri)

        val detectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()

        val detector = FaceDetection.getClient(detectorOptions)

        val videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
        var videoFrameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toFloat() ?: 0f

        // Ensure videoFrameRate is valid
        if (videoFrameRate <= 0f) {
            videoFrameRate = 30f // Set a default frame rate if it's invalid
        }

        val step = maxOf((1000 / videoFrameRate).toLong(), 1L) // Ensure step is at least 1

        // Log for debugging
        println("Video Duration: $videoDuration, Frame Rate: $videoFrameRate, Step: $step")

        Thread {
            for (i in 0 until videoDuration step step) {
                val bitmap = retriever.getFrameAtTime(i * 1000)
                val image = bitmap?.let { InputImage.fromBitmap(it, 0) }

                image?.let {
                    detector.process(it)
                        .addOnSuccessListener { faces ->
                            drawBoundingBoxes(faces)
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                        }
                }
            }
        }.start()
    }


    private fun drawBoundingBoxes(faces: List<com.google.mlkit.vision.face.Face>) {
        val canvas = try {
            surfaceHolder.lockCanvas()
        } catch (e: Exception) {
            // Handle exception if canvas cannot be locked
            return
        }

        canvas?.let {
            try {
                it.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

                val paint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 8f
                    color = ContextCompat.getColor(this@StartActivity, android.R.color.holo_red_light)
                }

                for (face in faces) {
                    val boundingBox = face.boundingBox
                    val rect = RectF(boundingBox)
                    it.drawRect(rect, paint)
                }
            } finally {
                surfaceHolder.unlockCanvasAndPost(it)
            }
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}*/
import android.graphics.RectF
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StartActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceOverlayView: FaceOverlayView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)


        previewView = findViewById(R.id.previewView)
        faceOverlayView = findViewById(R.id.faceOverlay)
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                // Handle errors
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()

            val detector = FaceDetection.getClient(options)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val faceBoundingBoxes = faces.map { face ->
                        calculateBoundingBox(face, imageProxy)
                    }
                    faceOverlayView.setFaces(faceBoundingBoxes)
                }
                .addOnFailureListener { e ->
                    // Handle detection failure
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun calculateBoundingBox(face: Face, imageProxy: ImageProxy): RectF {
        val boundingBox = face.boundingBox

        // Calculate the correct scale factor to map the face bounding box to the PreviewView size
        val scaleX = previewView.width.toFloat() / imageProxy.height.toFloat()
        val scaleY = previewView.height.toFloat() / imageProxy.width.toFloat()

        // Adjust the bounding box
        val adjustedLeft = boundingBox.left * scaleX
        val adjustedTop = boundingBox.top * scaleY
        val adjustedRight = boundingBox.right * scaleX
        val adjustedBottom = boundingBox.bottom * scaleY

        return RectF(adjustedLeft, adjustedTop, adjustedRight, adjustedBottom)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
