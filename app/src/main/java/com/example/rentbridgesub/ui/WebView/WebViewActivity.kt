package com.example.rentbridgesub.ui.WebView

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.example.rentbridgesub.R

class WebViewActivity : AppCompatActivity() {

    private lateinit var webViewClientImpl: WebViewClient
    private lateinit var webChromeClientImpl: WebChromeClient

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        // 0) WebViewAssetLoader 세팅 (assets/ 폴더를 앱 내부 URL로 매핑)
        //
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(this)
            )
            .build()

        // 2) WebViewClient 구현체
        webViewClientImpl = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                // assetLoader 가 assets/ 요청 처리
                assetLoader.shouldInterceptRequest(request.url)
                    ?.let { return it }
                return super.shouldInterceptRequest(view, request)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WebViewDebug", "pageFinished: $url")
            }
            override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                super.onReceivedError(view, req, err)
                Log.e("WebViewError", "url=${req.url} err=${err.errorCode}/${err.description}")
            }
        }

        // 3) WebChromeClient 구현체 (팝업 처리 포함)
        webChromeClientImpl = object : WebChromeClient() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
            ): Boolean {
                val popupContainer = findViewById<FrameLayout>(R.id.webview_popup_container)
                popupContainer.visibility = View.VISIBLE

                val newWebView = WebView(this@WebViewActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            allowFileAccessFromFileURLs = true
                            allowUniversalAccessFromFileURLs = true
                        }
                    }
                    webViewClient   = webViewClientImpl
                    webChromeClient = webChromeClientImpl
                    addJavascriptInterface(AndroidBridge(this@WebViewActivity), "Android")
                    requestFocus()
                }

                popupContainer.addView(newWebView)
                (resultMsg.obj as? WebView.WebViewTransport)?.let {
                    it.webView = newWebView
                    resultMsg.sendToTarget()
                }
                return true
            }

            override fun onCloseWindow(window: WebView) {
                // 팝업 닫힐 때 컨테이너 정리
                findViewById<FrameLayout>(R.id.webview_popup_container).apply {
                    removeAllViews()
                    visibility = View.GONE
                }
            }
        }

        // 4) 실제 WebView에 세팅
        val webView: WebView = findViewById(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
            }
        }
        webView.webViewClient = webViewClientImpl
        webView.webChromeClient = webChromeClientImpl
        webView.addJavascriptInterface(AndroidBridge(this), "Android")

        // 5) HTML 로드
        webView.loadUrl("https://appassets.androidplatform.net/assets/postcode.html")
    }

    class AndroidBridge(private val activity: Activity) {
        // 반드시 android.webkit.JavascriptInterface 로 임포트
        @JavascriptInterface
        fun processDATA(data: String) {
            Log.d("WebViewActivity", "JS에서 받은 주소: $data")
            activity.setResult(Activity.RESULT_OK, Intent().putExtra("selectedAddress", data))
            activity.finish()
        }
    }
}
