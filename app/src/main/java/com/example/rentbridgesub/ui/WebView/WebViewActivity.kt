package com.example.rentbridgesub.ui.WebView

import android.annotation.SuppressLint
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
import com.example.rentbridgesub.R

class WebViewActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        // 1) WebView 레퍼런스 가져오기
        val webView: WebView = findViewById(R.id.webView)

        // 2) WebSettings 설정
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
        }

        // 3) WebViewClient / WebChromeClient 설정
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e(
                    "WebViewError",
                    "url=${request?.url} / errCode=${error?.errorCode} / desc=${error?.description}"
                )
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WebViewDebug", "pageFinished: $url")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            // 팝업을 처리하기 위해 onCreateWindow를 오버라이드
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                // 1) 팝업 전용 WebView를 동적으로 생성
                val popupContainer = findViewById<FrameLayout>(R.id.webview_popup_container)
                popupContainer.visibility = View.VISIBLE

                val newWebView = WebView(this@WebViewActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient   = view?.webViewClient!!
                    webChromeClient = view?.webChromeClient
                    requestFocus()  // 포커스를 팝업 WebView로 넘긴다
                }

                // 2) 실제 레이아웃에 붙인다
                popupContainer.addView(newWebView)

                // 3) Transport로 webView 넘겨줌
                val transport = (resultMsg?.obj as? WebView.WebViewTransport)
                transport?.webView = newWebView
                resultMsg?.sendToTarget()
                return true
            }
        }

        // 4) JavaScript 인터페이스 등록
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun processDATA(data: String) {
                // 사용자가 주소를 선택하면 호출되는 콜백
                Log.d("WebViewActivity", "JS에서 받은 주소: $data")  // ✅ Android 로그
                val intent = Intent()
                intent.putExtra("selectedAddress", data)
                setResult(RESULT_OK, intent)
                finish()
            }
        }, "Android")

        // 5) assets 폴더 안에 있는 HTML 로드
        webView.loadUrl("file:///android_asset/postcode.html")
    }
}
