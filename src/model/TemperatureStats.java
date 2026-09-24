package model;

// статистика температур за период
public record TemperatureStats(int minCelsius, int maxCelsius, double averageCelsius) {
    @Override
    public String toString() {
        return "min=%d°C, max=%d°C, avg=%.2f°C".formatted(minCelsius, maxCelsius, averageCelsius);
    }
}