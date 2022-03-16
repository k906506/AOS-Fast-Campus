# Android - ImageViewer

<p align="center">
  <img src="https://images.velog.io/images/k906506/post/2437309e-78e9-48a0-ba1c-ec6b5e18c69d/Screenshot_1647430876.png" height = "400px" width = "200px">
  </p>

# 키워드

1. Retrofit2, OkHttpLoggingInterceptor
2. Shimmer
3. Glide
4. Content Resolver
5. Unsplash API

# 개발 과정 [(노션에서 확인하기)](https://codekodo.notion.site/Android-ImageViewer-d6c26d3d4f9f417ab3e58a10d3c740ab)

## 1. Unsplash API 와 연결

### Model 구현

`Plugin` 인 `Json To Kotlin` 을 활용하면 `JSON` 형식에 맞춰서 자동으로 변환하고 `Model` 을 생성해준다.

### Interface 선언

`Retrofit` 의 요청에 대한 `interface` 를 선언한다.

```kotlin
package com.example.imageviewer.data.service

import com.example.imageviewer.BuildConfig
import com.example.imageviewer.data.models.PhotoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashApiService {
    @GET("photos/random?" + "client_id=${BuildConfig.UNSPLASH_ACCESS_KEY}" + "&count=30")
    suspend fun getRandomPhotos(
        @Query("query") query : String?
    ): Response<List<PhotoResponse>>
}
```

### 싱글톤으로 구현

`object` 를 활용하면 `Singleton` 으로 손쉽게 구현할 수 있다. 우선 `interface` 를 정의한다. 이 때 `HTTP` 요청에 대한 로깅을 출력하기 위해 `OkHttp` 의 `HttpLoggingInterceptor` 를 구현하여 인자로 전달했다.

```kotlin
package com.example.imageviewer.data.util

import com.example.imageviewer.BuildConfig
import com.example.imageviewer.data.Url
import com.example.imageviewer.data.models.PhotoResponse
import com.example.imageviewer.data.service.UnsplashApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object RetrofitUtil {
    private val unsplashApiService: UnsplashApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Url.UNSPLASH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(buildOkHttpClient())
            .build()
            .create()
    }

    suspend fun getRandomPhotos(query: String?): List<PhotoResponse>? =
        unsplashApiService.getRandomPhotos(query).body()

    private fun buildOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
}
```

## 2. RecyclerView 와 연결

`API` 를 통해 사진을 불러오는 것을 구현했으니 이제 사진을 보여줄 `RecyclerView` 를 구현해야 한다. 지금까지 구현했던 방식과 동일하다. `ListAdapter` 를 상속하는 클래스와 `RecyclerView.ViewHolder` 를 상속하는 이너 클래스를 정의한다. 또한 `ClickListener` 를 통해 `Photo` 객체가 전달될 수 있도록 한다.

```kotlin
package com.example.imageviewer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.imageviewer.data.models.PhotoResponse
import com.example.imageviewer.databinding.ItemPhotoBinding

class PhotoAdapter(val itemClickListener : (PhotoResponse) -> Unit) : ListAdapter<PhotoResponse, PhotoAdapter.ViewHolder>(diffUtil) {
    inner class ViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindViews(photo: PhotoResponse) {
            val dimensionRatio = photo?.height!! / photo?.width?.toFloat()!!
            val targetWidth =
                binding.root.resources.displayMetrics.widthPixels - (binding.root.paddingStart + binding.root.paddingEnd)
            val targetHeight = (targetWidth * dimensionRatio).toInt()

            binding.contentsContainer.layoutParams = binding.contentsContainer.layoutParams.apply {
                height = targetHeight
            }

            Glide.with(binding.root)
                .load(photo.urls?.regular)
                .thumbnail(
                    Glide.with(binding.root)
                        .load(photo.urls?.thumb)
                        .transition(DrawableTransitionOptions.withCrossFade())
                )
                .override(targetWidth, targetHeight)
                .into(binding.photoImageView)

            Glide.with(binding.root)
                .load(photo.user?.profileImageUrls?.small)
                .placeholder(R.drawable.shape_profile_placeholder)
                .circleCrop()
                .into(binding.profileImageView)

            if (photo.user?.name.isNullOrBlank()) {
                binding.authorTextView.isGone = true
            } else {
                binding.authorTextView.isGone = false
                binding.authorTextView.text = photo.user?.name
            }

            if (photo.description.isNullOrBlank()) {
                binding.descriptionTextView.isGone = true
            } else {
                binding.descriptionTextView.isGone = false
                binding.descriptionTextView.text = photo.description
            }

            binding.root.setOnClickListener {
                itemClickListener(photo)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPhotoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return holder.bindViews(currentList[position])
    }

    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<PhotoResponse>() {
            override fun areItemsTheSame(oldItem: PhotoResponse, newItem: PhotoResponse): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: PhotoResponse,
                newItem: PhotoResponse
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
```

## 3. 검색창 구현

### action 속성 부여

사용자가 검색어를 입력하고 소프트 키보드의 `Enter` 를 입력했을 때 검색으로 바로 넘어가기 위해서 `imeOption` 을 `actionSearch` 로 변경했다. 

```xml
<EditText
        android:id="@+id/searchEditText"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:background="@color/white"
        android:drawableStart="@drawable/ic_baseline_search_24"
        android:drawablePadding="6dp"
        android:elevation="8dp"
        android:hint="검색어를 입력해주세요."
        android:imeOptions="actionSearch"
        android:importantForAutofill="no"
        android:inputType="text"
        android:paddingHorizontal="12dp"
        android:textSize="14sp" />
```

### 리스너 구현

검색에 대한 리스너를 달아준다. `setOnEditorActionListener` 와 `actionId` 를 활용한다. 이 때 단순히 `EditText` 의 `text` 를 `API` 의 파라미터로 넘기면 끝나는 것이 아니다. 소프트 키보드를 화면에서 제거해줘야한다. `inputMethodManager` 를 활용했다. 

```kotlin
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
	}
```

## 3. Facebook Shimmer Library

`페이스북` 에서 만든 라이브러리인 `Shimmer` 를 사용하면 데이터 요청이 끝나기 전까지의 레이아웃에 특별한 효과를 부여할 수 있다. 우리가 흔히 쓰는 유튜브도 `Shimmer` 를 사용했다. 사용법은 간단하다. `Shimmer` 로 보여줄 레이아웃을 작성하면 된다.

```xml
					<com.facebook.shimmer.ShimmerFrameLayout
                android:id="@+id/shimmerLayout"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                tools:visibility="gone">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical">

                    <include layout="@layout/shimmer_item_photo" />

                    <include layout="@layout/shimmer_item_photo" />

                </LinearLayout>
            </com.facebook.shimmer.ShimmerFrameLayout>
```

이후 `MainActivity` 에서 해당 레이아웃에 `isGone` 속성을 부여하는 방식으로 구현했다.

```kotlin
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
```

## 4. 사진 다운로드

`Glide` 를 사용해서 `Url` 이미지를 `Bitmap` 이미지로 변환 후 갤러리에 저장하는 것이 가능하다. 겨울 방학에 구현했던 `ShotTraker` 는 `ContentResolver` 를 사용해서 구현했었다. 이번에도 동일하게 구현했다. 하지만 이번엔 `SDK` 에 대한 예외처리를 추가했다. 

```kotlin
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
```

## 5. 배경화면으로 지정

`WallPaperManager` 를 사용하면 `비트맵` 이미지를 배경화면으로 지정하는 것이 가능하다.

```kotlin
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
```
