package app.semblance.engine.worker

import android.app.Service
import android.content.Intent
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import app.semblance.BuildConfig
import app.semblance.engine.ipc.IEngineCallback
import app.semblance.engine.ipc.IEngineWorker
import app.semblance.engine.model.ActionJson
import app.semblance.util.UrlUtils
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

abstract class BaseEngineWorker : Service() {

    private var webView: WebView? = null
    private var currentProfileId: Int = -1
    private var currentSuffix: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val callbacks = RemoteCallbackList<IEngineCallback>()

    private val json = Json { ignoreUnknownKeys = true }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        mainHandler.post {
            val view = webView
            if (view != null) {
                WorkerWebViewBridge.clear(view, applicationContext)
                view.stopLoading()
                view.loadUrl("about:blank")
                view.removeAllViews()
                view.destroy()
            }
            webView = null
        }
        callbacks.kill()
        super.onDestroy()
    }

    private fun applyViewport(w: Int, h: Int) {
        mainHandler.post {
            val view = webView ?: return@post
            if (WorkerWebViewBridge.isAttached(view)) return@post
            val width = w.coerceAtLeast(1)
            val height = h.coerceAtLeast(1)
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, width, height)
        }
    }

    private val binder = object : IEngineWorker.Stub() {

        override fun openProfile(profileData: Bundle) {
            val suffix = profileData.getString("suffix") ?: return
            val profileId = profileData.getInt("id", -1)

            if (currentSuffix != null && currentSuffix != suffix) {
                Process.killProcess(Process.myPid())
                return
            }

            mainHandler.post {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && currentSuffix == null) {
                    try {
                        WebView.setDataDirectorySuffix(suffix)
                        currentSuffix = suffix
                    } catch (e: IllegalStateException) {
                        if (currentSuffix != suffix) {
                            Process.killProcess(Process.myPid())
                            return@post
                        }
                    }
                }

                currentProfileId = profileId

                val screenW = profileData.getInt("screen_width", 1080).coerceAtLeast(1)
                val screenH = profileData.getInt("screen_height", 2400).coerceAtLeast(1)

                if (webView == null) {
                    val wrapper = WorkerWebViewBridge.newWebViewContext(applicationContext)
                    webView = WebView(wrapper).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            allowFileAccess = false
                            allowContentAccess = true
                            allowFileAccessFromFileURLs = false
                            allowUniversalAccessFromFileURLs = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            safeBrowsingEnabled = true
                        }
                        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

                        val userAgent = profileData.getString("user_agent")
                        if (!userAgent.isNullOrBlank()) {
                            settings.userAgentString = userAgent
                        }

                        val currentWv = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(currentWv, true)
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                WorkerWebViewBridge.updateFrom(
                                    view,
                                    progress = newProgress,
                                    loading = newProgress < 100
                                )
                            }

                            override fun onReceivedTitle(view: WebView, title: String?) {
                                WorkerWebViewBridge.updateFrom(
                                    view,
                                    title = title.orEmpty()
                                )
                            }

                            override fun onShowFileChooser(
                                webView: WebView,
                                filePathCallback: ValueCallback<Array<Uri>>,
                                fileChooserParams: FileChooserParams
                            ): Boolean {
                                return WorkerWebViewBridge.host?.showFileChooser(
                                    filePathCallback,
                                    fileChooserParams
                                ) ?: false
                            }

                            override fun onShowCustomView(
                                view: View,
                                callback: CustomViewCallback
                            ) {
                                val host = WorkerWebViewBridge.host
                                if (host == null) {
                                    callback.onCustomViewHidden()
                                    return
                                }
                                host.showCustomView(view, callback)
                                broadcastCustomView(true)
                            }

                            override fun onHideCustomView() {
                                WorkerWebViewBridge.host?.hideCustomView()
                                broadcastCustomView(false)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val uri = request.url
                                val scheme = uri.scheme?.lowercase()
                                if (scheme == "http" || scheme == "https") {
                                    return false
                                }
                                if (!request.isForMainFrame) return true
                                val handled = WorkerWebViewBridge.host?.openExternalUri(uri) == true
                                if (!handled) {
                                    broadcastError("Unsupported URL scheme: $scheme")
                                }
                                return true
                            }

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                if (view != null) {
                                    WorkerWebViewBridge.updateFrom(view, loading = true)
                                }
                                val currentUrl = url ?: "about:blank"
                                broadcastState("BROWSING", currentUrl)
                                try {
                                    val uri = Uri.parse(currentUrl)
                                    val host = uri.host
                                    if (!host.isNullOrBlank()) { broadcastDomain(host) }
                                } catch (_: Exception) {}
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (view != null) {
                                    WorkerWebViewBridge.updateFrom(view, loading = false)
                                }
                                broadcastState("IDLE", url ?: "about:blank")
                            }

                            override fun onReceivedSslError(
                                view: WebView,
                                handler: SslErrorHandler,
                                error: SslError
                            ) {
                                handler.cancel()
                                broadcastError("SSL certificate error for ${error.url}")
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    val errorCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.errorCode?.toString() ?: "UNKNOWN" else "ERROR"
                                    val errorDesc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.description?.toString() ?: "Network error" else "Network error"
                                    val failedUrl = request.url?.toString() ?: view?.url ?: "about:blank"
                                    broadcastError("Failed loading $failedUrl ($errorCode: $errorDesc)")
                                    broadcastState("ERROR", failedUrl)
                                }
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                            val host = WorkerWebViewBridge.host
                            if (host == null) {
                                broadcastError("Open the interactive browser to download files")
                            } else {
                                host.requestDownload(
                                    url = url,
                                    userAgent = userAgent.orEmpty(),
                                    contentDisposition = contentDisposition,
                                    mimeType = mimeType,
                                    contentLength = contentLength
                                )
                            }
                        }
                    }

                    val created = checkNotNull(webView)
                    WorkerWebViewBridge.publish(
                        view = created,
                        wrapper = wrapper,
                        profileId = profileId,
                        width = screenW,
                        height = screenH
                    )
                    applyViewport(screenW, screenH)
                }

                val rawUrl = profileData.getString("last_url")?.takeIf { it.isNotBlank() } ?: "https://www.google.com"
                val initialUrl = UrlUtils.normalizeUrl(rawUrl) ?: "https://www.google.com"
                webView?.loadUrl(initialUrl)
            }
        }

        override fun closeProfile(saveState: Boolean) {
            mainHandler.post {
                val view = webView
                if (view != null && saveState) {
                    val outBundle = Bundle()
                    view.saveState(outBundle)
                }
                broadcastState("SLEEPING", view?.url ?: "")
                if (view != null) {
                    WorkerWebViewBridge.clear(view, applicationContext)
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    view.removeAllViews()
                    view.destroy()
                }
                webView = null
                currentProfileId = -1
            }
        }

        override fun loadUrl(url: String) {
            mainHandler.post {
                val normalized = UrlUtils.normalizeUrl(url)
                if (normalized != null) {
                    webView?.loadUrl(normalized)
                } else {
                    broadcastError("Invalid URL: $url")
                    broadcastState("ERROR", url)
                }
            }
        }

        override fun requestThumbnail() {
            mainHandler.post {
                val view = webView ?: return@post
                if (WorkerWebViewBridge.isAttached(view)) return@post
                val wvWidth = view.width.takeIf { it > 0 } ?: 1080
                val wvHeight = view.height.takeIf { it > 0 } ?: 2400
                val targetWidth = 300
                val targetHeight = (300f * wvHeight / wvWidth).toInt().coerceAtLeast(1).coerceAtMost(600)
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
                val canvas = Canvas(bitmap)
                canvas.scale(targetWidth.toFloat() / wvWidth.toFloat(), targetHeight.toFloat() / wvHeight.toFloat())
                view.draw(canvas)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
                bitmap.recycle()
                broadcastThumbnail(stream.toByteArray())
            }
        }

        override fun executeAction(actionJson: String) {
            mainHandler.post {
                try {
                    val action = json.decodeFromString<ActionJson>(actionJson)
                    when (action) {
                        is ActionJson.Navigate -> {
                            val normalized = UrlUtils.normalizeUrl(action.url)
                            if (normalized != null) {
                                webView?.loadUrl(normalized)
                            } else {
                                broadcastError("Invalid URL: ${action.url}")
                                broadcastState("ERROR", action.url)
                            }
                        }
                        is ActionJson.Tap -> broadcastState("TYPING", webView?.url ?: "")
                        is ActionJson.TypeText -> broadcastState("TYPING", webView?.url ?: "")
                        is ActionJson.Back -> if (webView?.canGoBack() == true) webView?.goBack()
                        is ActionJson.Wait -> broadcastState("IDLE", webView?.url ?: "")
                        else -> {}
                    }
                } catch (e: Exception) { broadcastError("Failed to execute action: ${e.message}") }
            }
        }

        // P1B MINIMIZE / MAXIMIZE / APP SWITCH
        override fun maximize() {
            mainHandler.post {
                webView?.evaluateJavascript("document.querySelectorAll('video, audio').forEach(el => el.muted = false)", null)
            }
        }

        override fun minimize() {
            mainHandler.post {
                webView?.evaluateJavascript("document.querySelectorAll('video, audio').forEach(el => el.muted = true)", null)
            }
        }

        override fun simulateAppSwitch(durationMs: Long) {
            mainHandler.post {
                val wv = webView ?: return@post
                wv.onPause()
                mainHandler.postDelayed({ wv.onResume() }, durationMs)
            }
        }

        override fun registerCallback(cb: IEngineCallback?) { if (cb != null) callbacks.register(cb) }
        override fun unregisterCallback(cb: IEngineCallback?) { if (cb != null) callbacks.unregister(cb) }
    }

    private fun broadcastState(status: String, url: String) {
        broadcastCallbacks { cb -> cb.onStateChanged(currentProfileId, status, url) }
    }

    private fun broadcastDomain(host: String) {
        broadcastCallbacks { cb -> cb.onDomainVisited(currentProfileId, host) }
    }

    private fun broadcastError(message: String) {
        broadcastCallbacks { cb -> cb.onError(currentProfileId, message) }
    }

    private fun broadcastThumbnail(jpegData: ByteArray) {
        broadcastCallbacks { cb -> cb.onThumbnailReady(currentProfileId, jpegData) }
    }

    private fun broadcastCustomView(isShowing: Boolean) {
        broadcastCallbacks { cb -> cb.onCustomViewChanged(isShowing) }
    }

    private inline fun broadcastCallbacks(crossinline action: (IEngineCallback) -> Unit) {
        val count = callbacks.beginBroadcast()
        try {
            for (i in 0 until count) {
                try { action(callbacks.getBroadcastItem(i)) } catch (_: RemoteException) {}
            }
        } finally { callbacks.finishBroadcast() }
    }
}
