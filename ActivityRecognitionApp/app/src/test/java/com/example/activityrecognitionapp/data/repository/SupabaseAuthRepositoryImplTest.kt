package com.example.activityrecognitionapp.data.repository

import com.example.activityrecognitionapp.data.network.SupabaseApiClient
import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class SupabaseAuthRepositoryImplTest {

    // Mocked dependencies
    private lateinit var tokenRepository: TokenRepository
    private lateinit var dataRepository: DataRepository
    private lateinit var bluetoothRepository: BluetoothRepository
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var goTrue: GoTrue

    // Class under test
    private lateinit var authRepository: SupabaseAuthRepositoryImpl

    companion object {
        private lateinit var mockSupabaseClient: SupabaseClient
        private lateinit var mockGoTrue: GoTrue

        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            // Mock the top-level createSupabaseClient function
            mockkStatic("com.example.activityrecognitionapp.data.network.SupabaseApiClientKt")

            // Create mocked SupabaseClient
            mockSupabaseClient = mockk(relaxed = true)

            // Create mocked GoTrue
            mockGoTrue = mockk(relaxed = true)

            // Mock gotrue property in mockSupabaseClient
            every { mockSupabaseClient.gotrue } returns mockGoTrue

            // Mock the createSupabaseClient to return mockSupabaseClient
            every {
                createSupabaseClient(
                    any(),
                    any(),
                    any<SupabaseClientBuilder.() -> Unit>()
                )
            } returns mockSupabaseClient

            // Mock SettingsSessionManager to avoid initialization errors
            mockkObject(io.github.jan.supabase.gotrue.SettingsSessionManager::class)
            every { io.github.jan.supabase.gotrue.SettingsSessionManager(any()) } returns mockk(relaxed = true)
        }
    }

    @Before
    fun setUp() {
        // Initialize MockK annotations
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Initialize mocked dependencies
        tokenRepository = mockk(relaxed = true)
        dataRepository = mockk(relaxed = true)
        bluetoothRepository = mockk(relaxed = true)

        // Use mocked SupabaseClient and GoTrue
        supabaseClient = SupabaseApiClient.SupabaseClient.Client
        goTrue = supabaseClient.gotrue

        // Initialize the repository with mocked dependencies
        authRepository = spyk(
            SupabaseAuthRepositoryImpl(tokenRepository, dataRepository, bluetoothRepository),
            recordPrivateCalls = true
        )
    }

    @Test
    fun `login - success`() = runBlocking {
        // Test data
        val email = "test@example.com"
        val password = "password123"

        // Configure mock for loginWith
        coEvery { goTrue.loginWith(Email, any()) } returns Unit

        // Mock private methods
        coEvery { authRepository["saveSession"]() } returns Unit
        coEvery { authRepository["saveUserNameFromMetadata"]() } returns Unit

        // Call login method
        val result = authRepository.login(email, password)

        // Check that result is success
        assertTrue(result.isSuccess)

        // Verify that loginWith was called with correct parameters
        coVerify(exactly = 1) {
            goTrue.loginWith(Email, any())
        }

        // Verify that private methods were called
        coVerify(exactly = 1) { authRepository["saveSession"]() }
        coVerify(exactly = 1) { authRepository["saveUserNameFromMetadata"]() }
    }
}
