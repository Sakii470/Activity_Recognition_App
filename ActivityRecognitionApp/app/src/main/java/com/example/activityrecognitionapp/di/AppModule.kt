package com.example.activityrecognitionapp.di

//import io.github.jan.supabase.BuildConfig
//import io.github.jan.supabase.SupabaseClient

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
import com.example.activityrecognitionapp.domain.usecase.Authorization.GetCurrentUserUseCase
import com.example.activityrecognitionapp.domain.usecase.Authorization.IsSessionExpiredUseCase
import com.example.activityrecognitionapp.domain.usecase.Authorization.LoginUseCase
import com.example.activityrecognitionapp.domain.usecase.Authorization.LogoutUseCase
import com.example.activityrecognitionapp.domain.usecase.Authorization.RefreshSessionUseCase
import com.example.activityrecognitionapp.domain.usecase.Authorization.SignUpUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.ClearErrorMessageUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.ConnectToDeviceUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.DisconnectUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.EnableBluetoothUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.StartScanUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.StopScanUseCase
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

    val SUPABASE_URL = BuildConfig.supabaseUrl

    @Provides
    @Singleton
    fun provideBluetoothRepository(
        @ApplicationContext context: Context,
        bluetoothAdapterProvider: BluetoothAdapterProvider
    ): BluetoothRepository {
        return AndroidBluetoothRepository(context, bluetoothAdapterProvider)
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
    fun provideDataRepository(
        supabaseApiService: SupabaseApiService,
        activityDataDao: ActivityDataDao,
        tokenRepository: TokenRepository,
        @ApplicationContext context: Context
    ): DataRepository {
        return DataRepository(supabaseApiService, activityDataDao, tokenRepository, context)
    }

    @Provides
    @Singleton
    fun provideTokenRepository(@ApplicationContext context: Context): TokenRepository {
        return TokenRepository(context)
    }

    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
            .fallbackToDestructiveMigration()
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
        repository: DataRepository
    ): NetworkBannerManager {
        return NetworkBannerManager(connectivityObserver, repository, coroutineScope)
    }

    @Provides
    @Singleton
    fun provideLoginUseCase(
        supabaseAuthRepository: SupabaseAuthRepository
    ): LoginUseCase {
        return LoginUseCase(supabaseAuthRepository)
    }

    @Provides
    @Singleton
    fun provideSignUpUseCase(
        supabaseAuthRepository: SupabaseAuthRepository
    ): SignUpUseCase {
        return SignUpUseCase(supabaseAuthRepository)
    }

    @Provides
    @Singleton
    fun provideLogoutUseCase(
        supabaseAuthRepository: SupabaseAuthRepositoryImpl
    ): LogoutUseCase {
        return LogoutUseCase(supabaseAuthRepository)
    }

    @Provides
    @Singleton
    fun provideRefreshSessionUseCase(
        supabaseAuthRepository: SupabaseAuthRepository
    ): RefreshSessionUseCase {
        return RefreshSessionUseCase(supabaseAuthRepository)
    }

    @Provides
    @Singleton
    fun provideIsSessionExpiredUseCase(supabaseAuthRepository: SupabaseAuthRepository): IsSessionExpiredUseCase {
        return IsSessionExpiredUseCase(supabaseAuthRepository)
    }

    @Provides
    @Singleton
    fun provideGetCurrentUserUseCase(supabaseAuthRepository: SupabaseAuthRepository): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(supabaseAuthRepository)
    }

    @Provides
    @Singleton
    fun provideStartScanUseCase(
        repository: BluetoothRepository
    ): StartScanUseCase = StartScanUseCase(repository)

    @Provides
    @Singleton
    fun provideStopScanUseCase(
        repository: BluetoothRepository
    ): StopScanUseCase = StopScanUseCase(repository)

    @Provides
    @Singleton
    fun provideConnectToDeviceUseCase(
        repository: BluetoothRepository
    ): ConnectToDeviceUseCase = ConnectToDeviceUseCase(repository)

    @Provides
    @Singleton
    fun provideDisconnectUseCase(
        repository: BluetoothRepository
    ): DisconnectUseCase = DisconnectUseCase(repository)

    @Provides
    @Singleton
    fun provideEnableBluetoothUseCase(
        repository: BluetoothRepository
    ): EnableBluetoothUseCase = EnableBluetoothUseCase(repository)

    @Provides
    @Singleton
    fun provideClearErrorMessageUseCase(
        repository: BluetoothRepository
    ): ClearErrorMessageUseCase = ClearErrorMessageUseCase(repository)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSupabaseAuthRepository(
        supabaseAuthRepositoryImpl: SupabaseAuthRepositoryImpl
    ): SupabaseAuthRepository
}


