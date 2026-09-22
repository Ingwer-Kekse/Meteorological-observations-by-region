package model;

import java.util.Locale;

// восемь румбов. Диапазон в градусах, [start, end) с переходом через 360 у N
public enum WindDirection {
    N ("North",      337.5, 22.5),
    NE("North-East",  22.5, 67.5),
    E ("East",        67.5, 112.5),
    SE("South-East", 112.5, 157.5),
    S ("South",      157.5, 202.5),
    SW("South-West", 202.5, 247.5),
    W ("West",       247.5, 292.5),
    NW("North-West", 292.5, 337.5);

    private final String label;
    private final double startDeg;
    private final double endDeg;

    WindDirection(String label, double startDeg, double endDeg) {
        this.label = label;
        this.startDeg = startDeg;
        this.endDeg = endDeg;
    }

    public String label() { return label; }
    public double startDeg() { return startDeg; }
    public double endDeg()   { return endDeg; }

    // Попадает ли азимут (в градусах) в диапазон румба
    public boolean covers(double azimuth) {
        double a = ((azimuth % 360) + 360) % 360;
        if (startDeg < endDeg) {
            return a >= startDeg && a < endDeg;
        }
        // переход через 0 (север)
        return a >= startDeg || a < endDeg;
    }

    // румб по азимуту. Значение нормализуется в [0, 360)
    public static WindDirection fromAzimuth(double azimuth) {
        for (WindDirection d : values()) {
            if (d.covers(azimuth)) return d;
        }
        throw new IllegalArgumentException("Invalid azimuth: " + azimuth);
    }

    // Удобный парсер для строк ввода
    public static WindDirection parse(String s) {
        return WindDirection.valueOf(s.trim().toUpperCase(Locale.ROOT));
    }
}