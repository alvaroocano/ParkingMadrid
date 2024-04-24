package com.example.parkingmadrid
import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.util.AttributeSet
import android.view.View
import java.io.InputStream

class GifView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var movie: Movie? = null
    private var movieStart: Long = 0

    init {
        val inputStream: InputStream = context.resources.openRawResource(R.raw.parking)
        movie = Movie.decodeStream(inputStream)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = android.os.SystemClock.uptimeMillis()
        if (movieStart == 0L) {
            movieStart = now
        }
        movie?.let {
            val duration = it.duration()
            val relTime = ((now - movieStart) % duration).toInt()
            it.setTime(relTime)
            it.draw(canvas, 0f, 0f)
            invalidate()
        }
    }
}

