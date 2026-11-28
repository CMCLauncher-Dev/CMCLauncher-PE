package com.cmc.launcher.ui.screens.content

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key as routeContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation3.runtime.NavBackStack
import com.cmc.launcher.R
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.navigateTo
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel

fun NavBackStack<TitledNavKey>.navigateToWeb(webUrl: String) = navigateTo(NormalNavKey.WebScreen(webUrl), useClassEquality = true)

private fun webAddress(value: String?): Uri? {
    if (value.isNullOrBlank()) return null
    val uri = Uri.parse(value)
    return uri.takeIf { (it.scheme.equals("https", true) || it.scheme.equals("http", true)) && !it.host.isNullOrBlank() }
}

@Composable
fun WebViewScreen(
    key: NormalNavKey.WebScreen,
    backStackViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel
) {
    if (backStackViewModel.mainScreen.backStack.lastOrNull() != key) return
    routeContent(key.url) {
        val initial = remember(key.url) { webAddress(key.url) }
        if (initial == null) {
            Box(Modifier.fillMaxSize().padding(Space.lg), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.pe_web_invalid))
            }
        } else {
            BrowserPage(initial) { eventViewModel.sendEvent(EventViewModel.Event.OpenLink(it)) }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserPage(initial: Uri, openExternal: (String) -> Unit) {
    var browser by remember { mutableStateOf<WebView?>(null) }
    var location by remember { mutableStateOf(initial) }
    var hasHistory by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    val retry = { browser?.reload(); Unit }
    BackHandler(enabled = hasHistory) { browser?.goBack() }
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = hasHistory, onClick = { browser?.goBack() }) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.generic_back))
                }
                Text(location.host.orEmpty(), Modifier.weight(1f).padding(horizontal = Space.sm),
                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(enabled = browser != null && !loading, onClick = retry) {
                    Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.generic_refresh))
                }
                IconButton(onClick = { openExternal(location.toString()) }) {
                    Icon(painterResource(R.drawable.ic_link), stringResource(R.string.generic_open_link))
                }
            }
        }
        if (loading) Text(stringResource(R.string.generic_loading),
            Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.xs),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                request.isForMainFrame && webAddress(request.url.toString()) == null

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                webAddress(url)?.let { location = it }
                                loading = true
                                failed = false
                            }

                            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                hasHistory = view.canGoBack()
                                webAddress(url)?.let { location = it }
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                webAddress(view.url)?.let { location = it }
                                hasHistory = view.canGoBack()
                                loading = false
                            }

                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                if (request.isForMainFrame) {
                                    failed = true
                                    loading = false
                                    hasHistory = view.canGoBack()
                                }
                            }
                        }
                        browser = this
                        loadUrl(initial.toString())
                    }
                },
                onRelease = { view ->
                    view.webViewClient = WebViewClient()
                    view.stopLoading()
                    view.clearHistory()
                    view.clearCache(true)
                    view.destroy()
                    CookieManager.getInstance().removeAllCookies(null)
                    browser = null
                }
            )
            if (failed) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxSize().padding(Space.lg), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.pe_web_failed))
                        TextButton(onClick = retry) { Text(stringResource(R.string.generic_retry)) }
                    }
                }
            }
        }
    }
}
