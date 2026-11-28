package com.cmc.launcher.ui.screens.content.settings

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmc.launcher.R
import com.cmc.launcher.context.getFileName
import com.cmc.launcher.coroutine.Task
import com.cmc.launcher.coroutine.TaskSystem
import com.cmc.launcher.game.launch.executeJarWithUri
import com.cmc.launcher.game.multirt.Runtime
import com.cmc.launcher.game.multirt.RuntimesManager
import com.cmc.launcher.path.PathManager
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.ui.androidText
import com.cmc.launcher.ui.components.CmcTag
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.components.SimpleListItem
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.theme.CardShape
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.utils.file.checkExtensionOrThrow
import com.cmc.launcher.utils.string.getMessageOrToString
import com.cmc.launcher.viewmodel.ErrorViewModel
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.sendToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class JavaManageViewModel : ViewModel() {
    private val _runtimes = MutableStateFlow<List<Runtime>>(emptyList())
    val runtimes = _runtimes.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private var scan: Job? = null
    private var generation = 0L

    fun refresh() {
        viewModelScope.launch {
            val ticket = ++generation
            scan?.cancel()
            _loading.value = true
            _error.value = null
            scan = viewModelScope.launch {
                try {
                    val list = withContext(Dispatchers.IO) { RuntimesManager.getRuntimes(forceLoad = true) }
                    if (ticket == generation) _runtimes.value = list
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    if (ticket == generation) _error.value = failure.getMessageOrToString()
                } finally {
                    if (ticket == generation) _loading.value = false
                }
            }
        }
    }
}

@Composable
fun JavaManageScreen(key: NestedNavKey.Settings, settingsScreenKey: TitledNavKey?, mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel, submitError: (ErrorViewModel.ThrowableMessage) -> Unit) {
    if (key != mainScreenKey || settingsScreenKey != NormalNavKey.Settings.JavaManager) return
    val model: JavaManageViewModel = viewModel(key = "pe-java-manager") { JavaManageViewModel() }
    val runtimes by model.runtimes.collectAsStateWithLifecycle()
    val loading by model.loading.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val report by rememberUpdatedState(submitError)
    var deleteTarget by remember { mutableStateOf<Runtime?>(null) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var launchName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingJar by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRuntime by rememberSaveable { mutableStateOf<String?>(null) }
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val busyRuntimes = tasks.map { it.id }.toSet()
    LaunchedEffect(model) { model.refresh() }
    LaunchedEffect(pendingJar, pendingRuntime) {
        val uri = pendingJar ?: return@LaunchedEffect
        try {
            val name = pendingRuntime
            val runtime = withContext(Dispatchers.IO) {
                RuntimesManager.getRuntimes(forceLoad = true).firstOrNull { it.name == name && it.isCompatible() }
            } ?: error(context.getString(R.string.multirt_runtime_corrupt))
            check(!TaskSystem.containsTask("pe-java-${runtime.name}")) { context.getString(R.string.generic_in_progress) }
            var host = context
            while (host is android.content.ContextWrapper && host !is Activity && host.baseContext !== host) host = host.baseContext
            val activity = host as? Activity ?: error(context.getString(R.string.generic_error))
            executeJarWithUri(activity, Uri.parse(uri), runtime.name)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            report(ErrorViewModel.ThrowableMessage(androidText(R.string.execute_jar_title), androidText(failure.getMessageOrToString())))
        }
        pendingJar = null
        pendingRuntime = null
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { importRuntime(context, it, model::refresh, report) }
    }
    val jarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingRuntime = launchName.takeIf { uri != null }
        pendingJar = uri?.toString()
        launchName = null
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.lg), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pe_java_runtimes), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(enabled = !loading, onClick = model::refresh) {
                Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.generic_refresh))
            }
        }
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            TextButton(onClick = {
                try {
                    importPicker.launch("*/*")
                } catch (failure: Exception) {
                    report(ErrorViewModel.ThrowableMessage(androidText(R.string.multirt_runtime_import_failed), androidText(failure.getMessageOrToString())))
                }
            }) { Text(stringResource(R.string.generic_import) + " (.tar.xz)") }
            TextButton(enabled = !loading, onClick = {
                if (runtimes.none { it.javaVersion == 8 && it.isCompatible() }) {
                    eventViewModel.sendToast(androidText(R.string.multirt_no_java_8), Toast.LENGTH_LONG)
                } else {
                    if (runtimes.none { it.name == selectedName && it.isCompatible() }) {
                        selectedName = runtimes.firstOrNull { it.name == AllSettings.javaRuntime.getValue() && it.isCompatible() }?.name
                            ?: runtimes.firstOrNull { it.javaVersion == 8 && it.isCompatible() }?.name
                    }
                    choosing = true
                }
            }) { Text(stringResource(R.string.execute_jar_title)) }
        }
        HorizontalDivider()
        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.generic_loading)) }
            error != null -> Column(Modifier.weight(1f).fillMaxWidth().padding(Space.lg),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error.orEmpty())
                TextButton(onClick = model::refresh) { Text(stringResource(R.string.generic_retry)) }
            }
            runtimes.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth().padding(Space.lg), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.pe_java_empty))
            }
            else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.md)) {
                items(runtimes, key = { it.name }) { runtime ->
                    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(Modifier.fillMaxWidth().padding(Space.md), verticalAlignment = Alignment.CenterVertically) {
                            RuntimeDetails(runtime, Modifier.weight(1f))
                            IconButton(enabled = (!runtime.isProvidedByLauncher || !runtime.isCompatible()) &&
                                "pe-java-${runtime.name}" !in busyRuntimes, onClick = { deleteTarget = runtime }) {
                                Icon(painterResource(R.drawable.ic_delete_outlined), stringResource(R.string.generic_delete) + " · " + runtime.name)
                            }
                        }
                    }
                }
            }
        }
    }
    if (choosing) {
        val selected = runtimes.firstOrNull { it.name == selectedName && it.isCompatible() && "pe-java-${it.name}" !in busyRuntimes }
        DialogFrame(stringResource(R.string.execute_jar_title), { choosing = false }, actions = {
            TextButton(onClick = { choosing = false }) { Text(stringResource(R.string.generic_cancel)) }
            TextButton(enabled = selected != null, onClick = {
                if (selected != null) {
                    launchName = selected.name
                    try {
                        jarPicker.launch("*/*")
                        choosing = false
                    } catch (failure: Exception) {
                        launchName = null
                        report(ErrorViewModel.ThrowableMessage(androidText(R.string.execute_jar_title), androidText(failure.getMessageOrToString())))
                    }
                }
            }) { Text(stringResource(R.string.generic_confirm)) }
        }) {
            LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth(), contentPadding = PaddingValues(Space.md)) {
                items(runtimes, key = { it.name }) { runtime ->
                    SimpleListItem(itemName = runtime.name, selected = selectedName == runtime.name,
                        enabled = runtime.isCompatible() && "pe-java-${runtime.name}" !in busyRuntimes,
                        onClick = { selectedName = runtime.name }, summary = { RuntimeSummary(runtime) })
                }
            }
        }
    }
    deleteTarget?.let { runtime ->
        SimpleAlertDialog(title = stringResource(R.string.generic_delete),
            text = stringResource(R.string.multirt_runtime_delete_message, runtime.name),
            confirmText = stringResource(R.string.generic_delete), onDismiss = { deleteTarget = null }, onConfirm = {
                deleteTarget = null
                TaskSystem.submitTask(Task.runTask(id = "pe-java-${runtime.name}", dispatcher = Dispatchers.IO,
                    task = { task ->
                        task.updateMessage(androidText(R.string.multirt_runtime_deleting, runtime.name))
                        RuntimesManager.removeRuntime(runtime.name)
                    }, onError = { failure ->
                        withContext(Dispatchers.Main) {
                            report(ErrorViewModel.ThrowableMessage(androidText(R.string.multirt_runtime_delete_failed), androidText(failure.getMessageOrToString())))
                        }
                    }, onFinally = model::refresh))
            })
    }
}

