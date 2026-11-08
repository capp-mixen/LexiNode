@file:OptIn(ExperimentalMaterial3Api::class)

package com.lexinode.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lexinode.app.LexiViewModel
import com.lexinode.app.data.Word
import com.lexinode.app.data.WordType
import com.lexinode.app.tts.SpeakResult
import com.lexinode.app.tts.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
private fun rememberTts(): TtsManager {
    val context = LocalContext.current.applicationContext
    val tts = remember { TtsManager(context) }
    DisposableEffect(tts) { onDispose { tts.shutdown() } }
    return tts
}

private fun exportFileName(name: String?): String {
    val base = (name ?: "collection").replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_").trim()
    return (base.ifEmpty { "collection" }) + ".txt"
}

@Composable
fun CollectionScreen(vm: LexiViewModel, collectionId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val collection by remember(collectionId) { vm.collection(collectionId) }.collectAsState(initial = null)
    val words by remember(collectionId) { vm.words(collectionId) }.collectAsState(initial = null)
    val tts = rememberTts()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var showAdd by rememberSaveable { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Word?>(null) }
    var deleteTarget by remember { mutableStateOf<Word?>(null) }
    var ttsProblem by remember { mutableStateOf<SpeakResult?>(null) }
    var scrollToEnd by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val count = vm.export(context.contentResolver, collectionId, uri)
                    snackbar.showSnackbar("Экспорт завершён: ${wordsCountLabel(count)}")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    snackbar.showSnackbar("Не удалось сохранить файл")
                }
            }
        }
    }

    LaunchedEffect(words?.size) {
        val list = words
        if (scrollToEnd && !list.isNullOrEmpty()) {
            listState.animateScrollToItem(list.lastIndex)
            scrollToEnd = false
        }
    }

    val speak: (Word) -> Unit = { word ->
        tts.speak(word.text) { result ->
            if (result != SpeakResult.OK) ttsProblem = result
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        collection?.name ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = contentDirectionStyle(MaterialTheme.typography.titleLarge),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (words.isNullOrEmpty()) {
                            Toast.makeText(context, "В коллекции нет слов для экспорта", Toast.LENGTH_SHORT).show()
                        } else {
                            exportLauncher.launch(exportFileName(collection?.name))
                        }
                    }) {
                        Icon(AppIcons.FileDownload, contentDescription = "Экспорт")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Слово") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val list = words
        when {
            list == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            list.isEmpty() -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "В этой коллекции пока нет слов.\nНажмите «Слово», чтобы добавить.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(list, key = { it.id }) { word ->
                    WordRow(
                        word = word,
                        onSpeak = { speak(word) },
                        onEdit = { editTarget = word },
                        onDelete = { deleteTarget = word },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAdd) {
        WordDialog(
            title = "Новое слово",
            initialText = "",
            initialTranslation = "",
            initialType = WordType.NOUN,
            onDismiss = { showAdd = false },
            onSave = { text, translation, type ->
                vm.addWord(collectionId, text, translation, type)
                scrollToEnd = true
                showAdd = false
            },
        )
    }

    editTarget?.let { target ->
        WordDialog(
            title = "Изменить слово",
            initialText = target.text,
            initialTranslation = target.translation,
            initialType = WordType.fromName(target.type),
            onDismiss = { editTarget = null },
            onSave = { text, translation, type ->
                vm.updateWord(target, text, translation, type)
                editTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Удалить слово?",
            message = "Слово «${Bidi.isolate(target.text)}» — «${Bidi.isolate(target.translation)}» " +
                "будет удалено из коллекции.",
            confirmText = "Удалить",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deleteWord(target)
                deleteTarget = null
            },
        )
    }

    ttsProblem?.let { problem ->
        TtsProblemDialog(problem = problem, onDismiss = { ttsProblem = null }, context = context)
    }
}

@Composable
private fun TtsProblemDialog(problem: SpeakResult, onDismiss: () -> Unit, context: android.content.Context) {
    when (problem) {
        SpeakResult.HEBREW_VOICE_MISSING -> MessageDialog(
            title = "Голос для иврита не установлен",
            message = "Чтобы слушать слова на иврите, установите голос иврита для синтеза речи:\n\n" +
                "1. Нажмите «Установить голос» (или откройте Настройки → Система → Язык и ввод → " +
                "Синтез речи / Text-to-speech).\n" +
                "2. Выберите движок «Синтезатор речи Google» (Speech Services by Google) и откройте его настройки.\n" +
                "3. Перейдите в «Установить голосовые данные» и скачайте язык «Иврит» (${Bidi.isolate("עברית")}).\n" +
                "4. Вернитесь в приложение и нажмите кнопку прослушивания ещё раз.\n\n" +
                "Если движка Google нет, установите «Speech Services by Google» из Google Play.",
            onDismiss = onDismiss,
            actions = listOf(
                "Установить голос" to {
                    onDismiss()
                    TtsManager.openInstallVoiceData(context)
                },
                "Настройки" to {
                    onDismiss()
                    TtsManager.openTtsSettings(context)
                },
            ),
        )

        SpeakResult.LANGUAGE_MISSING -> MessageDialog(
            title = "Голос не установлен",
            message = "Для языка устройства нет голоса синтеза речи. Откройте Настройки → Система → " +
                "Язык и ввод → Синтез речи и установите голосовые данные.",
            onDismiss = onDismiss,
            actions = listOf(
                "Настройки" to {
                    onDismiss()
                    TtsManager.openTtsSettings(context)
                },
            ),
        )

        SpeakResult.ENGINE_UNAVAILABLE -> MessageDialog(
            title = "Синтез речи недоступен",
            message = "Не удалось запустить синтез речи. Установите «Speech Services by Google» " +
                "из Google Play и выберите его в Настройки → Система → Язык и ввод → Синтез речи.",
            onDismiss = onDismiss,
            actions = listOf(
                "Настройки" to {
                    onDismiss()
                    TtsManager.openTtsSettings(context)
                },
            ),
        )

        SpeakResult.OK -> Unit
    }
}

/** "{тип} слово - перевод" with each part isolated so Hebrew never reorders the line. */
private fun wordLine(word: Word, typeColor: Color): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = typeColor, fontStyle = FontStyle.Italic)) {
        append("{")
        append(WordType.fromName(word.type).label)
        append("}")
    }
    append(" ")
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
        append(Bidi.isolate(word.text))
    }
    append(" - ")
    append(Bidi.isolate(word.translation))
}

@Composable
private fun WordRow(
    word: Word,
    onSpeak: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val typeColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = wordLine(word, typeColor),
            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
        )
        IconButton(onClick = onSpeak) {
            Icon(AppIcons.VolumeUp, contentDescription = "Прослушать")
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Действия")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Изменить") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        menu = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Удалить") },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = {
                        menu = false
                        onDelete()
                    },
                )
            }
        }
    }
}
