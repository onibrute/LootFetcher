package org.wowmr.model;

public record Encounter(int id, String name) {
    @Override public String toString() {
        return name;
    }
}
