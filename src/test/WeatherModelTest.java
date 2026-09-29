package test;

import org.junit.jupiter.api.Test;
import model.*;
import service.WeatherService;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeatherModelTest {

    private static final ZoneId MSK = ZoneId.of("Europe/Moscow");
    private static final ZoneId TOK = ZoneId.of("Asia/Tokyo");

    // 1. Румб по азимуту — основные направления
    @Test
    void windDirection_cardinalPoints() {
        assertEquals(WindDirection.N,  WindDirection.fromAzimuth(0));
        assertEquals(WindDirection.E,  WindDirection.fromAzimuth(90));
        assertEquals(WindDirection.S,  WindDirection.fromAzimuth(180));
        assertEquals(WindDirection.W,  WindDirection.fromAzimuth(270));
    }

    // 2. Границы диапазонов
    @Test
    void windDirection_boundaries() {
        assertEquals(WindDirection.N,  WindDirection.fromAzimuth(22.4));
        assertEquals(WindDirection.NE, WindDirection.fromAzimuth(22.5));
        assertEquals(WindDirection.NE, WindDirection.fromAzimuth(67.4));
        assertEquals(WindDirection.E,  WindDirection.fromAzimuth(67.5));
        assertEquals(WindDirection.N,  WindDirection.fromAzimuth(337.5));
        assertEquals(WindDirection.NW, WindDirection.fromAzimuth(337.4));
    }

    // 3. Нормализация отрицательных и >360 значений
    @Test
    void windDirection_normalization() {
        assertEquals(WindDirection.N, WindDirection.fromAzimuth(-10));
        assertEquals(WindDirection.E, WindDirection.fromAzimuth(450));
        assertEquals(WindDirection.S, WindDirection.fromAzimuth(540));
    }

    // 4. Успешный парсинг массива строк
    @Test
    void parseObservations_valid() throws Exception {
        String[] lines = {
                "2024-01-15T12:00;-3;1015;5;S;SNOW",
                "2024-01-15T18:00;-5;1016;4;SW;RAIN"
        };
        var obs = WeatherService.parseObservations(lines, MSK);
        assertEquals(2, obs.size());
        assertEquals(-3, obs.get(0).temperatureCelsius());
        assertEquals(WeatherPhenomenon.SNOW, obs.get(0).phenomenon());
    }

    // 5. Ошибка парсинга несёт номер строки
    @Test
    void parseObservations_reportsLineNumber() {
        String[] lines = {
                "2024-01-15T00:00;-5;1020;3;N;CLEAR",
                "2024-01-15T06:00;abc;1020;3;N;CLEAR"
        };
        var ex = assertThrows(ObservationParseException.class,
                () -> WeatherService.parseObservations(lines, MSK));
        assertEquals(2, ex.lineNumber());
        assertTrue(ex.getMessage().contains("Line 2"));
    }

    // 6. Ошибка парсинга — неверное число полей
    @Test
    void parseObservations_badFieldCount() {
        String[] lines = {"2024-01-15T00:00;-5;1020"};
        var ex = assertThrows(ObservationParseException.class,
                () -> WeatherService.parseObservations(lines, MSK));
        assertEquals(1, ex.lineNumber());
    }

    // 7. Мин/макс/среднее
    @Test
    void stats_minMaxAverage() throws Exception {
        var d = new District("D");
        var s = new Station("S1", "S1", MSK);
        String[] lines = {
                "2024-01-15T00:00;-10;1020;3;N;CLEAR",
                "2024-01-15T12:00;0;1015;3;N;CLEAR",
                "2024-01-15T18:00;10;1010;3;N;CLEAR"
        };
        WeatherService.parseObservations(lines, MSK).forEach(s::addObservation);
        d.addStation(s);
        var st = WeatherService.temperatureStats(d, Instant.MIN, Instant.MAX);
        assertEquals(-10, st.minCelsius());
        assertEquals(10,  st.maxCelsius());
        assertEquals(0.0, st.averageCelsius(), 1e-9);
    }

    // 8. Пустой диапазон -> исключение
    @Test
    void stats_emptyRangeThrows() {
        var d = new District("D");
        assertThrows(IllegalStateException.class,
                () -> WeatherService.temperatureStats(d, Instant.MIN, Instant.MAX));
    }

    // 9. Суточный ход зависит от часового пояса
    @Test
    void dailyCourse_respectsZone() throws Exception {
        Instant t = Instant.parse("2024-01-15T22:00:00Z");
        var obs = new Observation(t, 5, 1010, 3, WindDirection.N, WeatherPhenomenon.CLEAR);
        var d = new District("D");
        var s = new Station("S1", "S1", MSK);
        s.addObservation(obs);
        d.addStation(s);

        var utc = WeatherService.dailyTemperatureCourse(d, ZoneId.of("UTC"),
                Instant.MIN, Instant.MAX);
        var msk = WeatherService.dailyTemperatureCourse(d, MSK,
                Instant.MIN, Instant.MAX);

        assertTrue(utc.containsKey(LocalDate.of(2024, 1, 15)));
        assertTrue(msk.containsKey(LocalDate.of(2024, 1, 16)));
    }

    // 10. Дни с осадками
    @Test
    void daysWithPrecipitation_filterCorrectly() throws Exception {
        var d = new District("D");
        var s = new Station("S1", "S1", MSK);
        String[] lines = {
                "2024-01-15T10:00;0;1010;3;N;CLEAR",
                "2024-01-16T10:00;1;1010;3;N;RAIN",
                "2024-01-17T10:00;2;1010;3;N;SNOW",
                "2024-01-17T18:00;2;1010;3;N;CLOUDY"
        };
        WeatherService.parseObservations(lines, MSK).forEach(s::addObservation);
        d.addStation(s);

        var days = WeatherService.daysWithPrecipitation(d, MSK, Instant.MIN, Instant.MAX);
        assertEquals(2, days.size());
        assertTrue(days.contains(LocalDate.of(2024, 1, 16)));
        assertTrue(days.contains(LocalDate.of(2024, 1, 17)));
        assertFalse(days.contains(LocalDate.of(2024, 1, 15)));
    }

    // 11. Аномальные дни
    @Test
    void anomalousDays_detects() throws Exception {
        var d = new District("D");
        var s = new Station("S1", "S1", MSK);
        String[] lines = {
                "2024-01-15T12:00;0;1010;3;N;CLEAR",
                "2024-01-16T12:00;1;1010;3;N;CLEAR",
                "2024-01-17T12:00;20;1010;3;N;CLEAR"
        };
        WeatherService.parseObservations(lines, MSK).forEach(s::addObservation);
        d.addStation(s);
        var days = WeatherService.anomalousDays(d, MSK, Instant.MIN, Instant.MAX, 5.0);
        assertEquals(List.of(LocalDate.of(2024, 1, 17)), days);
    }

    // 12. Один момент — разные зоны — одинаковый Instant
    @Test
    void sameMomentInDifferentZones() throws Exception {
        var msk = WeatherService.parseObservations(
                new String[]{"2024-01-15T12:00;0;1010;3;N;CLEAR"}, MSK);
        var tok = WeatherService.parseObservations(
                new String[]{"2024-01-15T18:00;0;1010;3;N;CLEAR"}, TOK);
        assertEquals(msk.get(0).time(), tok.get(0).time());
    }

    // 13. equals/hashCode контракт Station
    @Test
    void station_equalsHashCodeContract() {
        var a = new Station("X1", "Name A", MSK);
        var b = new Station("X1", "Name B", TOK); // тот же код — тот же идентификатор
        var c = new Station("X2", "Name A", MSK);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "X1");
    }

    // 14. Список наблюдений — неизменяемый
    @Test
    void station_observationsListIsImmutable() {
        var s = new Station("S1", "S1", MSK);
        s.addObservation(new Observation(Instant.now(), 0, 1010, 1,
                WindDirection.N, WeatherPhenomenon.CLEAR));
        var list = s.observations();
        assertThrows(UnsupportedOperationException.class,
                () -> list.add(null));
    }

    // 15. Список наблюдений — защитная копия
    @Test
    void station_observationsIsDefensiveCopy() {
        var s = new Station("S1", "S1", MSK);
        var o1 = new Observation(Instant.now(), 0, 1010, 1,
                WindDirection.N, WeatherPhenomenon.CLEAR);
        s.addObservation(o1);

        var snapshot = s.observations();
        s.addObservation(new Observation(Instant.now(), 5, 1010, 1,
                WindDirection.N, WeatherPhenomenon.CLEAR));

        assertEquals(1, snapshot.size()); // старый снимок не изменился
        assertEquals(2, s.observations().size());
    }

    // 16. Полиморфизм через Describable
    @Test
    void polymorphicDescribe() {
        Describable[] items = {
                new District("D"),
                new Station("S", "S", MSK),
                new Observation(Instant.parse("2024-01-15T12:00:00Z"),
                        0, 1010, 3, WindDirection.N, WeatherPhenomenon.CLEAR)
        };
        for (Describable d : items) {
            assertNotNull(d.describe());
            assertFalse(d.describe().isBlank());
        }
    }
}