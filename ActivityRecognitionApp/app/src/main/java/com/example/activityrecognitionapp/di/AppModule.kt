package com.example.activityrecognitionapp.di

//import io.github.jan.supabase.BuildConfig
//import io.github.jan.supabase.SupabaseClient

import android.content.Context
import androidx.room.Room
import com.example.activityrecognitionapp.BuildConfig
import com.example.activityrecognitionapp.components.NetworkBannerManager
import com.example.activityrecognitionapp.data.bluetooth.AndroidBluetoothController
import com.example.activityrecognitionapp.data.network.NetworkConnectivityObserver
import com.example.activityrecognitionapp.data.network.SupabaseApiClient
import com.example.activityrecognitionapp.data.network.SupabaseApiService
import com.example.activityrecognitionapp.data.repository.ActivityDataDao
import com.example.activityrecognitionapp.data.repository.AppDatabase
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.ActivityDataProcessor
import com.example.activityrecognitionapp.domain.BluetoothController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    fun provideDataRepository(supabaseApiService: SupabaseApiService, activityDataDao: ActivityDataDao
    , tokenRepository: TokenRepository, @ApplicationContext context: Context): DataRepository {
        return DataRepository(supabaseApiService, activityDataDao, tokenRepository,context)
    }

    @Provides
    @Singleton
    fun provideTokenRepository(@ApplicationContext context: Context): TokenRepository {
        return TokenRepository(context)
    }

    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase{
        return Room.databaseBuilder(context, AppDatabase::class.java, "app_database").fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideActivityDataDao(database: AppDatabase): ActivityDataDao {
        return database.activityDataDao()
    }

    @Singleton
    @Provides
    fun provideConnectivityObserver(@ApplicationContext context: Context): NetworkConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }

    @Singleton
    @Provides
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    @Singleton
    @Provides
    fun provideNetworkBannerManager(
        connectivityObserver: NetworkConnectivityObserver,
        coroutineScope: CoroutineScope,
        repository: DataRepository // Dodaj repository, jeśli używasz
    ): NetworkBannerManager {
        return NetworkBannerManager(connectivityObserver, repository,coroutineScope)
    }


    }

