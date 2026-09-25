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

    // аналитика

    public static TemperatureStats temperatureStats(District d, Instant from, Instant to) {
        var temps = new ArrayList<Integer>();
        forEachObservation(d, from, to, o -> temps.add(o.temperatureCelsius()));
        if (temps.isEmpty()) {
            throw new IllegalStateException("No observations in the given range");
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long sum = 0;
        for (int t : temps) {
            if (t < min) min = t;
            if (t > max) max = t;
            sum += t;
        }
        return new TemperatureStats(min, max, (double) sum / temps.size());
    }

    // суточный ход температуры (среднее по дням) в заданном часовом поясе
    public static Map<LocalDate, Double> dailyTemperatureCourse(
            District d, ZoneId zone, Instant from, Instant to) {

        Map<LocalDate, int[]> acc = new TreeMap<>(); // [sum, count]
        forEachObservation(d, from, to, o -> {
            LocalDate date = o.time().atZone(zone).toLocalDate();
            int[] a = acc.computeIfAbsent(date, k -> new int[2]);
            a[0] += o.temperatureCelsius();
            a[1]++;
        });
        Map<LocalDate, Double> result = new TreeMap<>();
        acc.forEach((k, v) -> result.put(k, (double) v[0] / v[1]));
        return Collections.unmodifiableMap(result);
    }

    public static Set<LocalDate> daysWithPrecipitation(
            District d, ZoneId zone, Instant from, Instant to) {

        Set<LocalDate> result = new TreeSet<>();
        forEachObservation(d, from, to, o -> {
            if (o.phenomenon().isPrecipitation()) {
                result.add(o.time().atZone(zone).toLocalDate());
            }
        });
        return Collections.unmodifiableSet(result);
    }

    public static List<LocalDate> anomalousDays(
            District d, ZoneId zone, Instant from, Instant to, double threshold) {

        var daily = dailyTemperatureCourse(d, zone, from, to);
        if (daily.isEmpty()) return List.of();
        double norm = daily.values().stream()
                .mapToDouble(Double::doubleValue).average().orElseThrow();
        return daily.entrySet().stream()
                .filter(e -> Math.abs(e.getValue() - norm) > threshold)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static void forEachObservation(District d, Instant from, Instant to,
                                           Consumer<Observation> action) {
        Objects.requireNonNull(d, "district");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' is before 'from'");
        }
        for (Station st : d.stations()) {
            for (Observation o : st.observations()) {
                if (!o.time().isBefore(from) && !o.time().isAfter(to)) {
                    action.accept(o);
                }
            }
        }
    }
}