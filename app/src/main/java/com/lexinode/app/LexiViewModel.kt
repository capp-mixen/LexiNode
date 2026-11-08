package com.lexinode.app

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lexinode.app.data.AppDatabase
import com.lexinode.app.data.CollectionWithCount
import com.lexinode.app.data.Word
import com.lexinode.app.data.WordCollection
import com.lexinode.app.data.WordType
import com.lexinode.app.ui.Bidi
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LexiViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()

    val collections: StateFlow<List<CollectionWithCount>?> =
        dao.observeCollections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun collection(id: Long): Flow<WordCollection?> = dao.observeCollection(id)

    fun words(collectionId: Long): Flow<List<Word>> = dao.observeWords(collectionId)

    fun createCollection(name: String) {
        val clean = Bidi.clean(name)
        if (clean.isEmpty()) return
        viewModelScope.launch {
            dao.insertCollection(WordCollection(name = clean, createdAt = System.currentTimeMillis()))
        }
    }

    fun renameCollection(id: Long, name: String) {
        val clean = Bidi.clean(name)
        if (clean.isEmpty()) return
        viewModelScope.launch { dao.renameCollection(id, clean) }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch { dao.deleteCollection(id) }
    }

    fun addWord(collectionId: Long, text: String, translation: String, type: WordType) {
        val w = Bidi.clean(text)
        val t = Bidi.clean(translation)
        if (w.isEmpty() || t.isEmpty()) return
        viewModelScope.launch {
            dao.insertWord(
                Word(
                    collectionId = collectionId,
                    text = w,
                    translation = t,
                    type = type.name,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun updateWord(word: Word, text: String, translation: String, type: WordType) {
        val w = Bidi.clean(text)
        val t = Bidi.clean(translation)
        if (w.isEmpty() || t.isEmpty()) return
        viewModelScope.launch {
            dao.updateWord(word.copy(text = w, translation = t, type = type.name))
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch { dao.deleteWord(word) }
    }

    /** Writes the collection as UTF-8 text: "тип | слово | перевод" per line. Returns word count. */
    suspend fun export(resolver: ContentResolver, collectionId: Long, uri: Uri): Int =
        withContext(Dispatchers.IO) {
            val words = dao.getWords(collectionId)
            val content = buildString {
                words.forEach { w ->
                    append(WordType.fromName(w.type).label)
                    append(" | ")
                    append(w.text)
                    append(" | ")
                    append(w.translation)
                    append('\n')
                }
            }
            val stream: OutputStream = openForWrite(resolver, uri)
                ?: throw IOException("Cannot open $uri")
            stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            words.size
        }

    private fun openForWrite(resolver: ContentResolver, uri: Uri): OutputStream? =
        try {
            resolver.openOutputStream(uri, "wt")
        } catch (e: IllegalArgumentException) {
            resolver.openOutputStream(uri, "w")
        } catch (e: UnsupportedOperationException) {
            resolver.openOutputStream(uri, "w")
        } catch (e: java.io.FileNotFoundException) {
            resolver.openOutputStream(uri, "w")
        }
}
