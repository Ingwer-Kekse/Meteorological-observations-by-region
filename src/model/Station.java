package model;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//Метеостанция. Идентичность — по коду
public final class Station implements Describable {

    private final String code;
    private final String name;
    private final ZoneId zone;
    private final List<Observation> observations = new ArrayList<>();

    public Station(String code, String name, ZoneId zone) {
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    public String code() { return code; }
    public String name() { return name; }
    public ZoneId zone() { return zone; }

    public void addObservation(Observation o) {
        observations.add(Objects.requireNonNull(o, "observation"));
    }

    //возвращаем неизменяемую копию — внутренний список наружу не утекает
    public List<Observation> observations() {
        return List.copyOf(observations);
    }

    @Override
    public String describe() {
        return "Station %s '%s' [%s], observations=%d".formatted(code, name, zone, observations.size());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Station other && code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}