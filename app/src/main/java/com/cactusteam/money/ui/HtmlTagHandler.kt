package com.cactusteam.money.ui

import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan

import org.xml.sax.XMLReader

/**
 * @author vpotapenko
 */
class HtmlTagHandler : Html.TagHandler {

    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        if (tag.equals("strike", ignoreCase = true) || tag == "s") {
            if (opening) {
                start(output as SpannableStringBuilder, Strike())
            } else {
                end(output as SpannableStringBuilder, Strike::class.java, StrikethroughSpan())
            }
        }
    }

    private fun <T> getLast(text: Spanned, kind: Class<T>): Any? {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        val objs = text.getSpans(0, text.length, kind)

        if (objs.isEmpty()) {
            return null
        } else {
            return objs[objs.size - 1]
        }
    }

    private fun start(text: SpannableStringBuilder, mark: Any) {
        val len = text.length
        text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK)
    }

    private fun <T> end(text: SpannableStringBuilder, kind: Class<T>,
                        repl: Any) {
        val len = text.length
        val obj = getLast(text, kind)
        val where = text.getSpanStart(obj)

        text.removeSpan(obj)

        if (where != len) {
            text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private class Strike
}
