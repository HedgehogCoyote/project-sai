package com.sai.backend.room;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sai.backend.common.session.SessionConst;
import com.sai.backend.room.dto.RoomLayout;
import com.sai.backend.room.dto.RoomLayout.Element;
import com.sai.backend.room.dto.SaveRoomRequest;
import com.sai.backend.room.exception.RoomConflictException;
import com.sai.backend.room.service.RoomService;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class RoomIntegrationTest {
    // Isolate all migrations and fixtures from the developer's application tables.
    private static final String SCHEMA = "room_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String URL = "jdbc:postgresql://localhost:5432/sai";
    private static final String PASSWORD = System.getenv("DB_PASSWORD");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        try (var connection = DriverManager.getConnection(URL, "postgres", PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
        }
        registry.add("spring.datasource.url", () -> URL + "?currentSchema=" + SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @AfterAll
    static void cleanup() throws Exception {
        try (var connection = DriverManager.getConnection(URL, "postgres", PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired RoomService service;
    Long owner, manager, member, outsider, space;

    @BeforeEach
    void fixtures() {
        owner = user(); manager = user(); member = user(); outsider = user();
        space = jdbc.queryForObject("INSERT INTO space (title, created_at) VALUES ('room test', CURRENT_TIMESTAMP) RETURNING id", Long.class);
        membership(owner, "OWNER"); membership(manager, "MANAGER"); membership(member, "MEMBER");
    }

    private Long user() {
        return jdbc.queryForObject("INSERT INTO users (name, login_id, password_hash, created_at) VALUES ('test', ?, 'test-hash', CURRENT_TIMESTAMP) RETURNING id",
                Long.class, "room-" + UUID.randomUUID());
    }

    private void membership(Long user, String role) {
        jdbc.update("INSERT INTO space_member (space_id, user_id, role, joined_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)", space, user, role);
    }

    private String endpoint() { return "/api/spaces/" + space + "/room"; }

    private RoomLayout layout() {
        return new RoomLayout(1, new RoomLayout.Grid(8, 8), List.of(
                new Element(UUID.randomUUID().toString(), "desk", 4, 3, 0, "tasks", "할 일")));
    }

    private String body(long version, RoomLayout layout) {
        return JSON.writeValueAsString(new SaveRoomRequest(version, layout));
    }

    @Test
    void unsavedRoomReturnsEmptyVersionZeroWithoutWriting() throws Exception {
        mvc.perform(get(endpoint()).sessionAttr(SessionConst.LOGIN_USER_ID, member))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.layout.elements").isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM space_room WHERE space_id = ?", Integer.class, space));
    }

    @Test
    void jsonbRoundTripAndSharedMemberRead() throws Exception {
        RoomLayout layout = layout();
        mvc.perform(put(endpoint()).sessionAttr(SessionConst.LOGIN_USER_ID, owner)
                .contentType(MediaType.APPLICATION_JSON).content(body(0, layout)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.updatedBy").value(owner)).andExpect(jsonPath("$.updatedAt").isNotEmpty());
        mvc.perform(get(endpoint()).sessionAttr(SessionConst.LOGIN_USER_ID, member))
                .andExpect(status().isOk()).andExpect(jsonPath("$.layout.elements[0].label").value("할 일"));
        assertEquals(layout, service.get(manager, space).layout());
        assertEquals("object", jdbc.queryForObject("SELECT jsonb_typeof(layout) FROM space_room WHERE space_id = ?", String.class, space));
        assertEquals(2, service.save(manager, space, new SaveRoomRequest(1L, RoomLayout.empty())).version());
        assertEquals(manager, service.get(owner, space).updatedBy());
    }

    @Test
    void unauthenticatedAndNonmembersCannotAccess() throws Exception {
        mvc.perform(get(endpoint())).andExpect(status().isUnauthorized());
        mvc.perform(put(endpoint()).contentType(MediaType.APPLICATION_JSON).content(body(0, layout())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(endpoint()).sessionAttr(SessionConst.LOGIN_USER_ID, outsider)).andExpect(status().isForbidden());
        mvc.perform(put(endpoint()).sessionAttr(SessionConst.LOGIN_USER_ID, outsider)
                .contentType(MediaType.APPLICATION_JSON).content(body(0, layout())))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCannotSaveAndMissingSpaceDoesNotLeakExistence() throws Exception {
        mvc.perform(put(endpoint()).sessionAttr(SessionConst.LOGIN_USER_ID, member)
                .contentType(MediaType.APPLICATION_JSON).content(body(0, layout())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/spaces/9223372036854775806/room").sessionAttr(SessionConst.LOGIN_USER_ID, outsider))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidLayoutAndVersionLeaveSavedRoomUnchanged() throws Exception {
        RoomLayout original = layout();
        service.save(owner, space, new SaveRoomRequest(0L, original));
        RoomLayout outside = new RoomLayout(1, new RoomLayout.Grid(8, 8), List.of(
                new Element(UUID.randomUUID().toString(), "sofa", 7, 7, 0, "", "")));
        mvc.perform(put(endpoint()).sessionAttr(SessionConst.LOGIN_USER_ID, owner)
                .contentType(MediaType.APPLICATION_JSON).content(body(1, outside)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ROOM_LAYOUT"));
        mvc.perform(put(endpoint()).sessionAttr(SessionConst.LOGIN_USER_ID, owner)
                .contentType(MediaType.APPLICATION_JSON).content(body(-1, original)))
                .andExpect(status().isBadRequest());
        assertEquals(original, service.get(owner, space).layout());
        assertEquals(1, service.get(owner, space).version());
    }

    @Test
    void malformedAndCoercedValuesAreRejectedInsteadOfSilentlyChanged() throws Exception {
        String valid = body(0, layout());
        for (String invalid : List.of("{", "null", valid.replace("\"x\":4", "\"x\":4.5"),
                valid.replace("\"version\":0", "\"version\":\"0\""),
                valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"event\":\"local-event\""))) {
            mvc.perform(put(endpoint()).sessionAttr(SessionConst.LOGIN_USER_ID, owner)
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, service.get(owner, space).version());
    }

    @Test
    void staleVersionReturns409AndPreservesWinner() throws Exception {
        RoomLayout winner = layout();
        service.save(owner, space, new SaveRoomRequest(0L, winner));
        mvc.perform(put(endpoint()).sessionAttr(SessionConst.LOGIN_USER_ID, manager)
                .contentType(MediaType.APPLICATION_JSON).content(body(0, RoomLayout.empty())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ROOM_VERSION_CONFLICT"));
        assertEquals(winner, service.get(member, space).layout());
    }

    @Test
    void concurrentFirstSavesHaveExactlyOneWinner() throws Exception { race(0); }

    @Test
    void concurrentUpdatesHaveExactlyOneWinner() throws Exception {
        service.save(owner, space, new SaveRoomRequest(0L, RoomLayout.empty()));
        race(1);
    }

    private void race(long version) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<RoomLayout> task = () -> {
                RoomLayout proposed = layout();
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                try {
                    return service.save(owner, space, new SaveRoomRequest(version, proposed)).layout();
                } catch (RoomConflictException e) { return null; }
            };
            var first = executor.submit(task); var second = executor.submit(task);
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            RoomLayout a = first.get(15, TimeUnit.SECONDS), b = second.get(15, TimeUnit.SECONDS);
            assertEquals(1, (a == null ? 0 : 1) + (b == null ? 0 : 1));
            var saved = service.get(member, space);
            assertEquals(version + 1, saved.version());
            assertEquals(a != null ? a : b, saved.layout());
        } finally { start.countDown(); }
    }
}
