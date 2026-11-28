package com.cmc.launcher.ui.screens.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation3.runtime.NavBackStack
import com.cmc.launcher.R
import com.cmc.launcher.setting.enums.isLauncherInDarkTheme
import com.cmc.launcher.ui.code_editor.EditorState
import com.cmc.launcher.ui.code_editor.SoraEditor
import com.cmc.launcher.ui.code_editor.TextMateRegistry
import com.cmc.launcher.ui.code_editor.scheme.SchemeIDEADark
import com.cmc.launcher.ui.code_editor.scheme.SchemeIDEALight
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.navigateTo
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.utils.logging.Logger
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun NavBackStack<TitledNavKey>.navigateToLogView(logPath: String) = navigateTo(NormalNavKey.LogView(logPath))

private sealed interface LogPage {
    data object Loading : LogPage
    data object Empty : LogPage
    data class Failed(val detail: String?) : LogPage
    data class Ready(val content: EditorState.Success, val truncated: Boolean) : LogPage
}

@Composable
fun LogViewScreen(key: NormalNavKey.LogView, backStackViewModel: ScreenBackStackViewModel) {
    if (backStackViewModel.mainScreen.currentKey != key) return
    val file = remember(key.logPath) { File(key.logPath) }
    var generation by remember(file) { mutableIntStateOf(0) }
    var page by remember(file) { mutableStateOf<LogPage>(LogPage.Loading) }
    LaunchedEffect(file, generation) {
        page = LogPage.Loading
        page = try {
            withContext(Dispatchers.IO) {
                val preview = LogPreviewReader.read(file)
                if (preview.text.isEmpty()) LogPage.Empty else LogPage.Ready(EditorState.Success(Content(preview.text)), preview.truncated)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Logger.warning("ViewLog", "Unable to read log file", error)
            LogPage.Failed(error.localizedMessage)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = Space.sm)) {
                Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(file.parent.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(enabled = page != LogPage.Loading, onClick = { generation++ }) {
                Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.generic_refresh))
            }
        }
        HorizontalDivider()
        when (val state = page) {
            is LogPage.Ready -> {
                if (state.truncated) Text(stringResource(R.string.pe_log_tail), Modifier.padding(horizontal = Space.md, vertical = Space.xs),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LogEditor(state.content, Modifier.weight(1f))
            }
            else -> Box(Modifier.weight(1f).fillMaxWidth().padding(Space.lg), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text(stringResource(when (state) {
                        LogPage.Loading -> R.string.generic_loading
                        LogPage.Empty -> R.string.pe_log_empty
                        else -> R.string.pe_log_failed
                    }))
                    if (state is LogPage.Failed) {
                        state.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { generation++ }) { Text(stringResource(R.string.generic_retry)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEditor(state: EditorState.Success, modifier: Modifier) {
    val dark = isLauncherInDarkTheme()
    val context = LocalContext.current
    val fallback = remember(dark) { if (dark) SchemeIDEADark() else SchemeIDEALight() }
    var language by remember { mutableStateOf<Language?>(null) }
    var scheme by remember(dark) { mutableStateOf<EditorColorScheme?>(null) }
    LaunchedEffect(dark, context) {
        language = TextMateRegistry.languageFor("text.log", context)
        scheme = TextMateRegistry.colorScheme(dark, context)
    }
    SoraEditor(modifier = modifier.fillMaxWidth(), state = state, isReadOnly = true, scheme = scheme ?: fallback,
        language = language, onSaveClick = {})
}
