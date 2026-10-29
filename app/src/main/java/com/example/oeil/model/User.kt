package com.example.oeil.model

/** Signed-in account. [token] is whatever the login API returned, if anything. */
data class User(
    val id: String,
    val email: String,
    val name: String,
    val token: String? = null
)
