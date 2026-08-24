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
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
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

                if (webView == null) {
                    webView = WebView(this@BaseEngineWorker).apply {
                        // P1B HARDENING SETTINGS
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(false)
                            builtInZoomControls = false
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        val userAgent = profileData.getString("user_agent")
                        if (!userAgent.isNullOrBlank()) {
                            settings.userAgentString = userAgent
                        }

                        // P1B COOKIES & STEALTH
                        val currentWv = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(currentWv, true)
                        }
                        WebView.setWebContentsDebuggingEnabled(false)

                        // P1B FULLSCREEN VIDEO
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                broadcastCustomView(true)
                            }
                            override fun onHideCustomView() {
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
                                    if (!host.isNullOrBlank()) { broadcastDomain(host) }
                                } catch (_: Exception) {}
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                broadcastState("IDLE", url ?: "about:blank")
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    val errorMsg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.description?.toString() ?: "Network error" else "Network error"
                                    broadcastError(errorMsg)
                                    broadcastState("ERROR", view?.url ?: "about:blank")
                                }
                            }
                        }

                        // Ensure headless WebView in Service has realistic mobile dimensions for Chromium layout
                        val defaultWidth = 1080
                        val defaultHeight = 2400
                        measure(
                            View.MeasureSpec.makeMeasureSpec(defaultWidth, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(defaultHeight, View.MeasureSpec.EXACTLY)
                        )
                        layout(0, 0, defaultWidth, defaultHeight)
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

        override fun loadUrl(url: String) { mainHandler.post { webView?.loadUrl(url) } }

        override fun requestThumbnail() {
            mainHandler.post {
                val wv = webView ?: return@post
                val wvWidth = wv.width.takeIf { it > 100 } ?: 1080
                val wvHeight = wv.height.takeIf { it > 100 } ?: 2400
                if (wv.width <= 0 || wv.height <= 0) {
                    wv.measure(
                        View.MeasureSpec.makeMeasureSpec(wvWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(wvHeight, View.MeasureSpec.EXACTLY)
                    )
                    wv.layout(0, 0, wvWidth, wvHeight)
                }
                val targetWidth = 360
                val targetHeight = (360f * wvHeight / wvWidth).toInt().coerceAtLeast(1).coerceAtMost(800)
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
                val canvas = Canvas(bitmap)
                canvas.scale(targetWidth.toFloat() / wvWidth.toFloat(), targetHeight.toFloat() / wvHeight.toFloat())
                wv.draw(canvas)
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
                        is ActionJson.Navigate -> webView?.loadUrl(action.url)
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
