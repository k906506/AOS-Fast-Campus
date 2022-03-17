package com.example.camerax

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class CameraXApplication : Application(), CameraXConfig.Provider {
    override fun getCameraXConfig(): CameraXConfig = Camera2Config.defaultConfig()
}