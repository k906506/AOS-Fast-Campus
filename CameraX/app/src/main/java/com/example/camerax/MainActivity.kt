package com.example.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.ScaleGestureDetector
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.bumptech.glide.Glide
import com.example.camerax.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var camera: Camera? = null
    private var _isCaptured: Boolean = false
    private var _flashOn: Boolean = false

    private lateinit var imageCapture: ImageCapture

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val cameraProviderFuture by lazy {
        ProcessCameraProvider.getInstance(this)
    }

    private val cameraMainExecutor by lazy {
        ContextCompat.getMainExecutor(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        requestPermissions()
        bindCaptureListener()
        bindZoomListener()
        bindFlashListener()
    }

    private fun bindCameraUseCase() = with(binding) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(LENS_FACING)
            .build()

        cameraProviderFuture.addListener(
            {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().apply {
                    setTargetAspectRatio(AspectRatio.RATIO_16_9)
                }.build()

                val imageCaptureBuilder = ImageCapture.Builder()
                    .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setFlashMode(FLASH_MODE_AUTO)

                imageCapture = imageCaptureBuilder.build()

                try {
                    cameraProvider.unbindAll() // 기존에 바인딩 되어 있는 카메라는 해제
                    camera = cameraProvider.bindToLifecycle(
                        this@MainActivity, cameraSelector, preview, imageCapture
                    )
                    preview.setSurfaceProvider(viewFinder.surfaceProvider)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, cameraMainExecutor
        )
    }

    private fun bindCaptureListener() = with(binding) {
        captureButton.setOnClickListener {
            if (_isCaptured.not()) {
                _isCaptured = true
                captureCamera()
            }
        }
    }

    private var contentUri: Uri? = null

    @SuppressLint("ClickableViewAccessibility")
    private fun bindZoomListener() = with(binding) {

        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor

                camera?.cameraControl?.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(this@MainActivity, listener)
        viewFinder.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    private fun captureCamera() {
        if (::imageCapture.isInitialized.not()) return

        val fileName = "${System.currentTimeMillis()}.jpg"

        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageDetails
        ).build()

        imageCapture.takePicture(
            outputOptions,
            cameraMainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    contentUri = outputFileResults.savedUri
                    bindThumbnailImageView(contentUri)
                    _isCaptured = false
                }

                override fun onError(e: ImageCaptureException) {
                    e.printStackTrace()
                    _isCaptured = false
                }
            })
    }

    private fun bindThumbnailImageView(contentUri: Uri?) = with(binding) {
        Handler(Looper.getMainLooper()).post {
            Glide.with(root)
                .load(contentUri)
                .centerCrop()
                .into(thumbnailImageView)
        }
    }

    private fun bindFlashListener() = with(binding) {
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() ?: false
        if (hasFlash) {
            flashButton.setOnClickListener {
                changeFlashState()
            }
        } else {
            flashButton.isGone = true
        }
    }

    private fun changeFlashState() {
        if (_flashOn) { // 켜진 상태 -> 꺼진 상태로 변경
            camera?.cameraControl?.enableTorch(_flashOn.not())
            _flashOn = _flashOn.not()
        } else { // 꺼진 상태 -> 켜진 상태로 변경
            camera?.cameraControl?.enableTorch(_flashOn.not())
            _flashOn = _flashOn.not()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            permissions,
            REQUEST_CAMERA_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            bindCameraUseCase()
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        private val LENS_FACING = CameraSelector.LENS_FACING_BACK
    }
}