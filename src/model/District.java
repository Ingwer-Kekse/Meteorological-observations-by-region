package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// район, состоящий из станций
public final class District implements Describable {

    private final String name;
    private final List<Station> stations = new ArrayList<>();

    public District(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() { return name; }

    public void addStation(Station s) {
        stations.add(Objects.requireNonNull(s, "station"));
    }

    public List<Station> stations() {
        return List.copyOf(stations);
    }

    @Override
    public String describe() {
        return "District '%s', stations=%d".formatted(name, stations.size());
    }
}