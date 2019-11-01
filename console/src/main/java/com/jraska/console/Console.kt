package com.jraska.console

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import kotlin.concurrent.fixedRateTimer
import kotlin.math.max


/**
 * Console like output view, which allows writing via static console methods
 * from anywhere of application.
 * If you want to see the output, you should use console in any of your layouts,
 * all calls to console static write methods will affect all instantiated consoles.
 */
class Console : FrameLayout {

  companion object {

    @JvmStatic
    fun writeLine() {
      controller.writeLine()
    }

    /**
     * Write provided object String representation to console and starts new line
     * "null" is written if the object is null
     *
     * @param o Object to write
     */
    @JvmStatic
    fun writeLine(o: Any?) {
      controller.writeLine(o)
    }

    /**
     * Write Spannable to console and starts new line
     * "null" is written if the object is null
     *
     * @param spannableString SpannableString to write
     */
    @JvmStatic
    fun writeLine(spannableString: SpannableString?) {
      controller.writeLine(spannableString)
    }

    /**
     * Write provided object String representation to console
     * "null" is written if the object is null
     *
     * @param o Object to write
     */
    @JvmStatic
    fun write(o: Any?) {
      controller.write(o)
    }

    /**
     * Write SpannableString to the console
     * "null" is written if the object is null
     *
     * @param spannableString SpannableString to write
     */
    @JvmStatic
    fun write(spannableString: SpannableString?) {
      controller.write(spannableString)
    }

    /**
     * Clears the console text
     */
    @JvmStatic
    fun clear() {
      controller.clear()
    }

    @JvmStatic
    fun consoleCount(): Int {
      return controller.size()
    }

    internal val controller = ConsoleController()
  }

  private val textView: EditText
  private val scrollView: ScrollView
  private val userTouchingListener = UserTouchingListener()
  private val flingProperty: FlingProperty
  private val scrollDownRunnable = Runnable { scrollFullDown() }
  private val MAX_OUTPUT_LINES = 15

  val text: CharSequence
    get() = textView.text.toString()

  // Fields are used to not schedule more than one runnable for scroll down
  private var fullScrollScheduled: Boolean = false

  private fun isUserInteracting(): Boolean {
    return userTouchingListener.isUserTouching || flingProperty.isFlinging
  }

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

  init {
    controller.add(this)

    LayoutInflater.from(context).inflate(R.layout.console_content, this)

    textView = findViewById(R.id.console_text)
    scrollView = findViewById(R.id.console_scroll_view)
    flingProperty = FlingProperty.create(scrollView)
    scrollView.setOnTouchListener(userTouchingListener)

    printBuffer()
    // need to have extra post here, because scroll view is fully initialized after another frame
    post { scrollDown() }

    val timerInterval = 2000

    fixedRateTimer(
      "consoleTimer",
      initialDelay = timerInterval.toLong(),
      period = timerInterval.toLong()
    ) {
      removeOldLines()
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    fullScrollScheduled = false
  }

  private val maxLines = 500;
  private val lock = Any()

  // remove leading lines from beginning of the output view
  private fun removeOldLines() {
    controller.buffer.clearOldLines(maxLines)
  }

  internal fun printScroll() {
    printBuffer()
    scrollDown()
  }

  private fun printBuffer() {
    controller.printTo(textView)
  }

  private fun scrollDown() {
    if (!isUserInteracting() && !fullScrollScheduled) {
      post(scrollDownRunnable)
      fullScrollScheduled = true
    }
  }

  private fun scrollFullDown() {
    val lastChild = scrollView.getChildAt(scrollView.childCount - 1)
    val bottom = lastChild.bottom + scrollView.paddingBottom
    val sy = scrollView.scrollY
    val sh = scrollView.height
    val delta = bottom - (sy + sh)

    scrollView.smoothScrollBy(0, delta)
  }
}
