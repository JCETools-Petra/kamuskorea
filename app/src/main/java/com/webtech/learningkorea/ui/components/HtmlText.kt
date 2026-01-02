package com.webtech.learningkorea.ui.components

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

/**
 * Composable untuk menampilkan HTML text dengan formatting
 * Mendukung tag HTML seperti <p>, <b>, <i>, <u>, <br>, dll
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
                // Hilangkan padding ekstra dari TextView
                includeFontPadding = false
                setPadding(0, 0, 0, 0)
            }
        },
        update = { textView ->
            // Parse HTML dan set ke TextView
            val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)

            // Trim trailing whitespace dari Spanned
            var text = spanned
            var len = text.length
            while (len > 0 && Character.isWhitespace(text[len - 1])) {
                len--
            }
            if (len < text.length) {
                text = text.subSequence(0, len) as android.text.Spanned
            }

            textView.text = text

            // Apply style dari Compose
            textView.textSize = style.fontSize.value
            textView.setTextColor(color.toArgb())

            // Set line height if available
            style.lineHeight.let { lineHeight ->
                if (lineHeight.value > 0) {
                    textView.setLineSpacing(0f, lineHeight.value / style.fontSize.value)
                }
            }
        }
    )
}
