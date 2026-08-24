package app.semblance.engine.worker

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
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
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import app.semblance.engine.ipc.IEngineCallback
import app.semblance.engine.ipc.IEngineWorker
import app.semblance.engine.model.ActionJson
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
            webView?.destroy()
            webView = null
        }
        callbacks.kill()
        super.onDestroy()
    }

    private val binder = object : IEngineWorker.Stub() {

        override fun openProfile(profileData: Bundle) {
            val suffix = profileData.getString("suffix") ?: return
            val profileId = profileData.getInt("id", -1)

            // CRITICAL RULE 1: If suffix changes, process must die so a fresh process can set it
            if (currentSuffix != null && currentSuffix != suffix) {
                Process.killProcess(Process.myPid())
                return
            }

            mainHandler.post {
                // CRITICAL RULE 1 & §4: Must be called before ANY WebView is instantiated
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && currentSuffix == null) {
                    try {
                        WebView.setDataDirectorySuffix(suffix)
                        currentSuffix = suffix
                    } catch (e: IllegalStateException) {
                        // Suffix already set or WebView previously initialized
                        if (currentSuffix != suffix) {
                            Process.killProcess(Process.myPid())
                            return@post
                        }
                    }
                }

                currentProfileId = profileId

                if (webView == null) {
                    webView = WebView(this@BaseEngineWorker).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true               // CRITICAL for mobile layouts
                            loadWithOverviewMode = true
                            setSupportZoom(false)
                            builtInZoomControls = false
                            mediaPlaybackRequiresUserGesture = false 
                            cacheMode = WebSettings.LOAD_DEFAULT // Real cache behavior
                        }

                        val userAgent = profileData.getString("user_agent")
                        if (!userAgent.isNullOrBlank()) {
                            settings.userAgentString = userAgent
                        }

                        val currentWv = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(currentWv, true) // Required for Google Auth & YouTube
                        }
                        WebView.setWebContentsDebuggingEnabled(false)    // Stealth: hide devtools

                        webChromeClient = object : WebChromeClient() {
                            private var customView: View? = null
                            private var customViewCallback: CustomViewCallback? = null

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                if (customView != null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                customView = view
                                customViewCallback = callback
                                // Notify Main Process UI to show a fullscreen overlay
                                broadcastCustomView(true)
                            }

                            override fun onHideCustomView() {
                                customViewCallback?.onCustomViewHidden()
                                customView = null
                                broadcastCustomView(false)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                val currentUrl = url ?: "about:blank"
                                broadcastState("BROWSING", currentUrl)

                                try {
                                    val uri = Uri.parse(currentUrl)
                                    val host = uri.host
                                    if (!host.isNullOrBlank()) {
                                        broadcastDomain(host)
                                    }
                                } catch (e: Exception) {
                                    // Ignore parse errors
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val currentUrl = url ?: "about:blank"
                                broadcastState("IDLE", currentUrl)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    val errorMsg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        error?.description?.toString() ?: "Network error"
                                    } else {
                                        "Network error"
                                    }
                                    broadcastError(errorMsg)
                                    broadcastState("ERROR", view?.url ?: "about:blank")
                                }
                            }
                        }
                    }
                }

                val lastUrl = profileData.getString("last_url")?.takeIf { it.isNotBlank() } ?: "https://www.google.com"
                webView?.loadUrl(lastUrl)
                broadcastState("IDLE", lastUrl)
            }
        }

        override fun closeProfile(saveState: Boolean) {
            mainHandler.post {
                val wv = webView
                if (wv != null && saveState) {
                    val outBundle = Bundle()
                    wv.saveState(outBundle)
                }
                broadcastState("SLEEPING", wv?.url ?: "")
                wv?.destroy()
                webView = null
                currentProfileId = -1
            }
        }

        override fun loadUrl(url: String) {
            mainHandler.post {
                webView?.loadUrl(url)
            }
        }

        override fun requestThumbnail() {
            mainHandler.post {
                val wv = webView ?: return@post
                val wvWidth = wv.width.coerceAtLeast(1)
                val wvHeight = wv.height.coerceAtLeast(1)

                // CRITICAL RULE 2: Strict Binder Limit Enforcement
                val targetWidth = 300
                val targetHeight = (300f * wvHeight / wvWidth).toInt().coerceAtLeast(1).coerceAtMost(600)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
                val canvas = Canvas(bitmap)
                val scaleX = targetWidth.toFloat() / wvWidth.toFloat()
                val scaleY = targetHeight.toFloat() / wvHeight.toFloat()
                canvas.scale(scaleX, scaleY)
                wv.draw(canvas)

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                bitmap.recycle()

                val jpegBytes = stream.toByteArray()
                broadcastThumbnail(jpegBytes)
            }
        }

        override fun maximize() {
            mainHandler.post {
                // Unmute all media elements in this worker's WebView
                webView?.evaluateJavascript(
                    "document.querySelectorAll('video, audio').forEach(el => el.muted = false)", null
                )
            }
        }

        override fun minimize() {
            mainHandler.post {
                // Mute all media elements so 8 profiles don't scream at the operator
                webView?.evaluateJavascript(
                    "document.querySelectorAll('video, audio').forEach(el => el.muted = true)", null
                )
            }
        }

        override fun simulateAppSwitch(durationMs: Long) {
            mainHandler.post {
                val wv = webView ?: return@post
                // onPause() triggers Chromium's internal visibilitychange event to 'hidden'
                // YouTube will automatically pause the video.
                wv.onPause()

                mainHandler.postDelayed({
                    // onResume() restores visibility to 'visible', video remains paused
                    // until the agent taps play (human behavior).
                    wv.onResume()
                }, durationMs)
            }
        }

        override fun executeAction(actionJson: String) {
            mainHandler.post {
                try {
                    val action = json.decodeFromString<ActionJson>(actionJson)
                    when (action) {
                        is ActionJson.Navigate -> {
                            webView?.loadUrl(action.url)
                        }
                        is ActionJson.Tap -> {
                            broadcastState("TYPING", webView?.url ?: "")
                        }
                        is ActionJson.TypeText -> {
                            broadcastState("TYPING", webView?.url ?: "")
                        }
                        is ActionJson.Back -> {
                            if (webView?.canGoBack() == true) {
                                webView?.goBack()
                            }
                        }
                        is ActionJson.Wait -> {
                            broadcastState("IDLE", webView?.url ?: "")
                        }
                        is ActionJson.Maximize -> {
                            maximize()
                        }
                        is ActionJson.Minimize -> {
                            minimize()
                        }
                        is ActionJson.SimulateAppSwitch -> {
                            simulateAppSwitch(action.durationMs)
                        }
                        is ActionJson.Volume -> {
                            if (action.dir.lowercase() == "mute") {
                                minimize()
                            } else {
                                maximize()
                            }
                        }
                        else -> {
                            // Supported in P2 Motor phase
                        }
                    }
                } catch (e: Exception) {
                    broadcastError("Failed to execute action: ${e.message}")
                }
            }
        }

        override fun registerCallback(cb: IEngineCallback?) {
            if (cb != null) {
                callbacks.register(cb)
            }
        }

        override fun unregisterCallback(cb: IEngineCallback?) {
            if (cb != null) {
                callbacks.unregister(cb)
            }
        }
    }

    private fun broadcastState(status: String, url: String) {
        val pid = currentProfileId
        broadcastCallbacks { cb ->
            cb.onStateChanged(pid, status, url)
        }
    }

    private fun broadcastDomain(host: String) {
        val pid = currentProfileId
        broadcastCallbacks { cb ->
            cb.onDomainVisited(pid, host)
        }
    }

    private fun broadcastError(message: String) {
        val pid = currentProfileId
        broadcastCallbacks { cb ->
            cb.onError(pid, message)
        }
    }

    private fun broadcastThumbnail(jpegData: ByteArray) {
        val pid = currentProfileId
        broadcastCallbacks { cb ->
            cb.onThumbnailReady(pid, jpegData)
        }
    }

    private fun broadcastCustomView(isShowing: Boolean) {
        broadcastCallbacks { cb ->
            cb.onCustomViewChanged(isShowing)
        }
    }

    private inline fun broadcastCallbacks(crossinline action: (IEngineCallback) -> Unit) {
        val count = callbacks.beginBroadcast()
        try {
            for (i in 0 until count) {
                try {
                    action(callbacks.getBroadcastItem(i))
                } catch (e: RemoteException) {
                    // Dead binder connection
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }
}
