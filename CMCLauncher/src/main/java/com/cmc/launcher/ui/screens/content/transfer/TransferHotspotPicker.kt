package com.cmc.launcher.ui.screens.content.transfer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Network
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.ContextCompat
import com.cmc.launcher.R
import com.cmc.launcher.game.transfer.TransferHotspots
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.components.SimpleListItem
import com.cmc.launcher.ui.kit.CmcPanel
import kotlinx.coroutines.CancellationException

@Composable
internal fun TransferHotspotPicker(
    enabled: Boolean,
    onNetworkChanged: (Network?) -> Unit,
    onUseSystemWifi: () -> Unit
) {
    val context = LocalContext.current
    var hotspots by remember { mutableStateOf(emptyList<String>()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var scanRequest by remember { mutableIntStateOf(0) }
    var scanning by remember { mutableStateOf(false) }
    var connection by remember { mutableStateOf<Pair<String, String>?>(null) }
    var connected by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Int?>(null) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        when {
            granted[Manifest.permission.ACCESS_FINE_LOCATION] == true -> scanRequest++
            granted[Manifest.permission.NEARBY_WIFI_DEVICES] == true -> {
                val ssid = selected
                if (ssid != null) {
                    connection = ssid to password
                    password = ""
                }
            }
            else -> status = R.string.pe_transfer_hotspot_permission
        }
    }

    fun scan() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            scanRequest++
        } else {
            permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LaunchedEffect(scanRequest) {
        if (scanRequest == 0) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                scanRequest++
            }
            return@LaunchedEffect
        }
        scanning = true
        status = null
        try {
            hotspots = TransferHotspots.scan(context)
            if (hotspots.isEmpty()) status = R.string.pe_transfer_hotspot_empty
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            status = R.string.pe_transfer_hotspot_scan_error
        } finally {
            scanning = false
        }
    }

    LaunchedEffect(connection) {
        val (ssid, passphrase) = connection ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT < 29) return@LaunchedEffect
        status = R.string.pe_transfer_hotspot_connecting
        try {
            TransferHotspots.join(context, ssid, passphrase).collect { network ->
                connected = true
                status = R.string.pe_transfer_hotspot_connected
                onNetworkChanged(network)
            }
            status = R.string.pe_transfer_hotspot_join_error
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            status = R.string.pe_transfer_hotspot_join_error
        } finally {
            connected = false
            onNetworkChanged(null)
        }
        connection = null
    }

    if (!enabled) return

    CmcPanel(Modifier.animateContentSize(tween(160))) {
        Text(stringResource(R.string.pe_transfer_hotspot_hint), style = MaterialTheme.typography.bodySmall)
        Row {
            TextButton(enabled = !scanning && connection == null, onClick = ::scan) {
                Text(stringResource(if (scanning) R.string.pe_transfer_hotspot_scanning else R.string.pe_transfer_hotspot_scan))
            }
            TextButton(onClick = {
                connection = null
                onUseSystemWifi()
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }) {
                Text(stringResource(R.string.pe_transfer_wifi_settings))
            }
        }
        if (connection == null) {
            hotspots.forEach { ssid ->
                SimpleListItem(
                    selected = ssid == selected,
                    itemName = ssid,
                    enabled = Build.VERSION.SDK_INT >= 29,
                    onClick = {
                        selected = ssid
                        password = ""
                    }
                )
            }
            AnimatedVisibility(
                visible = selected != null && Build.VERSION.SDK_INT >= 29,
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(110))
            ) {
                val ssid = selected ?: return@AnimatedVisibility
                Column {
                    OwnOutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text(stringResource(R.string.pe_transfer_hotspot_password)) }
                    )
                    TextButton(enabled = password.length in 8..63, onClick = {
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                                context, Manifest.permission.NEARBY_WIFI_DEVICES
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissions.launch(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES))
                        } else {
                            connection = ssid to password
                            password = ""
                        }
                    }) { Text(stringResource(R.string.pe_transfer_hotspot_join)) }
                }
            }
        } else {
            TextButton(onClick = {
                connection = null
                status = null
            }) {
                Text(stringResource(if (connected) R.string.pe_transfer_hotspot_disconnect else R.string.generic_cancel))
            }
        }
        status?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
    }
}
