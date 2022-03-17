<p align = "center">
   <img src="https://images.velog.io/images/k906506/post/6b65bb91-3f71-41ef-bb82-3300a321c2ef/Screenshot_20220317-222825_CameraX.jpg" height = "395px" width = "200px"/>
   <img src="https://images.velog.io/images/k906506/post/06aceff4-f120-4922-a7eb-dc786b6023fc/Screenshot_20220317-222828_CameraX.jpg" height = "395px" width = "200px"/>
</p>

# 키워드

1. CameraX
2. Glide
3. Content Resolver
4. ScaleGestureDetector

# 개발 과정

## 1. 레이아웃 구현

레이아웃을 구현하기 전에 `CameraX` 에 대한 의존성을 추가한다.

```xml
implementation "androidx.camera:camera-camera2:$camerax_version"
implementation "androidx.camera:camera-lifecycle:$camerax_version"
implementation "androidx.camera:camera-view:$camerax_view_version"
```

카메라를 통해 찍히는 화면이 보여질 `View` 를 구현해야 한다. `Preview.View` 를 통해 이를 구현할 수 있다. 해당 `View` 가 화면에 꽉차도록 `constraintHeight_default` 속성을 부여했다. 

```xml
		<androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/viewFinderContainer"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent">

        <androidx.camera.view.PreviewView
            android:id="@+id/viewFinder"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:clickable="false"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintDimensionRatio="h, 16:9"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintHeight_default="percent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
```

나머지 버튼에 대한 레이아웃도 구현해준다. `imageButton` 을 활용하여 구현했다.

```xml
		<ImageButton
        android:id="@+id/captureButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="20dp"
        android:background="@drawable/ic_baseline_camera_24"
        android:backgroundTint="@color/white"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent" />

    <ImageView
        android:id="@+id/thumbnailImageView"
        android:layout_width="60dp"
        android:layout_height="60dp"
        app:layout_constraintBottom_toBottomOf="@id/captureButton"
        app:layout_constraintEnd_toStartOf="@id/captureButton"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="@id/captureButton" />

    <ImageButton
        android:id="@+id/flashButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:background="@drawable/ic_baseline_flash_on_24"
        android:backgroundTint="@color/white"
        app:layout_constraintBottom_toBottomOf="@+id/captureButton"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toEndOf="@+id/captureButton"
        app:layout_constraintTop_toTopOf="@+id/captureButton" />
```

## 2. 권한 요청하기

`카메라` 역시 사용자에게 직접 권한을 요청 받아야 한다. 항상 구현했던 것처럼 구현했다.

```kotlin
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
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
```

## 3. ViewFinder 구현

사용자가 카메라를 통해 촬영할 사진을 미리 볼 수 있도록 `ViewFinder` 를 구현한다. 간단하게 말하면 위에서 구현한 레이아웃과 연결한다는 의미이다. 

### CameraProviderFuture, CameraMainExecutor

먼저 두 가지가 필요하다. 카메라에 대한 `공급자` 와 이를 실행할 수 있는 `쓰레드` 를 정의한다.

```kotlin
private val cameraProviderFuture by lazy {
		ProcessCameraProvider.getInstance(this)
}

private val cameraMainExecutor by lazy {
    ContextCompat.getMainExecutor(this)
}
```

```kotlin
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
```

## 4. 사진 촬영 기능

`Preview` 에 카메라 영역을 설정하는 것은 구현했으니 이제 실제로 버튼을 눌렀을 때 촬영이 되도록 `클릭 리스너` 를 달아준다. 

```kotlin
private fun bindCaptureListener() = with(binding) {
        captureButton.setOnClickListener {
            if (_isCaptured.not()) {
                _isCaptured = true
                captureCamera()
            }
        }
    }
```

`파일 경로` 를 현재 시각으로 설정하고 `이미지 세부 정보` 를 정의한다. 이후 `ImageCapture` 의 옵션으로 부여하고 `takePicture` 로 사진을 촬영한다. `이미지 저장` 에 대한 `콜백` 을 구현한다. 

```kotlin
private fun captureCamera() {
        if (::imageCapture.isInitialized.not()) return

        val fileName = "${System.currentTimeMillis()}.jpg"

        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
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
```

## 5. 카메라 줌 기능

단순히 촬영만 하는 카메라는 뭔가 많이 아쉽다. 그래서 줌 기능을 추가해줬다. `ScaleGestureDetector` 을 구현하고 이를 `Preview` 의 `터치리스너` 와 연결했다.

```kotlin
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
```

## 6. 촬영한 사진 미리보기

사진이 정상적으로 촬영되면 좌측 하단에 `썸네일` 로 볼 수 있게끔 `Glide` 를 활용해서 `ImageView` 에 넣어줬다. `클릭 리스너` 를 통해서 촬영한 모든 사진을 볼 수 있도록 액티비티와 연결해주면 괜찮을 듯 하다.

```kotlin
private fun bindThumbnailImageView(contentUri: Uri?) = with(binding) {
        Handler(Looper.getMainLooper()).post {
            Glide.with(root)
                .load(contentUri)
                .centerCrop()
                .into(thumbnailImageView)
        }
    }
```

## 7. 플래시 기능

마지막으로 우측 하단에는 `플래시` 를 키고 끌 수 있도록 `ImageButton` 을 구현했다. 현재 가지고 있는 디바이스가 플래시를 지원하지 않는 기기라서 테스트를 할 수 없지만 정상적으로 작동할 것이다.

```kotlin
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
```
