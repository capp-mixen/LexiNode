@file:OptIn(ExperimentalMaterial3Api::class)

package com.lexinode.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lexinode.app.LexiViewModel
import com.lexinode.app.data.CollectionWithCount

@Composable
fun CollectionsScreen(vm: LexiViewModel, onOpen: (Long) -> Unit) {
    val collections by vm.collections.collectAsState()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<CollectionWithCount?>(null) }
    var deleteTarget by remember { mutableStateOf<CollectionWithCount?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Мои коллекции") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Новая коллекция") },
            )
        },
    ) { padding ->
        val list = collections
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
                    "Коллекций пока нет.\nНажмите «Новая коллекция», чтобы создать первую.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(list, key = { it.id }) { item ->
                    CollectionCard(
                        item = item,
                        onOpen = { onOpen(item.id) },
                        onRename = { renameTarget = item },
                        onDelete = { deleteTarget = item },
                    )
                }
            }
        }
    }

    if (showCreate) {
        NameDialog(
            title = "Новая коллекция",
            initial = "",
            confirmText = "Создать",
            onDismiss = { showCreate = false },
            onConfirm = {
                vm.createCollection(it)
                showCreate = false
            },
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            title = "Переименовать коллекцию",
            initial = target.name,
            confirmText = "Сохранить",
            onDismiss = { renameTarget = null },
            onConfirm = {
                vm.renameCollection(target.id, it)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Удалить коллекцию?",
            message = "Коллекция «${Bidi.isolate(target.name)}» и все её слова " +
                "(${wordsCountLabel(target.wordCount)}) будут удалены без возможности восстановления.",
            confirmText = "Удалить",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deleteCollection(target.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
private fun CollectionCard(
    item: CollectionWithCount,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = contentDirectionStyle(MaterialTheme.typography.titleMedium),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    wordsCountLabel(item.wordCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Действия")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Переименовать") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menu = false
                            onRename()
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
}
