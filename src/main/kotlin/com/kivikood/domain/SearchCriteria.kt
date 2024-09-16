package com.kivikood.domain

import kotlinx.serialization.Serializable

@Serializable
data class SearchCriteria(
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val characterSet: Set<Char>? = null,
    val obligatoryCharacters: Set<Char>? = null,
    val startWith: Char? = null,
    val endWith: Char? = null,
    val startingIndex: Int? = null,
    val language: String? = null
)
