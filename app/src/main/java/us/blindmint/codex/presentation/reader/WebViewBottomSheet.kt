/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet

/**
 * Bottom sheet containing an embedded WebView for viewing dictionary/search results in-app.
 *
 * @param url The URL to load
 * @param onDismiss Called when the bottom sheet is dismissed
 * @param onOpenExternal Called when user wants to open in external browser
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewBottomSheet(
    url: String,
    onDismiss: () -> Unit,
    onOpenExternal: (String) -> Unit
) {
    var currentUrl by remember { mutableStateOf(url) }
    var pageTitle by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Clean up WebView when disposed
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                destroy()
            }
        }
    }

    ModalBottomSheet(
        hasFixedHeight = true,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        onDismissRequest = onDismiss,
        sheetGesturesEnabled = true
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with URL bar and actions
            WebViewHeader(
                url = currentUrl,
                title = pageTitle,
                isLoading = isLoading,
                loadingProgress = loadingProgress,
                onClose = onDismiss,
                onOpenExternal = { onOpenExternal(currentUrl) },
                onRefresh = { webView?.reload() }
            )

            // WebView content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
            ) {
                WebViewContent(
                    url = url,
                    onWebViewCreated = { webView = it },
                    onUrlChanged = { currentUrl = it },
                    onTitleChanged = { pageTitle = it },
                    onLoadingChanged = { isLoading = it },
                    onProgressChanged = { loadingProgress = it }
                )

                // Loading overlay for initial load
                AnimatedVisibility(
                    visible = isLoading && loadingProgress < 0.3f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun WebViewHeader(
    url: String,
    title: String,
    isLoading: Boolean,
    loadingProgress: Float,
    onClose: () -> Unit,
    onOpenExternal: () -> Unit,
    onRefresh: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_content_desc),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // URL/Title display
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = title.ifEmpty { extractDomain(url) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Refresh button
            IconButton(onClick = onRefresh) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh_content_desc),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Open in browser button
            IconButton(onClick = onOpenExternal) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = stringResource(R.string.open_in_browser),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Loading progress bar
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { loadingProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewContent(
    url: String,
    onWebViewCreated: (WebView) -> Unit,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onProgressChanged: (Float) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                    // Better text rendering
                    setSupportZoom(true)
                    textZoom = 100
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        // Handle navigation within the WebView
                        request?.url?.let { uri ->
                            onUrlChanged(uri.toString())
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChanged(true)
                        url?.let { onUrlChanged(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChanged(false)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress / 100f)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }
                }

                onWebViewCreated(this)
                loadUrl(url)
            }
        },
        update = { webView ->
            // Only reload if URL changed significantly (not just navigation)
            if (webView.url != url && !webView.url.orEmpty().startsWith(url.substringBefore("?"))) {
                webView.loadUrl(url)
            }
        }
    )
}

/**
 * Extracts the domain from a URL for display.
 */
private fun extractDomain(url: String): String {
    return try {
        val uri = android.net.Uri.parse(url)
        uri.host?.removePrefix("www.") ?: url
    } catch (e: Exception) {
        url
    }
}
