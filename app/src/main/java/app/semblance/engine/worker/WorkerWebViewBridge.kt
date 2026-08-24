package app.semblance.engine.worker

import android.app.Activity
import android.content.Context
import android.content.MutableContextWrapper
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.MainThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrowserChromeState(
    val profileId: Int = -1,
    val url: String = "about:blank",
    val title: String = "",
    val progress: Int = 0,
    val loading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
)

interface InteractiveBrowserHost {
    fun showFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams
    ): Boolean

    fun showCustomView(
        view: View,
        callback: WebChromeClient.CustomViewCallback
    )

    fun hideCustomView()

    fun openExternalUri(uri: Uri): Boolean

    fun requestDownload(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    )

    fun finishBrowser()
}

object WorkerWebViewBridge {
    private val _webView = MutableStateFlow<WebView?>(null)
    val webView: StateFlow<WebView?> = _webView.asStateFlow()

    private val _chromeState = MutableStateFlow(BrowserChromeState())
    val chromeState: StateFlow<BrowserChromeState> = _chromeState.asStateFlow()

    @Volatile
    var host: InteractiveBrowserHost? = null
        private set

    private var contextWrapper: MutableContextWrapper? = null
    private var attachedParent: ViewGroup? = null
    private var backgroundWidth: Int = 1080
    private var backgroundHeight: Int = 2400

    fun newWebViewContext(applicationContext: Context): MutableContextWrapper =
        MutableContextWrapper(applicationContext)

    @MainThread
    fun publish(
        view: WebView,
        wrapper: MutableContextWrapper,
        profileId: Int,
        width: Int,
        height: Int
    ) {
        contextWrapper = wrapper
        backgroundWidth = width.coerceAtLeast(1)
        backgroundHeight = height.coerceAtLeast(1)
        _webView.value = view
        updateFrom(view, profileId = profileId)
    }

    @MainThread
    fun registerHost(value: InteractiveBrowserHost) {
        host = value
    }

    @MainThread
    fun unregisterHost(value: InteractiveBrowserHost) {
        if (host === value) host = null
    }

    @MainThread
    fun attach(activity: Activity, container: ViewGroup) {
        val view = _webView.value ?: return
        if (view.parent !== container) {
            (view.parent as? ViewGroup)?.removeView(view)
            container.removeAllViews()
            contextWrapper?.baseContext = activity
            container.addView(
                view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        attachedParent = container
        view.visibility = View.VISIBLE
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.requestFocus(View.FOCUS_DOWN)
        view.onResume()
        updateFrom(view)
    }

    @MainThread
    fun detach(applicationContext: Context) {
        val view = _webView.value ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        attachedParent = null
        contextWrapper?.baseContext = applicationContext
        view.measure(
            View.MeasureSpec.makeMeasureSpec(
                backgroundWidth,
                View.MeasureSpec.EXACTLY
            ),
            View.MeasureSpec.makeMeasureSpec(
                backgroundHeight,
                View.MeasureSpec.EXACTLY
            )
        )
        view.layout(0, 0, backgroundWidth, backgroundHeight)
    }

    @MainThread
    fun isAttached(view: WebView): Boolean =
        _webView.value === view && attachedParent != null

    @MainThread
    fun updateFrom(
        view: WebView,
        profileId: Int = _chromeState.value.profileId,
        title: String = _chromeState.value.title,
        progress: Int = _chromeState.value.progress,
        loading: Boolean = _chromeState.value.loading
    ) {
        _chromeState.value = BrowserChromeState(
            profileId = profileId,
            url = view.url ?: "about:blank",
            title = title,
            progress = progress.coerceIn(0, 100),
            loading = loading,
            canGoBack = view.canGoBack(),
            canGoForward = view.canGoForward()
        )
    }

    @MainThread
    fun clear(view: WebView, applicationContext: Context) {
        if (_webView.value !== view) return
        host?.finishBrowser()
        detach(applicationContext)
        host = null
        contextWrapper = null
        attachedParent = null
        _webView.value = null
        _chromeState.value = BrowserChromeState()
    }
}
