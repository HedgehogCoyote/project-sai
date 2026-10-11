package com.sai.backend.room.dto;

import java.util.List;

public record RoomLayout(Integer schemaVersion, Grid grid, List<Element> elements) {
    public record Grid(Integer width, Integer height) {}
    public record Element(String id, String kind, Integer x, Integer y,
                          Integer rotation, String module, String label) {}

    public static RoomLayout empty() {
        return new RoomLayout(1, new Grid(8, 8), List.of());
    }
}
