package com.example.activityrecognitionapp.data.model

import kotlinx.datetime.Instant

/**
 * Represents a user session containing authentication tokens and expiration information.
 *
 * This data class holds the necessary information to manage a user's authenticated session,
 * including access and refresh tokens, as well as the session's expiration time.
 *
 * @property accessToken The token used to authenticate requests to protected resources.
 * @property refreshToken The token used to obtain a new access token when the current one expires.
 * @property expiresAt The exact timestamp when the current access token will expire.
 */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant
)