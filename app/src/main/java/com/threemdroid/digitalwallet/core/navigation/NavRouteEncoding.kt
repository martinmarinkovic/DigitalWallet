package com.threemdroid.digitalwallet.core.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal fun encodeRouteValue(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")
