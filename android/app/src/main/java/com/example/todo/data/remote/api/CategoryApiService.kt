package com.example.todo.data.remote.api

import com.example.todo.data.remote.dto.category.CategoryEnvelopeDto
import com.example.todo.data.remote.dto.category.CategoryListEnvelopeDto
import com.example.todo.data.remote.dto.category.CreateCategoryRequestDto
import com.example.todo.data.remote.dto.category.UpdateCategoryRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

/** Retrofit interface cho `api/v1/categories` — path/method theo `category.controller.ts`. */
interface CategoryApiService {

    @GET("api/v1/categories")
    suspend fun getCategories(): Response<CategoryListEnvelopeDto>

    @POST("api/v1/categories")
    suspend fun createCategory(@Body body: CreateCategoryRequestDto): Response<CategoryEnvelopeDto>

    @PUT("api/v1/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body body: UpdateCategoryRequestDto
    ): Response<CategoryEnvelopeDto>

    @DELETE("api/v1/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<Void>
}
