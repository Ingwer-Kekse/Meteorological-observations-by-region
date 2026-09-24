package service;

import model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;

public final class WeatherService {

    private WeatherService() {}

    // ---------- Парсинг ----------
    public static List<Observation> parseObservations(String[] lines, ZoneId zone)
            throws ObservationParseException {
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(zone, "zone");
        var result = new ArrayList<Observation>(lines.length);
        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String raw = lines[i];
            try {
                result.add(parseLine(raw, zone));
            } catch (RuntimeException e) {
                throw new ObservationParseException(lineNo, raw, e.getMessage(), e);
            }
        }
        return List.copyOf(result);
    }

    private static Observation parseLine(String raw, ZoneId zone) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("empty line");
        }
        String[] parts = raw.split(";");
        if (parts.length != 6) {
            throw new IllegalArgumentException("expected 6 fields, got " + parts.length);
        }
        LocalDateTime ldt = LocalDateTime.parse(parts[0].trim());
        Instant instant = ldt.atZone(zone).toInstant();
        int temp     = Integer.parseInt(parts[1].trim());
        int pressure = Integer.parseInt(parts[2].trim());
        int wind     = Integer.parseInt(parts[3].trim());
        WindDirection dir  = WindDirection.parse(parts[4]);
        WeatherPhenomenon ph = WeatherPhenomenon.parse(parts[5]);
        return new Observation(instant, temp, pressure, wind, dir, ph);
    }

}