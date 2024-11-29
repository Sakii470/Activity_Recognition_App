package com.example.activityrecognitionapp.presentation.viewmodels

import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import com.example.activityrecognitionapp.domain.usecases.Authorization.GetCurrentUserUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.IsSessionExpiredUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.LoginUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.LogoutUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.RefreshSessionUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.SignUpUseCase
import com.example.activityrecognitionapp.presentation.states.LoginUiState
import com.example.activityrecognitionapp.presentation.states.UserState
import com.example.activityrecognitionapp.utils.Event
import com.example.activityrecognitionapp.utils.EventBus
import io.github.jan.supabase.gotrue.user.UserInfo
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkObject
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class SupabaseAuthViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var tokenRepository: TokenRepository

    @Mock
    private lateinit var dataRepository: DataRepository

    @Mock
    private lateinit var bluetoothRepository: BluetoothRepository

    @Mock
    private lateinit var loginUseCase: LoginUseCase

    @Mock
    private lateinit var signUpUseCase: SignUpUseCase

    @Mock
    private lateinit var logoutUseCase: LogoutUseCase

    @Mock
    private lateinit var refreshSessionUseCase: RefreshSessionUseCase

    @Mock
    private lateinit var isSessionExpiredUseCase: IsSessionExpiredUseCase

    @Mock
    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    private lateinit var viewModel: SupabaseAuthViewModel

