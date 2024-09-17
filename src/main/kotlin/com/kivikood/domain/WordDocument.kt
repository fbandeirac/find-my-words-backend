package com.kivikood.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class WordDocument @JsonCreator constructor(
    @JsonProperty("id") val id: String,
    @JsonProperty("word") val word: String,
    @JsonProperty("language") val language: String
)