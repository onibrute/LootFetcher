package org.wowmr.model;

public record ZoneOrInstance(int id, String name, Type type) {
    public enum Type { ZONE, INSTANCE }
    @Override public String toString() {
        return name + (type == Type.ZONE ? " 🌍" : " 🏰");
    }
}