//    val SUPABASE_URL = BuildConfig.supabaseUrl
//
//    @Provides
//    @Singleton
//    fun provideBluetoothRepository(
//        @ApplicationContext context: Context,
//        bluetoothAdapterProvider: BluetoothAdapterProvider
//    ): BluetoothRepository {
//        return AndroidBluetoothRepository(context, bluetoothAdapterProvider)
//    }
//
//    @Provides
//    @Singleton
//    fun provideSupabaseApiService(): SupabaseApiService {
//        return SupabaseApiClient.apiService
//    }
//
//    @Provides
//    @Singleton
//    fun provideActivityDataProcessor(): ActivityDataProcessor {
//        return ActivityDataProcessor()
//    }
//
//    @Provides
//    @Singleton
//    fun provideDataRepository(
//        supabaseApiService: SupabaseApiService,
//        activityDataDao: ActivityDataDao,
//        tokenRepository: TokenRepository,
//        @ApplicationContext context: Context
//    ): DataRepository {
//        return DataRepository(supabaseApiService, activityDataDao, tokenRepository, context)
//    }
//
//    @Provides
//    @Singleton
//    fun provideTokenRepository(@ApplicationContext context: Context): TokenRepository {
//        return TokenRepository(context)
//    }
//
//    @Provides
//    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
//        return Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
//            .fallbackToDestructiveMigration()
//            .build()
//    }
//
//    @Provides
//    fun provideActivityDataDao(database: AppDatabase): ActivityDataDao {
//        return database.activityDataDao()
//    }
//
//    @Singleton
//    @Provides
//    fun provideConnectivityObserver(@ApplicationContext context: Context): NetworkConnectivityObserver {
//        return NetworkConnectivityObserver(context)
//    }
//
//    @Singleton
//    @Provides
//    fun provideCoroutineScope(): CoroutineScope {
//        return CoroutineScope(Dispatchers.Main + SupervisorJob())
//    }
//
//    @Singleton
//    @Provides
//    fun provideNetworkBannerManager(
//        connectivityObserver: NetworkConnectivityObserver,
//        coroutineScope: CoroutineScope,
//        repository: DataRepository // Dodaj repository, jeśli używasz
//    ): NetworkBannerManager {
//        return NetworkBannerManager(connectivityObserver, repository, coroutineScope)
//    }
//
//    @Provides
//    @Singleton
//    fun provideLoginUseCase(
//        SupabaseAuthRepository: SupabaseAuthRepository
//    ): LoginUseCase {return LoginUseCase(SupabaseAuthRepository)
//    }
//
//    @Provides
//    @Singleton
//    fun provideSignUpUseCase(
//        supabaseAuthRepository: SupabaseAuthRepository
//    ): SignUpUseCase {
//        return SignUpUseCase(supabaseAuthRepository)
//    }
//
//    @Provides
//    @Singleton
//    fun provideLogoutUseCase(
//        supabaseAuthRepository: SupabaseAuthRepositoryImpl
//    ): LogoutUseCase {
//        return LogoutUseCase(supabaseAuthRepository)
//    }
//
//    @Provides
//    @Singleton
//    fun provideRefreshSessionUseCase(
//        supabaseAuthRepository: SupabaseAuthRepository
//    ): RefreshSessionUseCase {
//        return RefreshSessionUseCase(supabaseAuthRepository)
//    }
//
//    @Provides
//    @Singleton
//    fun provideIsSessionExpiredUseCase(supabaseAuthRepository: SupabaseAuthRepository): IsSessionExpiredUseCase {
//        return IsSessionExpiredUseCase(supabaseAuthRepository)
//    }
//
//    @Provides
//    @Singleton
//    fun provideGetCurrentUserUseCase(supabaseAuthRepository: SupabaseAuthRepository): GetCurrentUserUseCase {
//        return GetCurrentUserUseCase(supabaseAuthRepository)
//    }
//
//    @Provides
//    @Singleton
//    fun provideStartScanUseCase(
//        repository: BluetoothRepository
//    ): StartScanUseCase = StartScanUseCase(repository)
//
//    @Provides
//    @Singleton
//    fun provideStopScanUseCase(
//        repository: BluetoothRepository
//    ): StopScanUseCase = StopScanUseCase(repository)
//
//    @Provides
//    @Singleton
//    fun provideConnectToDeviceUseCase(
//        repository: BluetoothRepository
//    ): ConnectToDeviceUseCase = ConnectToDeviceUseCase(repository)
//
//    @Provides
//    @Singleton
//    fun provideDisconnectUseCase(
//        repository: BluetoothRepository
//    ): DisconnectUseCase = DisconnectUseCase(repository)
//
//    @Provides
//    @Singleton
//    fun provideEnableBluetoothUseCase(
//        repository: BluetoothRepository
//    ): EnableBluetoothUseCase = EnableBluetoothUseCase(repository)
//
//    @Provides
//    @Singleton
//    fun provideClearErrorMessageUseCase(
//        repository: BluetoothRepository
//    ): ClearErrorMessageUseCase = ClearErrorMessageUseCase(repository)
//}
//
//
//    @Module
//    @InstallIn(SingletonComponent::class)
//    abstract class RepositoryModule {
//        @Binds
//        @Singleton
//        abstract fun bindSupabaseAuthRepository(
//            supabaseAuthRepository: SupabaseAuthRepositoryImpl
//        ): SupabaseAuthRepository
//
//        @Binds
//        @Singleton
//        abstract fun bindBluetoothRepository(
//            bluetoothRepository: AndroidBluetoothRepository
//        ):BluetoothRepository
//
//    }







