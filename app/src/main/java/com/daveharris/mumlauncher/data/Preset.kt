package com.daveharris.mumlauncher.data

data class Preset(
    val id: Long = 0,
    val name: String,
    val apps: List<String> = emptyList(),
)
