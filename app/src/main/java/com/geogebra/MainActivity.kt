package com.geogebra

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

const val APP_URL = "https://geogebra.org/geometry"

class MyWebViewActivity : AppCompatActivity() {
    private var uploadMessage: ValueCallback<Uri>? = null
    private var uploadMessageAboveL: ValueCallback<Array<Uri>>? = null
    private val FILE_DOWNLOAD_PERMISSION_REQUEST = 1000

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myWebView: WebView = findViewById(R.id.webview)
        myWebView.settings.javaScriptEnabled = true
        myWebView.webViewClient = WebViewClient()

        // Enable downloads
        myWebView.settings.setSupportZoom(true)
        myWebView.settings.allowFileAccess = true
        myWebView.settings.allowContentAccess = true

        // Handle download click events
        myWebView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(applicationContext, "Downloading File", Toast.LENGTH_LONG).show()
        }
        myWebView.loadUrl(APP_URL)
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        contentActivityResultLauncher.launch(intent)
    }

    private fun onActivityResultAboveL(resultCode: Int, intent: Intent?) {
        var results: Array<Uri>? = null
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                val dataString = intent.dataString
                val clipData = intent.clipData
                if (clipData != null) {
                    results = Array(clipData.itemCount) { i ->
                        clipData.getItemAt(i).uri
                    }
                }
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        uploadMessageAboveL?.onReceiveValue(results)
        uploadMessageAboveL = null
    }

    private val contentActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (uploadMessageAboveL != null) {
                    onActivityResultAboveL(result.resultCode, data)
                } else if (uploadMessage != null) {
                    val result = data?.data
                    uploadMessage?.onReceiveValue(result)
                    uploadMessage = null
                }
            } else {
                uploadMessage?.onReceiveValue(null)
                uploadMessage = null
                uploadMessageAboveL?.onReceiveValue(null)
                uploadMessageAboveL = null
            }
        }
}