@Composable
private fun RuntimeDetails(runtime: Runtime, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(runtime.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        RuntimeSummary(runtime)
    }
}

@Composable
private fun RuntimeSummary(runtime: Runtime) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        if (runtime.isProvidedByLauncher) CmcTag(stringResource(R.string.multirt_runtime_provided_by_launcher),
            MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        Text(runtime.versionString?.let { stringResource(R.string.multirt_runtime_version_name, it) }
            ?: stringResource(R.string.multirt_runtime_corrupt), style = MaterialTheme.typography.bodySmall,
            color = if (runtime.versionString == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        runtime.arch?.let {
            Text(stringResource(if (runtime.isCompatible()) R.string.multirt_runtime_version_arch else R.string.multirt_runtime_incompatible_arch, it),
                style = MaterialTheme.typography.bodySmall,
                color = if (runtime.isCompatible()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
        }
    }
}

private fun importRuntime(context: Context, uri: Uri, refresh: () -> Unit, report: (ErrorViewModel.ThrowableMessage) -> Unit) {
    val name = context.getFileName(uri)
    if (name == null) {
        report(ErrorViewModel.ThrowableMessage(androidText(R.string.multirt_runtime_import_failed), androidText(R.string.multirt_runtime_import_failed_file_name)))
        return
    }
    val app = context.applicationContext
    TaskSystem.submitTask(Task.runTask(id = "pe-java-$name", dispatcher = Dispatchers.IO,
        task = { task ->
            name.checkExtensionOrThrow(listOf("xz"))
            val input = app.contentResolver.openInputStream(uri) ?: error(app.getString(R.string.multirt_runtime_import_failed_input_stream))
            input.use {
                RuntimesManager.installRuntime(PathManager.DIR_NATIVE_LIB, it, name) { resource, args ->
                    task.updateMessage(androidText(resource, *args))
                }
            }
        }, onError = { failure ->
            withContext(Dispatchers.Main) {
                report(ErrorViewModel.ThrowableMessage(androidText(R.string.multirt_runtime_import_failed), androidText(failure.getMessageOrToString())))
            }
        }, onFinally = refresh))
}