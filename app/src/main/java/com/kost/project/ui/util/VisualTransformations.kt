package com.kost.project.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.*

class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val formattedText = try {
            val number = originalText.toLong()
            val formatter = NumberFormat.getInstance(Locale.forLanguageTag("id-ID"))
            formatter.format(number)
        } catch (_: Exception) {
            originalText
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val sub = originalText.substring(0, offset.coerceAtMost(originalText.length))
                val formattedSub = try {
                    val number = sub.toLong()
                    val formatter = NumberFormat.getInstance(Locale.forLanguageTag("id-ID"))
                    formatter.format(number)
                } catch (_: Exception) {
                    sub
                }
                return formattedSub.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                var originalOffset = 0
                var transformedOffset = 0
                while (originalOffset < originalText.length && transformedOffset < offset) {
                    transformedOffset++
                    if (formattedText.getOrNull(transformedOffset - 1) != '.') {
                        originalOffset++
                    }
                }
                return originalOffset
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
