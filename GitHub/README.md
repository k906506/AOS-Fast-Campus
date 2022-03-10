
# Android - GitHub Repository

<center>
[<img src="https://images.velog.io/images/k906506/post/b12a988c-b67b-47c6-87b4-e7cc9e69b82c/ezgif.com-gif-maker%20(4).gif" height="395" width="200">]()
</center>

# 키워드

1.  깃허브 API
2.  Retrofit2
3.  Room
4.  SharedPreference

# 개발 과정 [(노션에서 확인하기)](https://codekodo.notion.site/Android-GitHub-Repository-c3e74b5fc9854110b0493adf29a52898)

## 1. 사용자 확인

### 암시적 인텐트 선언

깃허브 API를 통해 사용자가 깃허브 계정을 로그인하면 `고유한 토큰` 을 부여받을 수 있도록 `암시적 인텐트` 를 선언해줬다. `액션` 과 `카테고리` 를 지정해주고 로그인에 성공한 경우 해당 액티비티로 돌아올 수 있도록 `외부 스킴` 을 설정해줬다.

```xml
	<intent-filter>
                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />

                <data
                    android:host="github-auth"
                    android:scheme="com.example.github" />
	</intent-filter>

```

```kotlin
	private fun loginGithub() {
        val loginUrl = Uri.Builder().scheme("https").authority("github.com")
            .appendPath("login")
            .appendPath("oauth")
            .appendPath("authorize")
            .appendQueryParameter("client_id", Key.GITHUB_CLIENT_ID)
            .build()

        CustomTabsIntent.Builder().build().also {
            it.launchUrl(this, loginUrl)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // todo getAccessToken
        intent?.data?.getQueryParameter("code")?.let {
					Log.d("AuthToken", it)
        }
    }

```

### 자동 로그인

`토큰` 으로 사용자를 확인하고 정상적으로 판단되면 `SharedPreference` 에 사용자 정보를 저장한다. 어플리케이션을 재실행했을 때 로그인 페이지를 자동으로 넘어가기 위함이다.

```kotlin
	private suspend fun getAccessToken(code: String) = launch(coroutineContext) {
        try {
            withContext(Dispatchers.IO) {
                val response = RetrofitUtil.authApiService.getAccessToken(
                    clientId = Key.GITHUB_CLIENT_ID,
                    clientSecret = Key.GITHUB_CLIENT_SECRET,
                    code = code
                )

                if (response.isSuccessful) {
                    val accessToken = response.body()?.accessToken ?: "login"
                    if (accessToken.isNotEmpty()) {
                        authTokenProvider.setToken(accessToken)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

```

```kotlin
class AuthTokenProvider(private val context: Context) {
    fun setToken(token: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(Key.KEY_AUTH_TOKEN, token)
            .apply()
    }

    val getToken: String?
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Key.KEY_AUTH_TOKEN, null)
}

```

로그인이 되어 있는 경우 로그인 화면을 자동으로 넘어가는 것을 볼 수 있다.

## 2. 레포지토리 검색

이 기능 역시 깃허브 API를 이용했다. `레트로핏` 을 사용하기 위해 `DTO` , `Interface` , `Object` 를 구현했다.

### DTO

```kotlin
data class GithubRepoSearchResponse(
    val totalCount: Int,
    val items: List<GithubRepository>
)

```

### Interface

```kotlin
interface GithubApiService {
    @GET("search/repositories")
    suspend fun searchRepositories(@Query("q") query: String): Response<GithubRepoSearchResponse>

    @GET("repos/{owner}/{name}")
    suspend fun getRepository(
        @Path("owner") ownerLogin: String,
        @Path("name") repoName: String
    ): Response<GithubRepository>
}

```

### Object

```kotlin
object RetrofitUtil {
    val authApiService: AuthApiService by lazy {
        getGithubAuthRetrofit().create(AuthApiService::class.java)
    }

    private fun getGithubAuthRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Key.GITHUB_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val githubApiSearchService: GithubApiService by lazy {
        getGithubSearchRetrofit().create(
            GithubApiService::class.java
        )
    }

    private fun getGithubSearchRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Key.GITHUB_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

}

```

