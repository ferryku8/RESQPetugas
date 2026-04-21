package com.uxonauts.resqadmin.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

object MapHelpers {

    /**
     * Membuat marker pin modern berbentuk teardrop (seperti Google Maps)
     * dengan lingkaran berwarna di tengah.
     */
    fun createPinMarker(
        context: Context,
        fillColor: Int,
        innerDotColor: Int = Color.WHITE
    ): Drawable {
        val width = 100
        val height = 130
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = width / 2f
        val circleRadius = 38f
        val circleCenterY = circleRadius + 8f
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        shadowPaint.color = Color.BLACK
        shadowPaint.alpha = 80
        canvas.drawOval(
            centerX - 18, height - 18f,
            centerX + 18, height - 4f,
            shadowPaint
        )
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        pinPaint.color = fillColor
        pinPaint.style = Paint.Style.FILL

        val path = Path()
        path.moveTo(centerX, height - 8f)
        path.lineTo(centerX - 22, circleCenterY + 12)
        path.lineTo(centerX + 22, circleCenterY + 12)
        path.close()
        canvas.drawPath(path, pinPaint)
        canvas.drawCircle(centerX, circleCenterY, circleRadius, pinPaint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.color = Color.WHITE
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 6f
        canvas.drawCircle(centerX, circleCenterY, circleRadius, borderPaint)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        dotPaint.color = innerDotColor
        canvas.drawCircle(centerX, circleCenterY, 12f, dotPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    /**
     * Marker bulat sederhana untuk posisi petugas (seperti dot biru di Google Maps)
     */
    fun createDotMarker(
        context: Context,
        fillColor: Int
    ): Drawable {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        haloPaint.color = fillColor
        haloPaint.alpha = 60
        canvas.drawCircle(center, center, center - 4, haloPaint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.color = Color.WHITE
        canvas.drawCircle(center, center, center - 14, borderPaint)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fillPaint.color = fillColor
        canvas.drawCircle(center, center, center - 18, fillPaint)

        return BitmapDrawable(context.resources, bitmap)
    }
}