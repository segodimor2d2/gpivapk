package com.rec.gpiv.model

/** Coordinates normalized to the image or viewport, from 0 to 1. */
data class CropRegion(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun toArray() = doubleArrayOf(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
}
