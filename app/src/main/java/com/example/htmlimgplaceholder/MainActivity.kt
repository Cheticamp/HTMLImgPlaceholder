package com.example.htmlimgplaceholder

// https://stackoverflow.com/questions/64680950/how-to-change-the-default-image-place-holder-loaded-in-a-textview-with-spanned

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var tvBody: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvBody = findViewById(R.id.tv_body)
        val exampleText =
            "W3 Schools Image<br><img src=\"https://www.w3schools.com/images/w3schools_green.jpg\"><br>Google Image<br><img src=\"https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png\">"
        tvBody.setText(fromHtml(exampleText, this), TextView.BufferType.SPANNABLE)
    }

    private var delaySeconds = MAX_DELAY
    private var downloadIndex = 0
    private val downloadedImages = intArrayOf(R.drawable.downloaded_1, R.drawable.downloaded_2)

    @Suppress("DEPRECATION", "SameParameterValue")
    private fun fromHtml(html: String?, context: Context): Spannable {
        // Define the ImageGetter for Html. The default "no image, yet" drawable is
        // R.drawable.placeholder but can be another drawable.
        val placeHolderImage =
            BitmapFactory.decodeResource(context.resources, R.drawable.placeholder)

        val imageGetter = Html.ImageGetter { url ->
            // Simulate a network fetch of the real image we want to display.
            val placeHolder = PlaceHolder(context, placeHolderImage, 100, 100)
            simulateNetworkFetch(
                context,
                placeHolder,
                url,
                delaySeconds,
                downloadedImages[downloadIndex]
            )
            // Stagger multiple fetches.
            delaySeconds -= 2
            if (delaySeconds <= 0) {
                delaySeconds = MAX_DELAY
            }
            downloadIndex = (downloadIndex + 1) % 2
            placeHolder
        }
        return SpannableString(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, imageGetter, null)
            } else {
                @Suppress("SameParameterValue")
                Html.fromHtml(html, imageGetter, null)
            }
        )
    }

    @SuppressLint("NewApi")
    private fun simulateNetworkFetch(
        context: Context,
        placeHolder: Drawable,
        url: String,
        seconds: Long,
        @DrawableRes imageToShow: Int
    ) {
        launch {
            Log.d("Applog", "Simulating fetch of $url")
            // Just wait for a busy network to get back to us.
            delay(seconds * 1000)
            // Get the "downloaded" image and place it in our image wrapper.
            val downloadedImage =
                BitmapFactory.decodeResource(context.resources, imageToShow)
            // Force a remeasure/relayout of the TextView with the new image.
            this@MainActivity.runOnUiThread {
                replaceSpan(context, tvBody.text as Spannable, placeHolder, downloadedImage)
            }
        }
    }

    // Replace the span that is holding the placeholder with the span of the fetched image.
    private fun replaceSpan(
        context: Context,
        text: Spannable,
        placeHolder: Drawable,
        bitmap: Bitmap
    ) {
        val imageSpan = text.getSpans(0, text.length, ImageSpan::class.java)
            .first { it.drawable == placeHolder } ?: return
        text.apply {
            val spanStart = getSpanStart(imageSpan)
            val spanEnd = getSpanEnd(imageSpan)
            val spanFlags = getSpanFlags(imageSpan)
            removeSpan(imageSpan)
            setSpan(ImageSpan(context, bitmap), spanStart, spanEnd, spanFlags)
        }
    }

    companion object {
        private const val MAX_DELAY = 4L
    }

    class PlaceHolder(context: Context, bitmap: Bitmap, width: Int, height: Int) :
        BitmapDrawable(context.resources, bitmap) {

        init {
            setBounds(0, 0, width, height)
        }
    }
}