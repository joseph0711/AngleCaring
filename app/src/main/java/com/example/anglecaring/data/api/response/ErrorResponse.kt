package com.example.anglecaring.data.api.response

import com.google.gson.annotations.SerializedName

/**
 * 通用錯誤響應數據類
 * 用於解析API錯誤響應
 */
data class ErrorResponse(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("message")
    val message: String? = null
)
