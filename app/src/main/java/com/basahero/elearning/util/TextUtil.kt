package com.basahero.elearning.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object TextUtil {
    /**
     * Parses a string containing <b>...</b> tags and returns an AnnotatedString with bold spans.
     */
    fun parseBoldText(text: String): AnnotatedString {
        return buildAnnotatedString {
            val boldRegex = Regex("<b>(.*?)</b>")
            var currentText = text
            val boldRanges = mutableListOf<IntRange>()
            
            var match = boldRegex.find(currentText)
            while (match != null) {
                val fullMatchRange = match.range
                val content = match.groupValues[1]
                
                // Replace tag with content in our tracking string
                currentText = currentText.replaceFirst(match.value, content)
                
                // Store the range where this content will end up
                val start = fullMatchRange.first
                val end = start + content.length
                boldRanges.add(start until end)
                
                match = boldRegex.find(currentText)
            }
            
            append(currentText)
            
            // Apply bold styles
            boldRanges.forEach { range ->
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = range.first,
                    end = range.last + 1
                )
            }
        }
    }
}
