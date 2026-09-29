package app;

import model.*;
import service.WeatherService;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;

public class WeatherFrame extends JFrame {

    private final JTextArea inputArea = new JTextArea(12, 60);
    private final JTextField zoneField = new JTextField("Europe/Moscow", 18);
    private final JTextArea outputArea = new JTextArea(18, 60);
    private final District district = new District("Demo District");

    public WeatherFrame() {
        super("Метеонаблюдения");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        inputArea.setText("""
                2024-01-15T00:00;-10;1020;3;N;CLEAR
                2024-01-15T06:00;-8;1018;2;NE;CLOUDY
                2024-01-15T12:00;-3;1015;5;S;SNOW
                2024-01-15T18:00;-5;1016;4;SW;SNOW
                2024-01-16T12:00;2;1010;6;W;RAIN
                2024-01-17T12:00;8;1008;3;NW;CLEAR
                """);
        outputArea.setEditable(false);

        var top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("ZoneId:"));
        top.add(zoneField);
        var loadBtn = new JButton("Загрузить наблюдения");
        loadBtn.addActionListener(e -> onLoad());
        top.add(loadBtn);

        var mid = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mid.add(button("Мин/макс/среднее",    e -> onStats()));
        mid.add(button("Суточный ход",         e -> onDaily()));
        mid.add(button("Дни с осадками",       e -> onRainy()));
        mid.add(button("Аномальные дни",       e -> onAnomaly()));

        var center = new JPanel(new BorderLayout(4, 4));
        center.add(new JScrollPane(inputArea), BorderLayout.NORTH);
        center.add(mid, BorderLayout.CENTER);
        center.add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    private JButton button(String text, java.awt.event.ActionListener l) {
        var b = new JButton(text);
        b.addActionListener(l);
        return b;
    }

    private void onLoad() {
        try {
            ZoneId zone = ZoneId.of(zoneField.getText().trim());
            String[] lines = inputArea.getText().split("\\R");
            var station = new Station("GUI-01", "GUI Station", zone);
            WeatherService.parseObservations(lines, zone).forEach(station::addObservation);
            district.addStation(station);
            append("Загружено наблюдений: " + station.observations().size()
                    + " (" + zone + ")");
        } catch (ObservationParseException e) {
            append("Ошибка разбора: " + e.getMessage());
        } catch (RuntimeException e) {
            append("Ошибка: " + e.getMessage());
        }
    }

    private void onStats() {
        try {
            var s = WeatherService.temperatureStats(district, Instant.MIN, Instant.MAX);
            append("Статистика: " + s);
        } catch (RuntimeException e) { append("Ошибка: " + e.getMessage()); }
    }

    private void onDaily() {
        try {
            ZoneId zone = ZoneId.of(zoneField.getText().trim());
            var map = WeatherService.dailyTemperatureCourse(district, zone, Instant.MIN, Instant.MAX);
            append("Суточный ход (" + zone + "):");
            map.forEach((d, t) -> append("   " + d + " -> " + "%.2f".formatted(t) + "°C"));
        } catch (RuntimeException e) { append("Ошибка: " + e.getMessage()); }
    }

    private void onRainy() {
        try {
            ZoneId zone = ZoneId.of(zoneField.getText().trim());
            var days = WeatherService.daysWithPrecipitation(district, zone, Instant.MIN, Instant.MAX);
            append("Дни с осадками: " + days);
        } catch (RuntimeException e) { append("Ошибка: " + e.getMessage()); }
    }

    private void onAnomaly() {
        try {
            ZoneId zone = ZoneId.of(zoneField.getText().trim());
            var days = WeatherService.anomalousDays(district, zone, Instant.MIN, Instant.MAX, 3.0);
            append("Аномальные дни (|Δ| > 3°C): " + days);
        } catch (RuntimeException e) { append("Ошибка: " + e.getMessage()); }
    }

    private void append(String s) {
        outputArea.append(s + System.lineSeparator());
    }
}