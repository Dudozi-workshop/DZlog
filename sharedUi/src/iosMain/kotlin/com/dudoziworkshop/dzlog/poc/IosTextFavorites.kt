package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dudoziworkshop.dzlog.ui.table.favorites.*
import com.dudoziworkshop.dzlog.ui.theme.DDZTheme
import kotlinx.coroutines.delay
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults

/** Both iOS controllers observe the same ordered list. Each presents its own dialogs. */
private object IosTextFavoritesStore : TextFavoritesStore {
    private val key = if (NSProcessInfo.processInfo.arguments.filterIsInstance<String>().any {
        it.startsWith("--") && it.endsWith("-flow")
    }) "dzlog.uitest.text-favorites.v1" else "dzlog.text-favorites.v1"
    private val defaults = NSUserDefaults.standardUserDefaults
    init {
        if (key == "dzlog.uitest.text-favorites.v1" &&
            NSProcessInfo.processInfo.arguments.contains("--reset-quick-values-fixture")) {
            defaults.removeObjectForKey(key)
        }
    }

    override var favorites by mutableStateOf(
        defaults.stringArrayForKey(key).orEmpty().filterIsInstance<String>()
            .filter(String::isNotBlank).distinct().take(MAX_TEXT_FAVORITES)
    )
        private set

    override fun save(items: List<String>) {
        defaults.setObject(items, forKey = key)
        favorites = items
    }
}

@Composable
internal fun IosAppTheme(content: @Composable () -> Unit) {
    val state = remember { TextFavoritesState(IosTextFavoritesStore) }
    DDZTheme {
        CompositionLocalProvider(LocalTextFavorites provides state) {
            Box(Modifier.fillMaxSize(), propagateMinConstraints = true) { content() }
            TextFavoritesDialogs(state)
            state.lastChange?.let { change ->
                LaunchedEffect(change.serial) {
                    delay(4000)
                    state.clearUndo(change.serial)
                }
                Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.BottomCenter) {
                    Snackbar(action = { TextButton(onClick = state::undo) { Text("실행 취소") } }) {
                        Text(change.label)
                    }
                }
            }
        }
    }
}
