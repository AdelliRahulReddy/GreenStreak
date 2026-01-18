package com.rahul.githubwallpaper.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface GitHubApi {
    @Headers("Content-Type: application/json")
    @POST("graphql")
    suspend fun executeQuery(
        @Header("Authorization") token: String,
        @Body request: String
    ): Response<String>
}
