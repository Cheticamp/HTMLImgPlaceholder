package com.example.htmlimgplaceholder

// https://stackoverflow.com/questions/64680950/how-to-change-the-default-image-place-holder-loaded-in-a-textview-with-spanned

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.os.Build
import android.os.Bundle
import android.text.*
import android.text.style.MetricAffectingSpan
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var tvBody: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvBody = findViewById(R.id.tv_body)
        val exampleText =
            "Example <br> <img src=\"https://www.w3schools.com/images/w3schools_green.jpg\" alt=\"W3Schools.com\"> <br> Example"
        tvBody.setText(fromHtml(exampleText, this), TextView.BufferType.SPANNABLE)
    }

    private fun fromHtml(html: String?, context: Context): Spannable {
        // Define the ImageGetter for Html. The default "no image, yet" drawable is
        // R.drawable.placeholder but can be another drawable.
        val imageGetter = Html.ImageGetter { url ->
            val dr = ContextCompat.getDrawable(context, R.drawable.placeholder) as BitmapDrawable
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ImageWrapperApi23(dr)
            } else {
                ImageWrapper(dr)
            }.apply {
                // Simulate a network fetch of the real image we want to display.
                simulateNetworkFetch(context, this, url)
            }
        }
        return SpannableString(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, imageGetter, null)
            } else {
                Html.fromHtml(html, imageGetter, null)
            }
        )
    }

    @SuppressLint("NewApi")
    private fun simulateNetworkFetch(context: Context, imageWrapper: Drawable, url: String) {
        GlobalScope.launch {
            Log.d("Applog", "Simulating fetch of $url")
            // Just wait for a busy network to get back to us.
            delay(4000)
            // Get the "downloaded" image and place it in our image wrapper.
            val dr = ContextCompat.getDrawable(context, R.drawable.downloaded) as BitmapDrawable
            if (imageWrapper is ImageWrapper) {
                imageWrapper.setBitmapDrawable(dr)
            } else {
                (imageWrapper as ImageWrapperApi23).drawable = dr
            }
            // Force a remeasure/relayout of the TextView with the new image. invalidate() and
            // requestLayout() should work (?) but don't.
            this@MainActivity.runOnUiThread {
                // This is the quick way but may be expensive.
//                tvBody.text = tvBody.text
                // This is a clever way but really?
                val text = tvBody.text as Spannable
                text.setSpan(ForceLayoutSpan(), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    // Has to extend MetricAffectingSpan to force size change.
    private class ForceLayoutSpan : MetricAffectingSpan() {
        override fun updateDrawState(tp: TextPaint?) {
        }

        override fun updateMeasureState(textPaint: TextPaint) {
        }

    }

    // Simple wrapper for a BitmapDrawable. Use this for API 22-.
    private class ImageWrapper(d: BitmapDrawable) : Drawable() {
        private lateinit var mBitMapDrawable: BitmapDrawable

        init {
            setBitmapDrawable(d)
        }

        override fun draw(canvas: Canvas) {
            mBitMapDrawable.draw(canvas)
        }

        override fun setAlpha(alpha: Int) {
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
        }

        override fun getOpacity(): Int {
            return PixelFormat.OPAQUE
        }

        fun setBitmapDrawable(bitmapDrawable: BitmapDrawable) {
            mBitMapDrawable = bitmapDrawable
            mBitMapDrawable.setBounds(
                0,
                0,
                mBitMapDrawable.intrinsicWidth,
                mBitMapDrawable.intrinsicHeight
            )
            bounds = mBitMapDrawable.bounds
        }
    }

    // Simple wrapper for a BitmapDrawable for API 23+.
    @RequiresApi(Build.VERSION_CODES.M)
    private class ImageWrapperApi23(dr: BitmapDrawable) : DrawableWrapper(dr) {

        init {
            drawable = dr
        }

        override fun setDrawable(dr: Drawable?) {
            dr?.also {
                bounds = Rect(0, 0, it.intrinsicWidth, it.intrinsicHeight)
            }
            super.setDrawable(dr)
        }
    }
}