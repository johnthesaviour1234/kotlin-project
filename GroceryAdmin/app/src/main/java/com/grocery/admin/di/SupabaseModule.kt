package com.grocery.admin.di

import android.content.Context
import com.grocery.admin.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import javax.inject.Singleton

/**
 * Supabase Module for Dependency Injection - Admin App
 * 
 * Provides SupabaseClient singleton configured with:
 * - Authentication (Auth)
 * - Database (Postgrest)
 * - Realtime (WebSocket) for admin-wide real-time updates
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(WebSockets)
            
            engine {
                config {
                    // OkHttp respects Android's network security config
                    // This allows WebSocket connections with proper TLS
                    connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideSupabaseClient(
        httpClient: HttpClient
    ): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // Install Auth module for authentication
            install(Auth)
            
            // Install Postgrest for database queries
            install(Postgrest)
            
            // Install Storage for file uploads
            install(Storage)
            
            // Install Realtime for WebSocket-based real-time sync
            install(Realtime) {
                // Realtime configuration
                // Automatically reconnects on connection loss
            }
            
            // Use custom HTTP client with WebSocket support
            httpEngine = httpClient.engine
        }
    }
}
