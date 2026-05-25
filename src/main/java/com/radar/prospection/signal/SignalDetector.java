package com.radar.prospection.signal;

import com.radar.prospection.domain.Signal;

import java.util.List;

public interface SignalDetector {
    List<Signal> detect();
}
