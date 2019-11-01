package com.jraska.console

import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.TextView
import java.lang.Thread.sleep

internal class ConsoleBuffer {
  private val lock = Any()

  private val buffer = SpannableStringBuilder()
  private var maxBufferSize = MAX_BUFFER_SIZE

  /**
   * @return true if buffer content changed
   */
  fun setSize(maxBufferSize: Int): Boolean {
    synchronized(lock) {
      val bufferChange = maxBufferSize < buffer.length
      this.maxBufferSize = maxBufferSize

      ensureSize()
      return bufferChange
    }
  }

  fun append(o: Any?): ConsoleBuffer {
    return append(o?.toString())
  }

  fun append(charSequence: CharSequence?): ConsoleBuffer {
    val toAppend = charSequence ?: "null"

    synchronized(lock) {
      buffer.append(toAppend)
      ensureSize()
    }
    return this
  }

  fun clear(): ConsoleBuffer {
    buffer.clear()
    return this
  }

  fun clearOldLines(maxLines: Int) {
    Handler(Looper.getMainLooper()).post(Runnable {
    synchronized(lock) {
      var spannableString = buffer.toString()
      // Erase excessive lines
      var lines = spannableString.lines()
      val excessLineNumber = lines.count() - maxLines
      if (excessLineNumber > 0) {

        var eolIndex = 0;

        for (i in 0..excessLineNumber) {
          val lastIndex = lines[i].lastIndex + 2 // for the new line "\n"
          Log.i("hey", "line $i: lastIndex: $lastIndex")
          eolIndex += lastIndex
        }

        if (eolIndex == 0) {
          //nothing to delete
        } else {
          // we can delete everything from start to that index:

          buffer.delete(0, eolIndex)
        }
      }
    }
    })
  }

  fun printTo(textView: TextView): ConsoleBuffer {
    synchronized(lock) {
      textView.text = buffer
    }

    return this
  }

  private fun ensureSize() {
    if (buffer.length > maxBufferSize) {
      val requiredReplacedCharacters = buffer.length - maxBufferSize
      buffer.replace(0, requiredReplacedCharacters, "")
    }
  }

  companion object {
    const val MAX_BUFFER_SIZE = 16000
  }
}
