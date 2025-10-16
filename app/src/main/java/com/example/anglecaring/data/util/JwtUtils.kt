package com.example.anglecaring.data.util

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets

/**
 * Utility class for JWT token operations
 */
object JwtUtils {
    private const val TAG = "JwtUtils"
    
    /**
     * Decode a JWT token and extract the payload as a JsonObject
     * 
     * @param token The JWT token to decode
     * @return The decoded payload as a JsonObject, or null if decoding fails
     */
    fun decodeToken(token: String): JsonObject? {
        try {
            // JWT token has three parts: header.payload.signature
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e(TAG, "Invalid JWT token format")
                return null
            }
            
            // Base64Url decode the payload (second part)
            val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE)
            val payload = String(payloadBytes, StandardCharsets.UTF_8)
            
            Log.d(TAG, "Decoded JWT payload: $payload")
            
            // Parse the JSON payload
            return JsonParser.parseString(payload).asJsonObject
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding JWT token", e)
            return null
        }
    }
    
    /**
     * Extract user ID from JWT token
     * 
     * @param token The JWT token
     * @return The user ID, or null if not found
     */
    fun extractUserId(token: String): Int? {
        val payload = decodeToken(token) ?: return null
        
        try {
            // Try to find the user ID in common JWT claim fields
            return when {
                payload.has("id") -> payload.get("id").asInt
                payload.has("sub") -> payload.get("sub").asString.toIntOrNull()
                payload.has("userId") -> payload.get("userId").asInt
                payload.has("user_id") -> payload.get("user_id").asInt
                else -> {
                    // Log all available fields for debugging
                    Log.e(TAG, "JWT token doesn't contain a recognized user ID field. Available fields: ${payload.keySet()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting user ID from JWT payload", e)
            return null
        }
    }
    
    /**
     * Extract email from JWT token
     * 
     * @param token The JWT token
     * @return The email, or null if not found
     */
    fun extractEmail(token: String): String? {
        val payload = decodeToken(token) ?: return null
        
        try {
            return when {
                payload.has("email") -> payload.get("email").asString
                else -> {
                    Log.d(TAG, "No email field found in JWT token")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting email from JWT payload", e)
            return null
        }
    }
    
    /**
     * Extract user name from JWT token
     * 
     * @param token The JWT token
     * @return The user name, or null if not found
     */
    fun extractUserName(token: String): String? {
        val payload = decodeToken(token) ?: return null
        
        try {
            // Check multiple possible field names for userName, prioritizing DB field name
            return when {
                payload.has("user_name") -> {
                    val name = payload.get("user_name").asString
                    Log.d(TAG, "Found user_name in token: $name")
                    name
                }
                payload.has("userName") -> {
                    val name = payload.get("userName").asString
                    Log.d(TAG, "Found userName in token: $name")
                    name
                }
                payload.has("username") -> {
                    val name = payload.get("username").asString
                    Log.d(TAG, "Found username in token: $name")
                    name
                }
                payload.has("name") -> {
                    val name = payload.get("name").asString
                    Log.d(TAG, "Found name in token: $name")
                    name
                }
                else -> {
                    // Log all available fields for debugging
                    Log.d(TAG, "No username field found in JWT token. Available fields: ${payload.keySet()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting username from JWT payload", e)
            return null
        }
    }
} 