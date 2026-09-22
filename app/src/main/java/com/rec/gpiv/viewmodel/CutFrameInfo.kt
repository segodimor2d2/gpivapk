package com.rec.gpiv.viewmodel

data class CutFrameInfo(
    val frameA: Long,
    val ptsA: Long,
    val frameB: Long,
    val ptsB: Long,
    val timeBaseNum: Long,
    val timeBaseDen: Long
) {
    val timeA: Double
        get() = ptsA.toDouble() * timeBaseNum / timeBaseDen

    val timeB: Double
        get() = ptsB.toDouble() * timeBaseNum / timeBaseDen

    val duration: Double
        get() = timeB - timeA
}
