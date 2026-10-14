package com.odom.docreader.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per document the user has opened. */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val uri: String,
    val name: String,
    val type: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val lastOpenedAt: Long,
)
