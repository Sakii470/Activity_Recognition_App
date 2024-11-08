package com.example.activityrecognitionapp.di

import android.content.Context
import com.example.activityrecognitionapp.BuildConfig
import com.example.activityrecognitionapp.data.bluetooth.AndroidBluetoothController
import com.example.activityrecognitionapp.data.network.SupabaseApiClient
import com.example.activityrecognitionapp.data.repository.DataRepository
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

    @Provides
    @Singleton
    fun provideBluetoothController(@ApplicationContext context: Context): BluetoothController {
        return AndroidBluetoothController(context)
    }


    @Provides
    @Singleton
    fun provideDataRepository(): DataRepository {
        return DataRepository(supabaseApiService = supa)
    }

}