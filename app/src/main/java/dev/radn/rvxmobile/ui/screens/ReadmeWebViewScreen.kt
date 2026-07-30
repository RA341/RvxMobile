package dev.radn.rvxmobile.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ReadmeWebViewScreen(rawMarkdown: String) {
    val currentBgColor = MaterialTheme.colorScheme.background
    val isDark = (currentBgColor.red * 0.2126f + currentBgColor.green * 0.7152f + currentBgColor.blue * 0.0722f) < 0.5f
    val bgColor = currentBgColor.toHtmlHex()
    val textColor = MaterialTheme.colorScheme.onBackground.toHtmlHex()
    val borderColor = MaterialTheme.colorScheme.outlineVariant.toHtmlHex()
    val primaryColor = MaterialTheme.colorScheme.primary.toHtmlHex()
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.toHtmlHex()
    
    val escapedMarkdown = remember(rawMarkdown) {
        rawMarkdown
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    val htmlTemplate = remember(escapedMarkdown, isDark, bgColor, textColor, borderColor, primaryColor, surfaceColor) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/5.5.0/github-markdown.min.css">
          <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
          <style>
            :root {
              --color-canvas-default: $bgColor !important;
              --color-fg-default: $textColor !important;
              --color-accent-fg: $primaryColor !important;
              --color-border-default: $borderColor !important;
              --color-canvas-subtle: $surfaceColor !important;
            }
            body {
              box-sizing: border-box;
              min-width: 200px;
              max-width: 980px;
              margin: 0 auto;
              padding: 16px;
              background-color: $bgColor !important;
              color: $textColor !important;
            }
            .markdown-body {
              background-color: $bgColor !important;
              color: $textColor !important;
            }
            .markdown-body a {
              color: $primaryColor !important;
            }
            .markdown-body table tr {
              background-color: $bgColor !important;
              border-top: 1px solid $borderColor !important;
            }
            .markdown-body table tr:nth-child(2n) {
              background-color: $surfaceColor !important;
            }
            .markdown-body table th, .markdown-body table td {
              border: 1px solid $borderColor !important;
            }
            details {
              border: 1px solid $borderColor !important;
              padding: 8px 12px;
              border-radius: 6px;
              margin-bottom: 8px;
              background-color: $surfaceColor !important;
            }
            summary {
              cursor: pointer;
              font-weight: 600;
              color: $textColor !important;
            }
          </style>
        </head>
        <body class="markdown-body">
          <div id="content">Loading...</div>
          <script>
            try {
              const markdown = `$escapedMarkdown`;
              document.getElementById('content').innerHTML = marked.parse(markdown);
            } catch (e) {
              document.getElementById('content').innerText = "Error rendering markdown: " + e.message;
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                loadDataWithBaseURL("https://github.com", htmlTemplate, "text/html", "utf-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://github.com", htmlTemplate, "text/html", "utf-8", null)
        }
    )
}

private fun Color.toHtmlHex(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02x%02x%02x", r, g, b)
}
