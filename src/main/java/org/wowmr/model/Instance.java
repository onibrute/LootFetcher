package org.wowmr.model;

public class Instance {
    private final int id;
    private final String name;
    private final String description;
    private final String map;
    private final String image;

    public Instance(int id, String name) {
        this(id, name, "", "", null);
    }

    public Instance(int id, String name, String description, String map, String image) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.map = map;
        this.image = image;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String map() {
        return map;
    }

    public String image() {
        return image;
    }

    @Override
    public String toString() {
        return name;
    }
}
