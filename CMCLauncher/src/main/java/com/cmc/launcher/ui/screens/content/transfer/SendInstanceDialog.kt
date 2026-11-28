package com.cmc.launcher.ui.screens.content.transfer

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.game.transfer.InstanceTransferSender
import com.cmc.launcher.game.transfer.TransferException
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.keepalive.TaskKeepAlive
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.SimpleListItem
import com.cmc.launcher.ui.components.StaticProgressIndicator
import com.cmc.launcher.ui.kit.CmcPanel
import com.cmc.launcher.ui.theme.Space
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class Approval(val address: String, val verification: String, val decision: CompletableDeferred<Boolean>)

@Composable
internal fun SendInstanceDialog(versions: List<Version>, initial: Version?, onDismiss: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val available = versions.filter { it.isValid() }
    var name by rememberSaveable { mutableStateOf(initial?.getVersionName()) }
    val selected = available.firstOrNull { it.getVersionName() == name } ?: available.firstOrNull()
    var job by remember { mutableStateOf<Job?>(null) }
    var addresses by remember { mutableStateOf<List<String>?>(null) }
    var approval by remember { mutableStateOf<Approval?>(null) }
    var transferred by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf<Int?>(null) }

    fun start(version: Version) {
        if (job != null || !version.isValid()) return
        message = null
        addresses = null
        transferred = 0
        total = 0
        job = scope.launch {
            TaskKeepAlive.acquire()
            var lastProgressUpdate = 0L
            try {
                InstanceTransferSender.send(
                    context, version,
                    onReady = { addresses = it },
                    confirm = { address, code ->
                        val decision = CompletableDeferred<Boolean>()
                        approval = Approval(address, code, decision)
                        try {
                            decision.await()
                        } finally {
                            approval = null
                        }
                    },
                    onProgress = { sent, size ->
                        val now = SystemClock.elapsedRealtime()
                        if (sent == 0L || sent == size || now - lastProgressUpdate >= 100) {
                            lastProgressUpdate = now
                            transferred = sent
                            total = size
                        }
                    }
                )
                message = R.string.pe_transfer_sent
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: TransferException) {
                message = failure.reason.messageRes()
            } catch (_: Exception) {
                message = R.string.pe_transfer_send_failed
            } finally {
                TaskKeepAlive.release()
                approval = null
                job = null
            }
        }
    }

    DialogFrame(
        title = stringResource(R.string.pe_transfer_send_title),
        onDismiss = { if (job == null) onDismiss() },
        actions = {
            TextButton(onClick = { job?.cancel() ?: onDismiss() }) {
                Text(stringResource(if (job == null) R.string.generic_close else R.string.generic_cancel))
            }
            val request = approval
            if (request != null) {
                TextButton(onClick = { request.decision.complete(false) }) {
                    Text(stringResource(R.string.pe_transfer_reject))
                }
                TextButton(onClick = { request.decision.complete(true) }) {
                    Text(stringResource(R.string.pe_transfer_approve))
                }
            } else if (job == null && selected != null) {
                TextButton(onClick = { start(selected) }) {
                    Text(stringResource(R.string.pe_transfer_start_sending))
                }
            }
        }
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()).padding(Space.xl)) {
            if (job == null) {
                Text(stringResource(if (available.isEmpty()) R.string.pe_transfer_no_send_versions else R.string.pe_transfer_send_hint))
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(available, key = { it.getVersionPath().absolutePath }) { version ->
                        SimpleListItem(
                            selected = version == selected,
                            itemName = version.getVersionName(),
                            onClick = { name = version.getVersionName() }
                        )
                    }
                }
            } else {
                val progress: (() -> Float)? = if (total > 0 && transferred < total) {
                    { (transferred.toFloat() / total).coerceIn(0f, 1f) }
                } else null
                StaticProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = progress)
                val request = approval
                val ready = addresses
                Text(
                    text = when {
                        request != null -> stringResource(R.string.pe_transfer_approve_hint, request.address, request.verification)
                        total > 0 && transferred >= total -> stringResource(R.string.pe_transfer_wait_receipt)
                        total > 0 -> stringResource(R.string.pe_transfer_sending, transferred / 1048576, total / 1048576)
                        ready != null -> stringResource(R.string.pe_transfer_wait_receiver)
                        else -> stringResource(R.string.pe_transfer_packing)
                    },
                    modifier = Modifier.padding(top = Space.sm),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (request == null && total == 0L && !ready.isNullOrEmpty()) {
                    SelectionContainer {
                        Text(ready.joinToString("\n"), modifier = Modifier.padding(top = Space.sm))
                    }
                }
            }
            AnimatedVisibility(message != null, enter = fadeIn(tween(140)), exit = fadeOut(tween(110))) {
                message?.let {
                    CmcPanel(Modifier.padding(top = Space.sm)) { Text(stringResource(it)) }
                }
            }
        }
    }
}
