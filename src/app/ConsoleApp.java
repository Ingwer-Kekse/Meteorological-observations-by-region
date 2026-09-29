package app;

import model.*;
import service.WeatherService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

public final class ConsoleApp {

    public static void run() {
        var moscow = ZoneId.of("Europe/Moscow");
        var tokyo  = ZoneId.of("Asia/Tokyo");

        var district = new District("Central");
        var msk = new Station("MSK-01", "Moscow Center", moscow);
        var tyo = new Station("TYO-01", "Tokyo Center",  tokyo);

        String[] mskLines = {
                "2024-01-15T00:00;-10;1020;3;N;CLEAR",
                "2024-01-15T06:00;-8;1018;2;NE;CLOUDY",
                "2024-01-15T12:00;-3;1015;5;S;SNOW",
                "2024-01-15T18:00;-5;1016;4;SW;SNOW",
                "2024-01-16T12:00;2;1010;6;W;RAIN",
                "2024-01-17T12:00;8;1008;3;NW;CLEAR"
        };
        String[] tyoLines = {
                // 18:00 Tokyo == 09:00 UTC  => тот же момент, что и 12:00 Moscow
                "2024-01-15T18:00;4;1012;4;SE;RAIN",
                "2024-01-16T09:00;6;1010;3;E;CLEAR",
                "2024-01-17T18:00;10;1005;5;SW;THUNDERSTORM"
        };

        try {
            WeatherService.parseObservations(mskLines, moscow).forEach(msk::addObservation);
            WeatherService.parseObservations(tyoLines, tokyo).forEach(tyo::addObservation);
        } catch (ObservationParseException e) {
            System.err.println("Parse error: " + e.getMessage());
            return;
        }

        district.addStation(msk);
        district.addStation(tyo);

        System.out.println(district.describe());
        district.stations().forEach(s -> System.out.println("  " + s.describe()));
        System.out.println();

        // Все наблюдения
        System.out.println("== Все наблюдения ==");
        for (Station s : district.stations()) {
            System.out.println("-- " + s.code() + " --");
            for (Observation o : s.observations()) {
                System.out.println("   " + o.describe());
            }
        }
        System.out.println();

        Instant from = ZonedDateTime.of(2024, 1, 15, 0, 0, 0, 0, moscow).toInstant();
        Instant to   = ZonedDateTime.of(2024, 1, 18, 0, 0, 0, 0, moscow).toInstant();

        var stats = WeatherService.temperatureStats(district, from, to);
        System.out.println("Минимум/максимум/среднее за период: " + stats);

        System.out.println("\nСуточный ход температуры (Europe/Moscow):");
        WeatherService.dailyTemperatureCourse(district, moscow, from, to)
                .forEach((d, t) -> System.out.printf("   %s -> %.2f°C%n", d, t));

        System.out.println("\nДни с осадками: "
                + WeatherService.daysWithPrecipitation(district, moscow, from, to));

        System.out.println("Аномальные дни (|Δ| > 3°C): "
                + WeatherService.anomalousDays(district, moscow, from, to, 3.0));

        // Демонстрация собственного исключения
        System.out.println("\n== Демонстрация ошибки парсинга ==");
        String[] bad = {
                "2024-01-15T00:00;-5;1020;3;N;CLEAR",
                "2024-01-15T06:00;abc;1020;3;N;CLEAR"
        };
        try {
            WeatherService.parseObservations(bad, moscow);
        } catch (ObservationParseException e) {
            System.err.println("Поймано: " + e.getMessage()
                    + " (строка №" + e.lineNumber() + ")");
        }

        System.out.println("\n== Полиморфизм (Describable) ==");
        Describable[] items = {
                district,
                msk,
                msk.observations().get(0),
                Arrays.stream(new Describable[]{tyo}).findFirst().orElseThrow()
        };
        for (var it : items) System.out.println(it.describe());
    }
}