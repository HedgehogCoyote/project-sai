package com.sai.backend.room.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sai.backend.room.dto.RoomLayout;
import com.sai.backend.room.dto.RoomLayout.Element;
import com.sai.backend.room.exception.InvalidRoomException;

@Component
public class RoomLayoutValidator {
    private static final Map<String, int[]> FOOTPRINTS = Map.of(
            "sofa", new int[]{3, 2}, "desk", new int[]{2, 2},
            "shelf", new int[]{2, 1}, "plant", new int[]{1, 1},
            "rug", new int[]{3, 2}, "frame", new int[]{1, 1},
            "cat", new int[]{1, 1}, "calendar", new int[]{1, 1});
    private static final Set<String> MODULES = Set.of("", "activities", "records", "tasks",
            "calendar", "itinerary", "places", "packing", "expenses", "polls");

    public void validate(RoomLayout layout) {
        if (layout == null || !Integer.valueOf(1).equals(layout.schemaVersion()))
            fail("지원하는 맵 schemaVersion은 1입니다.");
        if (layout.grid() == null || !Integer.valueOf(8).equals(layout.grid().width())
                || !Integer.valueOf(8).equals(layout.grid().height()))
            fail("방 격자는 8 × 8이어야 합니다.");
        if (layout.elements() == null || layout.elements().size() > 64)
            fail("맵 요소는 최대 64개까지 저장할 수 있습니다.");

        Set<String> ids = new HashSet<>();
        boolean[][] occupied = new boolean[8][8];
        for (Element element : layout.elements()) {
            if (element == null || !validId(element.id()) || !ids.add(element.id()))
                fail("맵 요소 ID는 중복되지 않는 UUID여야 합니다.");
            if (element.kind() == null || !FOOTPRINTS.containsKey(element.kind()))
                fail("지원하지 않는 가구 종류입니다.");
            if (element.rotation() == null || element.rotation() < 0 || element.rotation() > 3)
                fail("회전 값은 0~3이어야 합니다.");
            if (element.module() == null || !MODULES.contains(element.module()))
                fail("지원하지 않는 연결 기능입니다.");
            if (element.label() == null || element.label().length() > 24)
                fail("표시 이름은 24자 이하여야 합니다.");
            int[] footprint = FOOTPRINTS.get(element.kind());
            int width = footprint[element.rotation() % 2];
            int height = footprint[1 - element.rotation() % 2];
            if (element.x() == null || element.y() == null || element.x() < 0 || element.y() < 0
                    || element.x() > 8 - width || element.y() > 8 - height)
                fail("가구를 방 안에 배치해 주세요.");
            // Rugs may sit under furniture, matching the existing client placement rule.
            if (element.kind().equals("rug")) continue;
            for (int x = element.x(); x < element.x() + width; x++) {
                for (int y = element.y(); y < element.y() + height; y++) {
                    if (occupied[x][y]) fail("가구가 겹치는 배치는 저장할 수 없습니다.");
                    occupied[x][y] = true;
                }
            }
        }
    }

    private boolean validId(String id) {
        if (id == null || id.length() != 36) return false;
        try {
            return UUID.fromString(id).toString().equals(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void fail(String message) {
        throw new InvalidRoomException(message);
    }
}
