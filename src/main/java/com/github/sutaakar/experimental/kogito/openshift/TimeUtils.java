package com.github.sutaakar.experimental.kogito.openshift;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

import org.apache.maven.plugin.logging.Log;

public class TimeUtils {

    private Log log;

    public TimeUtils(Log log) {
        this.log = log;
    }

    public void wait(Duration maxDuration, Duration waitStep, BooleanSupplier booleanSupplier) {
        Instant timeout = Instant.now().plus(maxDuration);
        while (timeout.isAfter(Instant.now()) && !booleanSupplier.getAsBoolean()) {
            wait(waitStep);
        }
    }

    public void wait(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            log.warn("Interrupted!", e);
            Thread.currentThread().interrupt();
        }
    }
}
