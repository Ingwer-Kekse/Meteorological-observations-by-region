package model;

import java.util.Locale;

// явления погоды
public enum WeatherPhenomenon {
    CLEAR(false),
    PARTLY_CLOUDY(false),
    CLOUDY(false),
    FOG(false),
    DRIZZLE(true),
    RAIN(true),
    SNOW(true),
    HAIL(true),
    THUNDERSTORM(true);

    private final boolean precipitation;

    WeatherPhenomenon(boolean precipitation) {
        this.precipitation = precipitation;
    }

    public boolean isPrecipitation() { return precipitation; }

    public static WeatherPhenomenon parse(String s) {
        return WeatherPhenomenon.valueOf(s.trim().toUpperCase(Locale.ROOT));
    }
}