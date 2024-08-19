package com.example.facerecognition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint().apply {
        color = 0xFFFF0000.toInt() // Red color
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val facesBoundingBoxes = mutableListOf<RectF>()

    fun setFaces(faces: List<RectF>) {
        facesBoundingBoxes.clear()
        facesBoundingBoxes.addAll(faces)
        invalidate() // Redraw the view with updated face boxes
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (box in facesBoundingBoxes) {
            canvas.drawRect(box, paint)
        }
    }
}