//    @Mock
//    private lateinit var eventBus: EventBus

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SupabaseAuthViewModel(
            tokenRepository = tokenRepository,
            dataRepository = dataRepository,
            bluetoothRepository = bluetoothRepository,
            loginUseCase = loginUseCase,
            signUpUseCase = signUpUseCase,
            logoutUseCase = logoutUseCase,
            refreshSessionUseCase = refreshSessionUseCase,
            isSessionExpiredUseCase = isSessionExpiredUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase
        )
    }

    @Test
    fun `login success updates uiLoginState correctly`() = runTest {
        // Arrange
        val email = "test@test.com"
        val password = "123456"

        `when`(loginUseCase.invoke(email, password)).thenReturn(Result.success(Unit))

        val userInfoMock = mock(UserInfo::class.java)
        val userMetadata = buildJsonObject {
            put("display_name", JsonPrimitive("Test User"))
        }
        `when`(userInfoMock.userMetadata).thenReturn(userMetadata)
        `when`(getCurrentUserUseCase.invoke()).thenReturn(userInfoMock)

        // Act
        viewModel.login(email, password)
        advanceUntilIdle() // Upewnia się, że wszystkie korutyny się zakończyły

        // Assert
        assertEquals(true, viewModel.uiLoginState.value.isLoggedIn)
        assertEquals("Test User", viewModel.uiLoginState.value.userName)
    }

    @Test
    fun `login failure updates uiLoginState with error`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "wrongpassword"

        val exception = Exception("Invalid credentials")
        `when`(loginUseCase.invoke(email, password)).thenReturn(Result.failure(exception))

        // Act
        viewModel.login(email, password)

        // Upewnij się, że korutyny zostały wykonane
        advanceUntilIdle()

        // Assert
        val expectedState = LoginUiState(
            userState = UserState.Error("Invalid credentials"),
            isLoggedIn = false
        )
        assertEquals(expectedState, viewModel.uiLoginState.value)
    }

    @Test
    fun `login with empty email and password updates uiLoginState with error`() = runTest {
        // Arrange
        val email = ""
        val password = ""

        val exception = Exception("missing email or phone")
        `when`(loginUseCase.invoke(email, password)).thenReturn(Result.failure(exception))

        // Act
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert
        val expectedState = LoginUiState(
            userState = UserState.Error("missing email or phone"),
            isLoggedIn = false
        )
        assertEquals(expectedState, viewModel.uiLoginState.value)
    }

    @Test
    fun `login with invalid email format updates uiLoginState with error`() = runTest {
        // Arrange
        val email = "invalidEmail"
        val password = "123456"

        val exception = Exception("Invalid email format")
        `when`(loginUseCase.invoke(email, password)).thenReturn(Result.failure(exception))

        // Act
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert
        val expectedState = LoginUiState(
            userState = UserState.Error("Invalid email format"),
            isLoggedIn = false
        )
        assertEquals(expectedState, viewModel.uiLoginState.value)
    }

    @Test
    fun `login with correct email but incorrect password updates uiLoginState with error`() = runTest {
        // Arrange
        val email = "test@test.com"
        val password = "wrongpassword"

        val exception = Exception("Authentication failed")
        `when`(loginUseCase.invoke(email, password)).thenReturn(Result.failure(exception))

        // Act
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert
        val expectedState = LoginUiState(
            userState = UserState.Error("Authentication failed"),
            isLoggedIn = false
        )
        assertEquals(expectedState, viewModel.uiLoginState.value)
    }

    @Test
    fun `login with correct email but empty password updates uiLoginState with error`() = runTest {
        // Arrange
        val email = "test@test.com"
        val password = ""

        val exception = Exception("Password cannot be empty")
        `when`(loginUseCase.invoke(email, password)).thenReturn(Result.failure(exception))

        // Act
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert
        val expectedState = LoginUiState(
            userState = UserState.Error("Password cannot be empty"),
            isLoggedIn = false
        )
        assertEquals(expectedState, viewModel.uiLoginState.value)
    }

    @Test
    fun `login with server error updates uiLoginState with error`() = runTest {
        // Arrange
        val email = "test@test.com"
        val password = "123456"

        val exception = Exception("Server error")
        `when`(loginUseCase.invoke(email, password)).thenReturn(Result.failure(exception))

        // Act
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert
        val expectedState = LoginUiState(
            userState = UserState.Error("Server error"),
            isLoggedIn = false
        )
        assertEquals(expectedState, viewModel.uiLoginState.value)
    }

    /**
     * logout() test
     *
     *
     */

    @Test
    fun `logout success updates uiLoginState correctly and sends logout event`() = runTest {
        // Arrange
        mockkObject(EventBus)
        `when`(logoutUseCase.invoke()).thenReturn(Result.success(Unit))
        coEvery { EventBus.sendEvent(Event.Logout) } just Runs // Mockowanie funkcji zawieszenia

        // Act
        viewModel.logout()
        advanceUntilIdle()

        // Assert
        assertEquals(
            LoginUiState(
                userState = UserState.Success("Logged out successfully"),
                isLoggedIn = false
            ),
            viewModel.uiLoginState.value
        )

            coVerify { EventBus.sendEvent(Event.Logout) }

        // Oczyszczanie po teście
        unmockkObject(EventBus)
    }

    @Test
    fun `logout failure updates uiLoginState with offline message`() = runTest {
        // Arrange
        val exception = Exception("Network error")
        mockkObject(EventBus)
        `when`(logoutUseCase.invoke()).thenReturn(Result.failure(exception))
        coEvery { EventBus.sendEvent(Event.Logout) } just Runs // Mockowanie funkcji zawieszenia

        // Act
        viewModel.logout()
        advanceUntilIdle()

        // Assert
        assertEquals(
            LoginUiState(
                userState = UserState.Success("Logged out offline"),
                isLoggedIn = false
            ),
            viewModel.uiLoginState.value
        )
        // Weryfikacja, że zdarzenie nie zostało wysłane
        coVerify(exactly = 0) { EventBus.sendEvent(Event.Logout) }
        // Oczyszczanie po teście
        unmockkObject(EventBus)
    }

    @Test
    fun `logout sets isLoggedIn to false regardless of result`() = runTest {
        // Arrange
        `when`(logoutUseCase.invoke()).thenReturn(Result.failure(Exception("Error")))

        // Act
        viewModel.logout()
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.uiLoginState.value.isLoggedIn)
    }

    @Test
    fun `logout success displays correct user message`() = runTest {
        // Arrange
        `when`(logoutUseCase.invoke()).thenReturn(Result.success(Unit))

        // Act
        viewModel.logout()
        advanceUntilIdle()

        // Assert
        assertEquals(
            UserState.Success("Logged out successfully"),
            viewModel.uiLoginState.value.userState
        )
    }

    @Test
    fun `logout failure displays offline logout message`() = runTest {
        // Arrange
        `when`(logoutUseCase.invoke()).thenReturn(Result.failure(Exception("Error")))

        // Act
        viewModel.logout()
        advanceUntilIdle()

        // Assert
        assertEquals(
            UserState.Success("Logged out offline"),
            viewModel.uiLoginState.value.userState
        )
    }

    @Test
    fun `exception during logout is handled gracefully`() = runTest {
        // Arrange
        `when`(logoutUseCase.invoke()).thenThrow(RuntimeException("Unexpected error"))

        // Act
        viewModel.logout()
        advanceUntilIdle()

        // Assert
        assertEquals(
            LoginUiState(
                userState = UserState.Success("Logged out offline"),
                isLoggedIn = false
            ),
            viewModel.uiLoginState.value
        )
    }

    @Test
    fun `logout failure does not send logout event`() = runTest {
        // Arrange
        mockkObject(EventBus)
        `when`(logoutUseCase.invoke()).thenReturn(Result.failure(Exception("Error")))
        coEvery { EventBus.sendEvent(Event.Logout) } just Runs // Mockowanie funkcji zawieszenia


        // Act
        viewModel.logout()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { EventBus.sendEvent(Event.Logout) }
    }

    /**
     * checkSessionExpired() test
     *
     *
     */

    @Test
    fun `checkSessionExpired when session is expired updates uiLoginState with error`() = runTest {
        // Arrange
        `when`(isSessionExpiredUseCase.invoke()).thenReturn(true)

        // Act
        viewModel.checkSessionExpired()
        advanceUntilIdle()

        // Assert
        assertEquals(
            LoginUiState(
                userState = UserState.Error("Session Expired"),
                isLoggedIn = false
            ),
            viewModel.uiLoginState.value
        )
    }
    @Test
    fun `checkSessionExpired when session is not expired and refresh succeeds updates uiLoginState`() = runTest {
        // Arrange
        `when`(isSessionExpiredUseCase.invoke()).thenReturn(false)
        `when`(refreshSessionUseCase.invoke()).thenReturn(Result.success(Unit))
        val userInfoMock = mock(UserInfo::class.java)
        val userMetadata = buildJsonObject {
            put("display_name", JsonPrimitive("Test User"))
        }
        `when`(userInfoMock.userMetadata).thenReturn(userMetadata)
        `when`(getCurrentUserUseCase.invoke()).thenReturn(userInfoMock)

        // Act
        viewModel.checkSessionExpired()
        advanceUntilIdle()

        // Assert
        assertEquals(
            LoginUiState(
                userState = UserState.Success("Session refreshed successfully"),
                isLoggedIn = true,
                userName = "Test User"
            ),
            viewModel.uiLoginState.value
        )
    }

    @Test
    fun `checkSessionExpired when session is not expired but refresh fails updates uiLoginState with error`() = runTest {
        // Arrange
        `when`(isSessionExpiredUseCase.invoke()).thenReturn(false)
        val exception = Exception("Refresh token invalid")
        `when`(refreshSessionUseCase.invoke()).thenReturn(Result.failure(exception))

        // Act
        viewModel.checkSessionExpired()
        advanceUntilIdle()

        // Assert
        assertEquals(
            LoginUiState(
                userState = UserState.Error("Refresh token invalid"),
                isLoggedIn = false
            ),
            viewModel.uiLoginState.value
        )
    }

}




