package com.example.activityrecognitionapp.di

//import io.github.jan.supabase.BuildConfig
//import io.github.jan.supabase.SupabaseClient
import android.content.Context
import com.example.activityrecognitionapp.BuildConfig
import com.example.activityrecognitionapp.data.bluetooth.AndroidBluetoothController
import com.example.activityrecognitionapp.data.network.SupabaseApiClient
import com.example.activityrecognitionapp.data.network.SupabaseApiService
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.ActivityDataProcessor
import com.example.activityrecognitionapp.domain.BluetoothController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

     val SUPABASE_URL = BuildConfig.supabaseUrl

    @Provides
    @Singleton
    fun provideBluetoothController(@ApplicationContext context: Context): BluetoothController {
        return AndroidBluetoothController(context)
    }

    @Provides
    @Singleton
    fun provideSupabaseApiService(): SupabaseApiService {
        return SupabaseApiClient.apiService
    }

    @Provides
    @Singleton
    fun provideActivityDataProcessor(): ActivityDataProcessor {
        return ActivityDataProcessor()
    }

    @Provides
    @Singleton
    fun provideDataRepository(supabaseApiService: SupabaseApiService): DataRepository {
        return DataRepository(supabaseApiService)
    }

    @Provides
    @Singleton
    fun provideTokenRepository(@ApplicationContext context: Context): TokenRepository {
        return TokenRepository(context) // Załóżmy, że TokenRepository nie potrzebuje dodatkowej konfiguracji
    }










    }

