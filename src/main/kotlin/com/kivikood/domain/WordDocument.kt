package com.kivikood.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

// Data class representing a word document with a default constructor for Jackson
data class WordDocument @JsonCreator constructor(
    @JsonProperty("id") val id: String,
    @JsonProperty("word") val word: String,
    @JsonProperty("language") val language: String
)