package com.baec23.anonboard.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Post(
    @Id
    val id: String?,
    val userDisplayName: String,
    val message: String,
    val createdTimestamp: Long,
    val parentId: String?,
    val childIds: List<String>,
    val nestingLevel: Int = 0,
)
