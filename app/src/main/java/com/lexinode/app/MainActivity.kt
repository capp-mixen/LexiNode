package com.lexinode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lexinode.app.ui.CollectionScreen
import com.lexinode.app.ui.CollectionsScreen
import com.lexinode.app.ui.LexiNodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LexiNodeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    LexiNodeApp()
                }
            }
        }
    }
}

@Composable
fun LexiNodeApp(vm: LexiViewModel = viewModel()) {
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val id = openId
    if (id == null) {
        CollectionsScreen(vm = vm, onOpen = { openId = it })
    } else {
        BackHandler { openId = null }
        CollectionScreen(vm = vm, collectionId = id, onBack = { openId = null })
    }
}
