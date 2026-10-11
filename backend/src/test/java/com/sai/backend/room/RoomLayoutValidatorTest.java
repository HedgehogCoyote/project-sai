package com.sai.backend.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sai.backend.room.dto.RoomLayout;
import com.sai.backend.room.dto.RoomLayout.Element;
import com.sai.backend.room.exception.InvalidRoomException;
import com.sai.backend.room.service.RoomLayoutValidator;

class RoomLayoutValidatorTest {
    private final RoomLayoutValidator validator = new RoomLayoutValidator();
    private RoomLayout layout(Element... elements) {
        return new RoomLayout(1, new RoomLayout.Grid(8, 8), List.of(elements));
    }
    private Element element(String kind, int x, int y, int rotation) {
        return new Element(UUID.randomUUID().toString(), kind, x, y, rotation, "", "");
    }
    @Test void rotationUsesServerFootprints() {
        assertDoesNotThrow(() -> validator.validate(layout(element("sofa", 6, 5, 1))));
        assertThrows(InvalidRoomException.class, () -> validator.validate(layout(element("sofa", 6, 5, 0))));
    }
    @Test void rejectsOverlapButAllowsRugUnderFurnitureRegardlessOfOrder() {
        Element desk = element("desk", 0, 0, 0), rug = element("rug", 0, 0, 0);
        assertDoesNotThrow(() -> validator.validate(layout(desk, rug)));
        assertDoesNotThrow(() -> validator.validate(layout(rug, desk)));
        assertThrows(InvalidRoomException.class, () -> validator.validate(layout(desk, element("plant", 1, 1, 0))));
    }
    @Test void rejectsDuplicateIdsAndUnrecognizedValues() {
        Element plant = element("plant", 0, 0, 0);
        assertThrows(InvalidRoomException.class, () -> validator.validate(layout(plant, plant)));
        assertThrows(InvalidRoomException.class, () -> validator.validate(layout(element("unknown", 0, 0, 0))));
        assertThrows(InvalidRoomException.class, () -> validator.validate(layout(element("plant", 0, 0, 4))));
        assertThrows(InvalidRoomException.class, () -> validator.validate(layout(new Element(plant.id(), "plant", 0, 0, 0, "unknown", ""))));
        assertThrows(InvalidRoomException.class, () -> validator.validate(layout(new Element("invalid", "plant", 0, 0, 0, "", ""))));
        assertThrows(InvalidRoomException.class, () -> validator.validate(layout(new Element(plant.id(), "plant", 0, 0, 0, "", "a".repeat(25)))));
        assertThrows(InvalidRoomException.class, () -> validator.validate(layout(element("plant", Integer.MAX_VALUE, 0, 0))));
    }
    @Test void rejectsMissingFieldsUnsupportedSchemaAndTooManyElements() {
        assertThrows(InvalidRoomException.class, () -> validator.validate(null));
        assertThrows(InvalidRoomException.class, () -> validator.validate(new RoomLayout(2, new RoomLayout.Grid(8, 8), List.of())));
        assertThrows(InvalidRoomException.class, () -> validator.validate(new RoomLayout(1, null, List.of())));
        assertThrows(InvalidRoomException.class, () -> validator.validate(new RoomLayout(1, new RoomLayout.Grid(9, 8), List.of())));
        assertThrows(InvalidRoomException.class, () -> validator.validate(new RoomLayout(1, new RoomLayout.Grid(8, 8), null)));
        assertThrows(InvalidRoomException.class, () -> validator.validate(new RoomLayout(1, new RoomLayout.Grid(8, 8), Collections.nCopies(65, element("plant", 0, 0, 0)))));
        assertThrows(InvalidRoomException.class, () -> validator.validate(layout(new Element(UUID.randomUUID().toString(), "plant", null, 0, 0, "", ""))));
    }
}
