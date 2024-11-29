package com.example.activityrecognitionapp.di

import android.content.Context
import androidx.room.Room
import com.example.activityrecognitionapp.BuildConfig
import com.example.activityrecognitionapp.components.network.NetworkBannerManager
import com.example.activityrecognitionapp.data.bluetooth.AndroidBluetoothRepository
import com.example.activityrecognitionapp.data.bluetooth.BluetoothAdapterProvider
import com.example.activityrecognitionapp.data.network.NetworkConnectivityObserver
import com.example.activityrecognitionapp.data.network.SupabaseApiClient
import com.example.activityrecognitionapp.data.network.SupabaseApiService
import com.example.activityrecognitionapp.data.repository.ActivityDataDao
import com.example.activityrecognitionapp.data.repository.AppDatabase
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.SupabaseAuthRepositoryImpl
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.ActivityDataProcessor
import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import com.example.activityrecognitionapp.domain.usecases.ActivitiesData.FetchUserActivitiesUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.GetCurrentUserUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.IsSessionExpiredUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.LoginUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.LogoutUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.RefreshSessionUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.SignUpUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.ClearErrorMessageUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.ConnectToDeviceUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.DisconnectUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.EnableBluetoothUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.StartScanUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.StopScanUseCase
import dagger.Binds
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

    // Base URL for Supabase API
    val SUPABASE_URL = BuildConfig.supabaseUrl

    /**
     * Provides a singleton instance of BluetoothRepository.
     */
    @Provides
    @Singleton
    fun provideBluetoothRepository(
        @ApplicationContext context: Context,
        bluetoothAdapterProvider: BluetoothAdapterProvider,
    ): BluetoothRepository {
        return AndroidBluetoothRepository(context, bluetoothAdapterProvider)
    }

    /**
     * Provides a singleton instance of SupabaseApiService.
     */
    @Provides
    @Singleton
    fun provideSupabaseApiService(): SupabaseApiService {
        return SupabaseApiClient.apiService
    }

    /**
     * Provides a singleton instance of ActivityDataProcessor.
     */
    @Provides
    @Singleton
    fun provideActivityDataProcessor(): ActivityDataProcessor {
        return ActivityDataProcessor()
    }

    /**
     * Provides a singleton instance of DataRepository.
     */
    @Provides
    @Singleton
    fun provideDataRepository(
        supabaseApiService: SupabaseApiService,
        activityDataDao: ActivityDataDao,
        tokenRepository: TokenRepository,
        @ApplicationContext context: Context,
    ): DataRepository {
        return DataRepository(supabaseApiService, activityDataDao, tokenRepository, context)
    }

    /**
     * Provides a singleton instance of TokenRepository.
     */
    @Provides
    @Singleton
    fun provideTokenRepository(@ApplicationContext context: Context): TokenRepository {
        return TokenRepository(context)
    }

    /**
     * Provides the Room database instance.
     */
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Provides the ActivityDataDao from the Room database.
     */
    @Provides
    fun provideActivityDataDao(database: AppDatabase): ActivityDataDao {
        return database.activityDataDao()
    }

    /**
     * Provides a singleton instance of NetworkConnectivityObserver.
     */
    @Singleton
    @Provides
    fun provideConnectivityObserver(@ApplicationContext context: Context): NetworkConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }

    /**
     * Provides a singleton CoroutineScope for the application.
     */
    @Singleton
    @Provides
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    /**
     * Provides a singleton instance of NetworkBannerManager.
     */
    @Singleton
    @Provides
    fun provideNetworkBannerManager(
        connectivityObserver: NetworkConnectivityObserver,
        coroutineScope: CoroutineScope,
        repository: DataRepository,
    ): NetworkBannerManager {
        return NetworkBannerManager(connectivityObserver, repository, coroutineScope)
    }

    /**
     * Provides a singleton instance of LoginUseCase.
     */
    @Provides
    @Singleton
    fun provideLoginUseCase(
        supabaseAuthRepository: SupabaseAuthRepository,
    ): LoginUseCase {
        return LoginUseCase(supabaseAuthRepository)
    }

    /**
     * Provides a singleton instance of SignUpUseCase.
     */
    @Provides
    @Singleton
    fun provideSignUpUseCase(
        supabaseAuthRepository: SupabaseAuthRepository,
    ): SignUpUseCase {
        return SignUpUseCase(supabaseAuthRepository)
    }

    /**
     * Provides a singleton instance of LogoutUseCase.
     */
    @Provides
    @Singleton
    fun provideLogoutUseCase(
        supabaseAuthRepository: SupabaseAuthRepositoryImpl,
    ): LogoutUseCase {
        return LogoutUseCase(supabaseAuthRepository)
    }

    /**
     * Provides a singleton instance of RefreshSessionUseCase.
     */
    @Provides
    @Singleton
    fun provideRefreshSessionUseCase(
        supabaseAuthRepository: SupabaseAuthRepository,
    ): RefreshSessionUseCase {
        return RefreshSessionUseCase(supabaseAuthRepository)
    }

    /**
     * Provides a singleton instance of IsSessionExpiredUseCase.
     */
    @Provides
    @Singleton
    fun provideIsSessionExpiredUseCase(supabaseAuthRepository: SupabaseAuthRepository): IsSessionExpiredUseCase {
        return IsSessionExpiredUseCase(supabaseAuthRepository)
    }

    /**
     * Provides a singleton instance of GetCurrentUserUseCase.
     */
    @Provides
    @Singleton
    fun provideGetCurrentUserUseCase(supabaseAuthRepository: SupabaseAuthRepository): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(supabaseAuthRepository)
    }

    /**
     * Provides a singleton instance of StartScanUseCase.
     */
    @Provides
    @Singleton
    fun provideStartScanUseCase(
        repository: BluetoothRepository,
    ): StartScanUseCase = StartScanUseCase(repository)

    /**
     * Provides a singleton instance of StopScanUseCase.
     */
    @Provides
    @Singleton
    fun provideStopScanUseCase(
        repository: BluetoothRepository,
    ): StopScanUseCase = StopScanUseCase(repository)

    /**
     * Provides a singleton instance of ConnectToDeviceUseCase.
     */
    @Provides
    @Singleton
    fun provideConnectToDeviceUseCase(
        repository: BluetoothRepository,
    ): ConnectToDeviceUseCase = ConnectToDeviceUseCase(repository)

    /**
     * Provides a singleton instance of DisconnectUseCase.
     */
    @Provides
    @Singleton
    fun provideDisconnectUseCase(
        repository: BluetoothRepository,
    ): DisconnectUseCase = DisconnectUseCase(repository)

    /**
     * Provides a singleton instance of EnableBluetoothUseCase.
     */
    @Provides
    @Singleton
    fun provideEnableBluetoothUseCase(
        repository: BluetoothRepository,
    ): EnableBluetoothUseCase = EnableBluetoothUseCase(repository)

    /**
     * Provides a singleton instance of ClearErrorMessageUseCase.
     */
    @Provides
    @Singleton
    fun provideClearErrorMessageUseCase(
        repository: BluetoothRepository,
    ): ClearErrorMessageUseCase = ClearErrorMessageUseCase(repository)

    @Provides
    @Singleton
    fun provideFetchUserActivitiesUseCase(
        repository: DataRepository,
        activityDataProcessor: ActivityDataProcessor,
        tokenRepository: TokenRepository,
        connectivityObserver: NetworkConnectivityObserver,
    ): FetchUserActivitiesUseCase {
        return FetchUserActivitiesUseCase(repository,activityDataProcessor,tokenRepository,connectivityObserver
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds SupabaseAuthRepositoryImpl to SupabaseAuthRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindSupabaseAuthRepository(
        supabaseAuthRepositoryImpl: SupabaseAuthRepositoryImpl,
    ): SupabaseAuthRepository
}