### RecyclerView 에 전달

`response` 의 결과를 `adapter` 에 넣어준다. `RecyclerView Adapter` 를 구현할 때 `RecyclerView.Adapter` 를 상속하는 것이 아닌 `ListAdapter` 를 상속하도록 구현했다. 따라서 `submistList` 를 통해 결과를 전달한다.

```kotlin
private fun searchKeyword(keyword: String) = launch {
        withContext(Dispatchers.IO) {
            val response = RetrofitUtil.githubApiSearchService.searchRepositories(keyword)
            if (response.isSuccessful) {
                val body = response.body()
                withContext(Dispatchers.Main) {
                    adapter.submitList(body?.items)
                }
            }
        }
    }

```

## 3. 레포지토리 상세 조회

검색 결과에서 특정 레포지토리를 클릭하면 해당 레포지토리에 대한 상세 정보가 출력되도록 구현하고자 한다. 우선 `RecyclerView Adapter` 에서 `inner class` 로 선언한 `View Holder` 에 `클릭리스너` 를 달아줬다. 클릭하면 해당 객체를 반환하도록 구현했다.

```kotlin
class RepositoryAdapter(val itemClickListener: (GithubRepository) -> Unit) :
    ListAdapter<GithubRepository, RepositoryViewHolder>(diffUtil) {
    inner class RepositoryViewHolder(private val binding: ItemRepositoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindData(repo: GithubRepository) = with(binding) {
            Glide.with(ownerProfileImageView.context)
                .load(repo.owner.avatarUrl)
                .into(ownerProfileImageView)

            ownerNameTextView.text = repo.owner.login
            nameTextView.text = repo.fullName
            subtextTextView.text = repo.description
            stargazersCountText.text = repo.stargazersCount.toString()
            repo.language?.let {
                languageText.isGone = false
                languageText.text = it
            } ?: kotlin.run {
                languageText.isGone = true
                languageText.text = ""
            }

        }

        fun bindViews(repo: GithubRepository) {
            binding.root.setOnClickListener {
                itemClickListener(repo)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepositoryViewHolder {
        return RepositoryViewHolder(
            ItemRepositoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RepositoryViewHolder, position: Int) {
        holder.bindData(currentList[position])
        holder.bindViews(currentList[position])
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<GithubRepository>() {
            override fun areItemsTheSame(
                oldItem: GithubRepository,
                newItem: GithubRepository
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: GithubRepository,
                newItem: GithubRepository
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}

```

이를 `액티비티` 에서 `인탠트` 를 통하여 `상세 정보 액티비티` 로 전환한다.

```kotlin
	private val adapter by lazy {
        RepositoryAdapter(itemClickListener = {
            val intent = Intent(this@SearchActivity, RepositoryActivity::class.java)
            intent.putExtra(RepositoryActivity.REPOSITORY_NAME_KEY, it.name)
            intent.putExtra(RepositoryActivity.REPOSITORY_OWNER_KEY, it.owner.login)
            startActivity(intent)
        })
    }

```

## 4. 좋아요한 레포지토리

좋아요 기능은 `Room` 을 활용했다. 로직은 아래와 같다.

1.  검색 이후 사용자가 해당 레포지토리를 확인했을 때 `Room DB` 에 해당 레포지토리가 존재하는지 확인힌다.
2.  해당 레포지토리가 존재하지 않는 경우 `좋아요(웃는 얼굴)` 을 표시하고 이미 존재하는 경우 `싫어요(우는 얼굴)` 을 표시한다.
3.  좋아요를 클릭하면 `Room DB` 에 저장하며 해당 아이콘을 싫어요로 변경한다.
4.  싫어요를 클릭하면 `Room DB` 에서 삭제하며 해당 아이콘을 좋아요로 변경한다.
