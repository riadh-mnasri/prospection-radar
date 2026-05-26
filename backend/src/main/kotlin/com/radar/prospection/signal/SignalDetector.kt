package com.radar.prospection.signal

import com.radar.prospection.domain.Signal

interface SignalDetector {
    fun detect(): List<Signal>
}
