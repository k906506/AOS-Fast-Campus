package com.example.imageviewer

import android.Manifest
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.imageviewer.data.models.PhotoResponse
import com.example.imageviewer.data.util.RetrofitUtil
import com.example.imageviewer.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val adapter by lazy {
        PhotoAdapter(
            itemClickListener = {
                showDownloadPhotoConfirmationDialog(it)
            }
        )
    }

    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initViews()
        bindViews()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fetchRandomPhotos()
        } else {
            requestWriteStoragePermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun fetchRandomPhotos(query: String? = null) = scope.launch {
        try {
            binding.errorDescriptionTextView.isGone = true
            RetrofitUtil.getRandomPhotos(query)?.let { photos ->
                adapter.submitList(photos)
            }
            binding.recyclerView.visibility = View.VISIBLE
        } catch (e: Exception) {
            binding.recyclerView.visibility = View.INVISIBLE
            binding.errorDescriptionTextView.isGone = false
        } finally {
            binding.shimmerLayout.isGone = true
            binding.refreshLayout.isRefreshing = false
        }
    }

    private fun initViews() = with(binding) {
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.adapter = adapter
    }

    private fun bindViews() = with(binding) {
        searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentFocus?.let { view ->
                    val inputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)

                    view.clearFocus()


                    fetchRandomPhotos(searchEditText.text.toString())
                }
            }
            true
        }

        refreshLayout.setOnRefreshListener {
            fetchRandomPhotos(binding.searchEditText.text.toString())
        }
    }

    private fun showDownloadPhotoConfirmationDialog(photo: PhotoResponse) {
        AlertDialog.Builder(this)
            .setMessage("사진을 저장하시겠습니까?")
            .setPositiveButton("저장") { dialog, _ ->
                downloadPhoto(photo.urls?.full)
                dialog.dismiss()

            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun downloadPhoto(photoUrl: String?) {
        photoUrl ?: return

        Glide.with(this)
            .asBitmap()
            .load(photoUrl)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(
                object : CustomTarget<Bitmap>(SIZE_ORIGINAL, SIZE_ORIGINAL) {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        saveBitmapToMediaStore(resource)


                        val wallpaperManager = WallpaperManager.getInstance(this@MainActivity)
                        val snackBar = Snackbar.make(binding.root, "다운로드 완료", Snackbar.LENGTH_SHORT)

                        if (wallpaperManager.isWallpaperSupported && wallpaperManager.isSetWallpaperAllowed) {
                            snackBar.setAction("배경화면으로 지정") {
                                try {
                                    wallpaperManager.setBitmap(resource)
                                } catch (e: Exception) {
                                    Snackbar.make(
                                        binding.root,
                                        "배경화면 변경에 실패하였습니다.",
                                        Snackbar.LENGTH_SHORT
                                    )
                                }
                            }
                            snackBar.duration = Snackbar.LENGTH_INDEFINITE
                        }
                        snackBar.show()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onLoadStarted(placeholder: Drawable?) {
                        super.onLoadStarted(placeholder)

                        Snackbar.make(binding.root, "다운로드 중...", Snackbar.LENGTH_INDEFINITE).show()
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)

                        Snackbar.make(binding.root, "다운로드 실패", Snackbar.LENGTH_SHORT).show()
                    }

                }
            )

    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap) {
        val fileName = "${System.currentTimeMillis()}.jpg"
        val resolver = applicationContext.contentResolver
        val imageCollectionUri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(imageCollectionUri, imageDetails)

        imageUri ?: return

        resolver.openOutputStream(imageUri).use { outputStream ->
            bitmap.compress(
                Bitmap.CompressFormat.JPEG, 100, outputStream
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            imageDetails.clear()
            imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, imageDetails, null, null)
        }
    }

    private fun requestWriteStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            permissions,
            REQUEST_WRITE_EXTERNAL_STORAGE
        )
    }

    companion object {
        private val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        const val REQUEST_WRITE_EXTERNAL_STORAGE = 100
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchRandomPhotos()
        } else {
            finish()
        }
    }
}