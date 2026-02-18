package com.example.eastsyria.SignUp.Data

data class User(val uid: String = "",
                val fullName: String = "",
                val email: String = "",
                val phoneNumber: String = "",
                val createdAt: Long = System.currentTimeMillis(),
                val isVerified: Boolean = false,
                val city: String=""
) {
    constructor() : this("", "", "", "", 0L, false)
}
