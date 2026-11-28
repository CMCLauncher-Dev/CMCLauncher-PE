package com.cmc.launcher.ui.screens.content.transfer

import android.os.SystemClock
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.cmc.launcher.game.transfer.InstanceTransferReceiver
import com.cmc.launcher.game.transfer.TransferProtocol
import com.cmc.launcher.game.transfer.TransferException
import com.cmc.launcher.game.transfer.receiveTransferredInstance
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.components.SimpleListItem
import com.cmc.launcher.ui.components.StaticProgressIndicator
import com.cmc.launcher.ui.kit.CmcChip
import com.cmc.launcher.ui.kit.CmcListAction
import com.cmc.launcher.ui.kit.CmcPanel
import com.cmc.launcher.ui.theme.Space
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.net.SocketFactory

@Composable
fun ReceiveInstanceDialog(
    versions: List<Version>,
    initial: Version?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var address by rememberSaveable { mutableStateOf("") }
    var verification by remember { mutableStateOf<String?>(null) }
    var targetName by rememberSaveable { mutableStateOf(initial?.getVersionName()) }
    var manualAddress by rememberSaveable { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var transferJob by remember { mutableStateOf<Job?>(null) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    var receivedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var importedFiles by remember { mutableIntStateOf(0) }
    var installing by remember { mutableStateOf<String?>(null) }

    val availableVersions = versions.filter { it.isValid() }
    val target = availableVersions.firstOrNull { it.getVersionName() == targetName }
    val addressReady = InstanceTransferReceiver.splitAddress(address) != null
    var devices by remember { mutableStateOf(emptyList<String>()) }
    var scanning by remember { mutableStateOf(false) }
    var scanRequest by remember { mutableIntStateOf(0) }
    var hotspotMode by rememberSaveable { mutableStateOf(false) }
    var hotspotNetwork by remember { mutableStateOf<Network?>(null) }
    var useSystemWifi by remember { mutableStateOf(false) }
    var systemWifi by remember { mutableStateOf<Network?>(null) }
    val network = if (hotspotMode) hotspotNetwork ?: systemWifi else null
    val readyToDiscover = !hotspotMode || network != null
    val scroll = rememberScrollState()

    LaunchedEffect(running, result != null, errorRes != null) {
        if (running || result != null || errorRes != null) scroll.animateScrollTo(0, tween(150))
    }
    val connectivity = remember(context) { context.getSystemService(ConnectivityManager::class.java) }

    @Suppress("DEPRECATION")
    fun connectedWifi(): Network? = connectivity.allNetworks.firstOrNull {
        connectivity.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    LaunchedEffect(hotspotMode, useSystemWifi) {
        while (hotspotMode && useSystemWifi) {
            systemWifi = connectedWifi()
            delay(1000)
        }
    }
    LaunchedEffect(scanRequest, running, readyToDiscover, network) {
        if (!readyToDiscover) return@LaunchedEffect
        while (!running) {
            scanning = true
            try {
                devices = if (network != null) {
                    TransferProtocol.discover(connectivity.getLinkProperties(network)?.interfaceName) {
                        network.bindSocket(it)
                    }
                } else {
                    TransferProtocol.discover()
                }
                if (address.isBlank() && !manualAddress) {
                    devices.singleOrNull()?.let { address = it }
                }
            } finally {
                scanning = false
            }
            delay(5000)
        }
    }

    fun changeMode(useHotspot: Boolean) {
        if (running || hotspotMode == useHotspot) return
        hotspotMode = useHotspot
        hotspotNetwork = null
        systemWifi = null
        useSystemWifi = false
        address = ""
        devices = emptyList()
        manualAddress = false
        errorRes = null
        result = null
    }

    fun start() {
        val destination = target?.getGameDir()
        if (!readyToDiscover || !addressReady || running) return
        running = true
        errorRes = null
        result = null
        verification = null
        receivedBytes = 0L
        totalBytes = 0L
        importedFiles = 0
        installing = null
        choosing = false
        transferJob = scope.launch {
            var lastProgressUpdate = 0L
            try {
                val transfer = receiveTransferredInstance(
                    context = context.applicationContext,
                    address = address,
                    target = destination,
                    onBytes = { done, size ->
                        val now = SystemClock.elapsedRealtime()
                        if (totalBytes == 0L || done == size || now - lastProgressUpdate >= 100) {
                            lastProgressUpdate = now
                            receivedBytes = done
                            totalBytes = size
                        }
                    },
                    onImport = { importedFiles = it },
                    onAwaitingApproval = { verification = it },
                    onInstalling = { installing = it },
                    socketFactory = network?.socketFactory ?: SocketFactory.getDefault()
                )
                result = if (destination == null) {
                    context.getString(R.string.pe_transfer_created, transfer.target.name, transfer.files)
                } else if (transfer.files == 0) {
                    context.getString(R.string.pe_transfer_receive_empty, transfer.profile.name)
                } else {
                    context.getString(
                        R.string.pe_transfer_receive_done,
                        transfer.profile.name,
                        transfer.files,
                        transfer.bytes / 1024 / 1024
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: TransferException) {
                errorRes = failure.reason.messageRes()
            } catch (failure: Exception) {
                errorRes = R.string.pe_transfer_error_io
            } finally {
                running = false
            }
        }
    }

    DialogFrame(
        title = stringResource(R.string.pe_transfer_receive_title),
        onDismiss = { if (!running) onDismiss() },
        actions = {
            TextButton(onClick = { if (running) transferJob?.cancel() else onDismiss() }) {
                Text(stringResource(if (running) R.string.generic_cancel else R.string.generic_close))
            }
            TextButton(
                enabled = !running && readyToDiscover && addressReady,
                onClick = { start() }
            ) {
                Text(stringResource(R.string.pe_transfer_request_connection))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(scroll)
                .padding(horizontal = Space.xl, vertical = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            val showStatus = running || result != null || errorRes != null
            AnimatedVisibility(visible = showStatus, enter = fadeIn(tween(140)), exit = fadeOut(tween(110))) {
                CmcPanel {
                    if (running) {
                        StaticProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = if (totalBytes > 0 && installing == null) {
                                { (receivedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) }
                            } else null
                        )
                        Spacer(Modifier.height(Space.xs))
                        val status = when {
                            importedFiles > 0 -> stringResource(R.string.pe_transfer_importing, importedFiles)
                            installing != null -> stringResource(R.string.pe_transfer_installing, installing.orEmpty())
                            totalBytes > 0 -> stringResource(R.string.pe_transfer_receiving, receivedBytes / 1048576, totalBytes / 1048576)
                            verification != null -> stringResource(R.string.pe_transfer_verify_on_sender, verification.orEmpty())
                            else -> stringResource(R.string.pe_transfer_contacting_sender)
                        }
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                    }
                    result?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.pe_transfer_restart_hint), style = MaterialTheme.typography.bodySmall)
                    }
                    errorRes?.let { error ->
                        Text(stringResource(error), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (!running) {
                CmcPanel(Modifier.animateContentSize(tween(160))) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        CmcChip(stringResource(R.string.pe_transfer_lan_mode), !hotspotMode, { changeMode(false) })
                        CmcChip(stringResource(R.string.pe_transfer_hotspot_mode), hotspotMode, { changeMode(true) })
                    }
                    if (!hotspotMode) {
                        Spacer(Modifier.height(Space.sm))
                        Text(
                            stringResource(R.string.pe_transfer_receive_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (hotspotMode) {
                TransferHotspotPicker(
                    enabled = !running,
                    onNetworkChanged = { connected ->
                        hotspotNetwork = connected
                        if (connected != null) {
                            useSystemWifi = false
                            systemWifi = null
                        }
                        devices = emptyList()
                        address = ""
                    },
                    onUseSystemWifi = {
                        devices = emptyList()
                        address = ""
                        useSystemWifi = true
                    }
                )
            }
            if (!running && readyToDiscover) {
                CmcPanel {
                    Text(stringResource(R.string.pe_transfer_sender_heading), style = MaterialTheme.typography.titleSmall)
                    if (devices.isEmpty()) {
                        Text(
                            stringResource(if (scanning) R.string.pe_transfer_scanning else R.string.pe_transfer_none_found),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    devices.forEach { device ->
                        SimpleListItem(
                            selected = address == device,
                            itemName = device,
                            onClick = {
                                address = device
                                manualAddress = false
                            }
                        )
                    }
                    Row {
                        TextButton(enabled = !scanning, onClick = { scanRequest++ }) {
                            Text(stringResource(R.string.pe_transfer_scan))
                        }
                        TextButton(onClick = { manualAddress = !manualAddress }) {
                            Text(stringResource(R.string.pe_transfer_address))
                        }
                    }
                    if (!manualAddress && address.isNotBlank() && address !in devices) {
                        Text(address, style = MaterialTheme.typography.bodySmall)
                    }
                    AnimatedVisibility(manualAddress, enter = fadeIn(tween(140)), exit = fadeOut(tween(110))) {
                        OwnOutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = address.isNotBlank() && !addressReady,
                            label = { Text(stringResource(R.string.pe_transfer_address)) },
                            placeholder = { Text("192.168.1.10:45838") }
                        )
                    }
                }
            }
            if (!running) {
                CmcListAction(
                    title = target?.getVersionName() ?: stringResource(R.string.pe_transfer_create_instance),
                    iconRes = R.drawable.ic_package_2_outlined,
                    summary = target?.getVersionSummary() ?: stringResource(R.string.pe_transfer_create_detail),
                    endIconRes = if (availableVersions.isNotEmpty()) R.drawable.ic_arrow_drop_down_rounded else null,
                    enabled = !running && availableVersions.isNotEmpty(),
                    onClick = { choosing = !choosing }
                )
                if (choosing) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                        contentPadding = PaddingValues(vertical = Space.xs)
                    ) {
                        item {
                            SimpleListItem(
                                modifier = Modifier.fillMaxWidth(),
                                selected = target == null,
                                itemName = stringResource(R.string.pe_transfer_create_instance),
                                onClick = {
                                    targetName = null
                                    choosing = false
                                }
                            )
                        }
                        items(availableVersions, key = { it.getVersionPath().absolutePath }) { version ->
                            SimpleListItem(
                                modifier = Modifier.fillMaxWidth(),
                                selected = version == target,
                                itemName = version.getVersionName(),
                                summary = { Text(version.getVersionSummary(), style = MaterialTheme.typography.bodySmall) },
                                enabled = !running && version.isValid(),
                                onClick = {
                                    targetName = version.getVersionName()
                                    choosing = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun TransferException.Reason.messageRes(): Int = when (this) {
    TransferException.Reason.INVALID_ADDRESS -> R.string.pe_transfer_error_address
    TransferException.Reason.CONNECT_FAILED -> R.string.pe_transfer_error_connect
    TransferException.Reason.PAIRING_REJECTED -> R.string.pe_transfer_error_rejected
    TransferException.Reason.INVALID_ARCHIVE -> R.string.pe_transfer_error_archive
    TransferException.Reason.BATCH_UNSUPPORTED -> R.string.pe_transfer_error_batch
    TransferException.Reason.NO_SPACE -> R.string.pe_transfer_error_space
    TransferException.Reason.IO_ERROR -> R.string.pe_transfer_error_io
    TransferException.Reason.VERSION_UNSUPPORTED -> R.string.pe_transfer_error_version
    TransferException.Reason.INSTALL_FAILED -> R.string.pe_transfer_error_install
}