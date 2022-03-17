package com.example.camerax

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerax.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var displayId: Int = -1
    private var camera: Camera? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val cameraProviderFuture by lazy {
        ProcessCameraProvider.getInstance(this)
    }

    private val displayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private val cameraMainExecutor by lazy {
        ContextCompat.getMainExecutor(this)
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            if (this@MainActivity.displayId == displayId) {

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        requestPermissions()

    }


    private fun startCamera(viewFinder: PreviewView) {
        displayManager.registerDisplayListener(displayListener, null)
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewFinder.postDelayed({
            displayId = viewFinder.display.displayId
            bindCameraUseCase()
        }, 10)
    }

    private fun bindCameraUseCase() = with(binding) {
        val rotation = viewFinder.display.rotation
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(LENS_FACING)
            .build()

        cameraProviderFuture.addListener(
            {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().apply {
                    setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    setTargetRotation(rotation)
                }.build()

                val imageCaptureBuilder = ImageCapture.Builder()
                    .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setTargetRotation(rotation)
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
            startCamera(binding.viewFinder)
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private val permissions = arrayOf(
            Manifest.permission.CAMERA
        )
        private val LENS_FACING = CameraSelector.LENS_FACING_BACK
    }
}