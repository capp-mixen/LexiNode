package com.lexinode.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LexiDao {
    @Query(
        "SELECT c.id AS id, c.name AS name, COUNT(w.id) AS wordCount " +
            "FROM collections c LEFT JOIN words w ON w.collectionId = c.id " +
            "GROUP BY c.id ORDER BY c.name COLLATE NOCASE",
    )
    fun observeCollections(): Flow<List<CollectionWithCount>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observeCollection(id: Long): Flow<WordCollection?>

    @Insert
    suspend fun insertCollection(collection: WordCollection): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun renameCollection(id: Long, name: String)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollectionRow(id: Long)

    @Query("DELETE FROM words WHERE collectionId = :collectionId")
    suspend fun deleteWordsOf(collectionId: Long)

    @Transaction
    suspend fun deleteCollection(id: Long) {
        deleteWordsOf(id)
        deleteCollectionRow(id)
    }

    @Query("SELECT * FROM words WHERE collectionId = :collectionId ORDER BY createdAt, id")
    fun observeWords(collectionId: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE collectionId = :collectionId ORDER BY createdAt, id")
    suspend fun getWords(collectionId: Long): List<Word>

    @Insert
    suspend fun insertWord(word: Word): Long

    @Update
    suspend fun updateWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)
}
