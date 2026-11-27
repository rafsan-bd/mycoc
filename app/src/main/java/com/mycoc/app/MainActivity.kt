package com.mycoc.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Message
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var splashLayout: LinearLayout
    private lateinit var errorLayout: LinearLayout
    private var hasError = false

    private val chromeUA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        // 1. WebView
        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.userAgentString = chromeUA

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    hasError = false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    CookieManager.getInstance().flush()
                    swipeRefresh.isRefreshing = false
                    if (splashLayout.visibility == View.VISIBLE) {
                        splashLayout.animate().alpha(0f).setDuration(250).withEndAction {
                            splashLayout.visibility = View.GONE
                        }.start()
                    }
                    if (!hasError) {
                        errorLayout.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                    }
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        hasError = true
                        swipeRefresh.isRefreshing = false
                        splashLayout.visibility = View.GONE
                        webView.visibility = View.INVISIBLE
                        errorLayout.visibility = View.VISIBLE
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    val dialog = Dialog(this@MainActivity, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
                    val popupWebView = WebView(this@MainActivity).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = chromeUA
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                val u = req?.url?.toString() ?: ""
                                if (u.startsWith("https://coc.rafsanjani.top/?app=1")) {
                                    CookieManager.getInstance().flush()
                                    dialog.dismiss()
                                    webView.loadUrl(u)
                                    return true
                                }
                                return false
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onCloseWindow(window: WebView?) {
                                dialog.dismiss()
                            }
                        }
                    }
                    dialog.setContentView(popupWebView)
                    dialog.show()

                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    transport?.webView = popupWebView
                    resultMsg?.sendToTarget()
                    return true
                }
            }
        }

        // 2. Pull-to-Refresh
        swipeRefresh = SwipeRefreshLayout(this).apply {
            setColorSchemeColors(Color.parseColor("#F59E0B"))
            setProgressBackgroundColorSchemeColor(Color.parseColor("#1E293B"))
            addView(webView)
            setOnChildScrollUpCallback { _, _ -> webView.scrollY > 0 }
            setOnRefreshListener {
                hasError = false
                webView.reload()
            }
        }

        // 3. Offline Error Screen
        errorLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(64, 64, 64, 64)
            visibility = View.GONE

            addView(TextView(this@MainActivity).apply {
                text = "No Internet Connection"
                setTextColor(Color.WHITE)
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
            })

            addView(TextView(this@MainActivity).apply {
                text = "ইন্টারনেট কানেকশন চেক করে আবার চেষ্টা করুন"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 40)
            })

            addView(Button(this@MainActivity).apply {
                text = "Retry"
                setTextColor(Color.BLACK)
                setTypeface(null, Typeface.BOLD)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F59E0B"))
                    cornerRadius = 24f
                }
                setPadding(80, 24, 80, 24)
                setOnClickListener {
                    hasError = false
                    errorLayout.visibility = View.GONE
                    splashLayout.alpha = 1f
                    splashLayout.visibility = View.VISIBLE
                    webView.reload()
                }
            })
        }

        // 4. Splash Screen Overlay
        splashLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))

            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_launcher)
                layoutParams = LinearLayout.LayoutParams(240, 240)
            })

            addView(TextView(this@MainActivity).apply {
                text = "My COC"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#FACC15"))
                textSize = 26f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 24, 0, 32)
            })

            addView(ProgressBar(this@MainActivity).apply {
                indeterminateTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))
            })
        }

        root.addView(swipeRefresh)
        root.addView(errorLayout)
        root.addView(splashLayout)
        setContentView(root)

        webView.loadUrl("https://coc.rafsanjani.top/?app=1")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }
}
