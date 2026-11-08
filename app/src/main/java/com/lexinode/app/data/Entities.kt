package com.lexinode.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class WordCollection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = WordCollection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("collectionId")],
)
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: Long,
    val text: String,
    val translation: String,
    val type: String,
    val createdAt: Long,
)

data class CollectionWithCount(
    val id: Long,
    val name: String,
    val wordCount: Int,
)

enum class WordType(val label: String) {
    NOUN("существительное"),
    VERB("глагол"),
    ADJECTIVE("прилагательное"),
    ADVERB("наречие"),
    PREPOSITION("предлог"),
    OTHER("другое");

    companion object {
        fun fromName(name: String): WordType = entries.firstOrNull { it.name == name } ?: OTHER
    }
}
