package dev.robotwin.evaldiff;

import java.math.BigDecimal;

public record MetricDelta(String name, BigDecimal baseline, BigDecimal candidate, BigDecimal delta) {
}
