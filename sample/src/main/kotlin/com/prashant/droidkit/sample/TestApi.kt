package com.prashant.droidkit.sample

import com.prashant.droidkit.DroidKit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val category: String,
    val inStock: Boolean
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String
)

data class Cart(
    val id: Int,
    val userId: Int,
    val products: List<Map<String, Any>>,
    val total: Double,
    val totalProducts: Int
)

data class Post(
    val id: Int,
    val title: String,
    val body: String,
    val userId: Int
)

data class Comment(
    val id: Int,
    val body: String,
    val postId: Int,
    val userId: Int
)

interface TestApi {
    @GET("products")
    suspend fun getProducts(@Query("limit") limit: Int = 10): Map<String, Any>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): Map<String, Any>

    @GET("products/search")
    suspend fun searchProducts(@Query("q") query: String): Map<String, Any>

    @GET("users")
    suspend fun getUsers(): Map<String, Any>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): Map<String, Any>

    @GET("carts")
    suspend fun getCarts(): Map<String, Any>

    @GET("carts/{id}")
    suspend fun getCart(@Path("id") id: Int): Map<String, Any>

    @POST("carts/add")
    suspend fun addCart(@Body cart: Map<String, Any>): Map<String, Any>

    @PUT("carts/{id}")
    suspend fun updateCart(@Path("id") id: Int, @Body cart: Map<String, Any>): Map<String, Any>

    @DELETE("carts/{id}")
    suspend fun deleteCart(@Path("id") id: Int): Map<String, Any>

    @GET("posts")
    suspend fun getPosts(@Query("limit") limit: Int = 10): Map<String, Any>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): Map<String, Any>

    @POST("posts/add")
    suspend fun addPost(@Body post: Map<String, Any>): Map<String, Any>

    @GET("comments")
    suspend fun getComments(@Query("limit") limit: Int = 5): Map<String, Any>

    companion object {
        fun create(): TestApi {
            val client = OkHttpClient.Builder()
                .addInterceptor(DroidKit.networkInterceptor())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://dummyjson.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(TestApi::class.java)
        }
    }
}
