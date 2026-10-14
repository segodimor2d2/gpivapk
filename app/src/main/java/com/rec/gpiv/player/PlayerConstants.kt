package com.rec.gpiv.player

const val SEEK_FRAMES = 1
const val SEEK_FRAMES_PLUS = 20
const val SEEK_SECONDS = 1.0

const val FILE_JUMP = 10

const val MAX_VOLUME = 300.0
const val VOLUME_STEP = 5.0

const val BRIGHTNESS_STEP = 10.0
const val CONTRAST_STEP = 10.0
const val GAMMA_STEP = 10.0
const val SATURATION_STEP = 10.0

const val BRIGHTNESS_PROP = 80.0
const val CONTRAST_PROP = 80.0
const val GAMMA_PROP = 80.0
const val SATURATION_PROP = 80.0

// Sensibilidade de bordas: valores maiores detectam mais contornos.
const val EDGE_DETECTION_DEFAULT = 0.0
const val EDGE_DETECTION_MIN = 0.0
const val EDGE_DETECTION_MAX = 5.0
// Variação ao arrastar uma distância equivalente à altura da área de gestos.
const val EDGE_DETECTION_PROP = 4.0

// Zero desliga o filtro. MIN/MAX = limites; PROP = sensibilidade do arraste.
const val NOISE_REDUCTION_DEFAULT = 0.0
const val NOISE_REDUCTION_MIN = 0.0
const val NOISE_REDUCTION_MAX = 12.0
const val NOISE_REDUCTION_PROP = 8.0

const val SHARPNESS_DEFAULT = 0.0
const val SHARPNESS_MIN = 0.0
const val SHARPNESS_MAX = 5.0
const val SHARPNESS_PROP = 4.0

const val BLUR_DEFAULT = 0.0
const val BLUR_MIN = 0.0
const val BLUR_MAX = 20.0
const val BLUR_PROP = 10.0

const val DEBAND_DEFAULT = 0.0
const val DEBAND_MIN = 0.0
const val DEBAND_MAX = 0.15
const val DEBAND_PROP = 0.1

const val ZOOM_DOUBLE_TAP_DRAG = 6.0

const val SEEK_GESTURE_SENSITIVITY = 40.0
