package model;

import java.time.Instant;
import java.util.Objects;

/*
    Наблюдение — неизменяемое значение.
    Время хранится как Instant, что позволяет корректно сравнивать наблюдения
    станций в разных часовых поясах «в один и тот же момент».
 */
public record Observation(
        Instant time,
        int temperatureCelsius,
        int pressureHpa,
        int windSpeedMs,
        WindDirection windDirection,
        WeatherPhenomenon phenomenon
) implements Describable {

    public Observation {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(windDirection, "windDirection");
        Objects.requireNonNull(phenomenon, "phenomenon");
        if (pressureHpa <= 0)     throw new IllegalArgumentException("pressureHpa must be > 0");
        if (windSpeedMs < 0)      throw new IllegalArgumentException("windSpeedMs must be >= 0");
    }

    @Override
    public String describe() {
        return "%s  %+d°C  %d hPa  %d m/s %s  %s".formatted(
                time, temperatureCelsius, pressureHpa, windSpeedMs, windDirection, phenomenon);
    }
}