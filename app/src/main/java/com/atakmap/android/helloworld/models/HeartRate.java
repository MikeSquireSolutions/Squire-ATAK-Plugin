package com.atakmap.android.helloworld.models;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class HeartRate {
    UUID patientUuid;
    Date timestamp;
    int heartRate;

    public HeartRate(UUID uuid, int hr) {
        this.heartRate = hr;
        this.patientUuid = uuid;
        this.timestamp = Date.from(Instant.now());
    }

    public UUID getPatientUuid() {
        return this.patientUuid;
    }

    public int getValue() {
        return heartRate;
    }
}
