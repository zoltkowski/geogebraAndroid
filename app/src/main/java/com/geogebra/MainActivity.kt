package com.geogebra

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast


import com.geogebra.databinding.ActivityMainBinding



const val APP_URL = "https://geogebra.org/geometry"


class MainActivity : AppCompatActivity() {
    private var uploadMessage: ValueCallback<Uri>? = null
    private var uploadMessageAboveL: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val webView = binding.webview

        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }
            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
                uploadMessageAboveL = filePathCallback
                openImageChooser()
                return true
            }
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "No Browser Found", Toast.LENGTH_LONG).show()
            }
        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                }
                else {
                    webView.evaluateJavascript("javascript:ggbApplet.undo();", null)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        webView.loadUrl(APP_URL)
    }
    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        contentActivityResultLauncher.launch(intent)
    }
    private val contentActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (uploadMessageAboveL != null) {
                    onActivityResultAboveL(data)
                } else if (uploadMessage != null) {
                    val resultUri = data?.data
                    uploadMessage?.onReceiveValue(resultUri)
                    uploadMessage = null
                }
            } else {
                uploadMessage?.onReceiveValue(null)
                uploadMessage = null
                uploadMessageAboveL?.onReceiveValue(null)
                uploadMessageAboveL = null
            }
        }

    private fun onActivityResultAboveL(data: Intent?) {
        var results: Array<Uri>? = null
        if (data != null) {
            val dataString = data.dataString
            val clipData = data.clipData
            if (clipData != null) {
                results = Array(clipData.itemCount) { i ->
                    clipData.getItemAt(i).uri
                }
            }
            if (dataString != null) {
                results = arrayOf(Uri.parse(dataString))
            }
        }
        uploadMessageAboveL?.onReceiveValue(results)
        uploadMessageAboveL = null
    }
}