package com.prashant.droidkit.sample

import com.prashant.droidkit.DroidKit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)

data class User(
    val id: Int,
    val name: String,
    val email: String
)

interface TestApi {
    @GET("posts")
    suspend fun getPosts(): List<Post>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): Post

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): User

    @POST("posts")
    suspend fun createPost(@Body post: Post): Post

    companion object {
        fun create(): TestApi {
            val client = OkHttpClient.Builder()
                .addInterceptor(DroidKit.networkInterceptor())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://jsonplaceholder.typicode.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(TestApi::class.java)
        }
    }
}
