package com.daveharris.mumlauncher.data

data class Contact(
    val id: Long = 0,
    val displayName: String,
    val phoneNumber: String,
    val callable: Boolean = true,
    val messageable: Boolean = true,
    val sortOrder: Int = 0,
)
