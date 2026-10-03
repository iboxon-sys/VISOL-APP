package com.example.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Bulletproof Phone Number Input Mask for Uzbekistan (+998 XX XXX XX XX).
 * Strictly guarantees that no negative index or OutOfBounds index is ever returned to Compose.
 */
class PhoneNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        val digits = if (raw.length > 9) raw.substring(0, 9) else raw

        val prefix = "+998 "
        val sb = StringBuilder(prefix)

        for (i in digits.indices) {
            sb.append(digits[i])
            if (i == 1 || i == 4 || i == 6) {
                sb.append(" ")
            }
        }

        val formatted = sb.toString()
        val origLength = text.text.length
        val formattedLength = formatted.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, origLength)
                var trans = prefix.length // Start after "+998 "
                for (i in 0 until clamped) {
                    if (i < 9) {
                        trans += 1
                        if (i == 1 || i == 4 || i == 6) {
                            trans += 1
                        }
                    }
                }
                return trans.coerceIn(0, formattedLength)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= prefix.length) {
                    return 0
                }
                var orig = 0
                var currentTrans = prefix.length
                for (i in 0 until 9) {
                    if (currentTrans >= offset) break
                    currentTrans += 1
                    orig += 1
                    if (i == 1 || i == 4 || i == 6) {
                        if (currentTrans >= offset) break
                        currentTrans += 1
                    }
                }
                return orig.coerceIn(0, origLength)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
