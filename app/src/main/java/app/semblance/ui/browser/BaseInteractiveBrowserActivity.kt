package app.semblance.ui.browser

import android.Manifest
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.semblance.engine.worker.InteractiveBrowserHost
import app.semblance.engine.worker.WorkerWebViewBridge
import app.semblance.ui.theme.SemblanceTheme
import app.semblance.util.UrlUtils

abstract class BaseInteractiveBrowserActivity :
    ComponentActivity(),
    InteractiveBrowserHost {

    private lateinit var root: FrameLayout
    private lateinit var composeView: ComposeView
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingDownload: DownloadSpec? = null

    private data class DownloadSpec(
        val url: String,
        val userAgent: String,
        val contentDisposition: String?,
        val mimeType: String?
    )

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val selected = WebChromeClient.FileChooserParams.parseResult(
            result.resultCode,
            result.data
        )
        fileCallback?.onReceiveValue(selected)
        fileCallback = null
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val spec = pendingDownload
        pendingDownload = null
        if (granted && spec != null) {
            enqueueDownload(spec)
        } else if (!granted) {
            toast("Storage permission is required on Android 9")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        val profileId = intent.getIntExtra(EXTRA_PROFILE_ID, -1)
        val alias = intent.getStringExtra(EXTRA_ALIAS).orEmpty()

        composeView = ComposeView(this).apply {
            setContent {
                SemblanceTheme {
                    InteractiveBrowserScreen(
                        activity = this@BaseInteractiveBrowserActivity,
                        profileId = profileId,
                        alias = alias,
                        onClose = { finish() }
                    )
                }
            }
        }

        root.addView(
            composeView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (customView != null) {
                        hideCustomView()
                        return
                    }
                    val webView = WorkerWebViewBridge.webView.value
                    if (webView?.canGoBack() == true) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        WorkerWebViewBridge.registerHost(this)
        WorkerWebViewBridge.webView.value?.onResume()
    }

    override fun onStop() {
        WorkerWebViewBridge.webView.value?.onPause()
        WorkerWebViewBridge.unregisterHost(this)
        super.onStop()
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        if (::root.isInitialized) {
            WorkerWebViewBridge.detach(applicationContext)
        }
        super.onDestroy()
    }

    override fun showFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams
    ): Boolean {
        fileCallback?.onReceiveValue(null)
        fileCallback = callback
        return try {
            fileChooserLauncher.launch(params.createIntent())
            true
        } catch (_: ActivityNotFoundException) {
            fileCallback?.onReceiveValue(null)
            fileCallback = null
            toast("No file picker is installed")
            false
        }
    }

    override fun showCustomView(
        view: View,
        callback: WebChromeClient.CustomViewCallback
    ) {
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }
        customView = view
        customViewCallback = callback
        composeView.visibility = View.GONE
        root.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun hideCustomView() {
        val view = customView ?: return
        root.removeView(view)
        customView = null
        composeView.visibility = View.VISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    override fun openExternalUri(uri: Uri): Boolean {
        val allowed = setOf("mailto", "tel", "sms", "geo", "market")
        if (uri.scheme?.lowercase() !in allowed) return false
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    override fun requestDownload(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        val spec = DownloadSpec(
            url,
            userAgent,
            contentDisposition,
            mimeType
        )
        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = spec
            storagePermissionLauncher.launch(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            enqueueDownload(spec)
        }
    }

    private fun enqueueDownload(spec: DownloadSpec) {
        val uri = Uri.parse(spec.url)
        val filename = URLUtil.guessFileName(
            spec.url,
            spec.contentDisposition,
            spec.mimeType
        )
        val request = DownloadManager.Request(uri)
            .setTitle(filename)
            .setMimeType(spec.mimeType)
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                filename
            )
        if (spec.userAgent.isNotBlank()) {
            request.addRequestHeader("User-Agent", spec.userAgent)
        }
        CookieManager.getInstance().getCookie(spec.url)?.let {
            request.addRequestHeader("Cookie", it)
        }
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        toast("Downloading $filename")
    }

    override fun finishBrowser() {
        runOnUiThread { finish() }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_ALIAS = "alias"
    }
}

@Composable
private fun InteractiveBrowserScreen(
    activity: BaseInteractiveBrowserActivity,
    profileId: Int,
    alias: String,
    onClose: () -> Unit
) {
    val webView by WorkerWebViewBridge.webView.collectAsState()
    val chrome by WorkerWebViewBridge.chromeState.collectAsState()
    val focusManager = LocalFocusManager.current
    var address by remember { mutableStateOf(TextFieldValue(chrome.url)) }

    LaunchedEffect(chrome.url) {
        if (!address.text.equals(chrome.url, ignoreCase = true)) {
            address = TextFieldValue(chrome.url)
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    enabled = chrome.canGoBack,
                    onClick = { webView?.goBack() }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                IconButton(
                    enabled = chrome.canGoForward,
                    onClick = { webView?.goForward() }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Forward")
                }
                IconButton(
                    onClick = {
                        if (chrome.loading) webView?.stopLoading()
                        else webView?.reload()
                    }
                ) {
                    Icon(
                        if (chrome.loading) Icons.Default.Close
                        else Icons.Default.Refresh,
                        if (chrome.loading) "Stop" else "Reload"
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            UrlUtils.normalizeUrl(address.text)?.let {
                                webView?.loadUrl(it)
                            }
                            focusManager.clearFocus()
                        }
                    ),
                    label = { Text(if (alias.isBlank()) "URL" else "@$alias") }
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close browser")
                }
            }
            if (chrome.loading) {
                LinearProgressIndicator(
                    progress = { chrome.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (webView == null || chrome.profileId != profileId) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Waiting for profile browser process...")
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context -> FrameLayout(context) },
                    update = { container ->
                        WorkerWebViewBridge.attach(activity, container)
                    }
                )
                DisposableEffect(Unit) {
                    onDispose {
                        WorkerWebViewBridge.detach(
                            activity.applicationContext
                        )
                    }
                }
            }
        }
    }
}
