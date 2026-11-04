package com.sai.backend.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.DriverManager;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sai.backend.common.session.SessionConst;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceIntegrationTest {
    private static final String SCHEMA = "workspace_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String URL = "jdbc:postgresql://localhost:5432/sai";
    private static final String PASSWORD = System.getenv("DB_PASSWORD");
    private static final String STORAGE = System.getProperty("java.io.tmpdir") + "/sai-photos-" + UUID.randomUUID();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        try (var c = DriverManager.getConnection(URL, "postgres", PASSWORD); var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + SCHEMA);
        }
        registry.add("spring.datasource.url", () -> URL + "?currentSchema=" + SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("sai.storage.dir", () -> STORAGE);
    }

    @AfterAll
    static void cleanup() throws Exception {
        try (var c = DriverManager.getConnection(URL, "postgres", PASSWORD); var s = c.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    Long owner, manager, member, outsider, space;

    private Long user(String name) {
        return jdbc.queryForObject("""
                INSERT INTO users (name, login_id, password_hash, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                RETURNING id""", Long.class, name, name + UUID.randomUUID().toString().substring(0, 6),
                encoder.encode("password123"));
    }

    private void join(Long userId, Long spaceId, String role) {
        jdbc.update("INSERT INTO space_member (user_id, space_id, joined_at, role) VALUES (?, ?, CURRENT_TIMESTAMP - INTERVAL '1 hour', ?)",
                userId, spaceId, role);
    }

    private MockHttpSession as(Long userId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(SessionConst.LOGIN_USER_ID, userId);
        return s;
    }

    private JsonNode json(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        return JSON.readTree(r.andReturn().getResponse().getContentAsString());
    }

    @BeforeEach
    void fixtures() {
        owner = user("owner");
        manager = user("manager");
        member = user("member");
        outsider = user("outsider");
        space = jdbc.queryForObject("INSERT INTO space (title, created_at) VALUES ('동기 모임', CURRENT_TIMESTAMP) RETURNING id", Long.class);
        join(owner, space, "OWNER");
        join(manager, space, "MANAGER");
        join(member, space, "MEMBER");
        jdbc.update("DELETE FROM space_feed"); // fixture joins are not news
    }

    @Test
    void spaceSettingsMembersAndRoles() throws Exception {
        String base = "/api/spaces/" + space;
        mvc.perform(get(base).session(as(outsider))).andExpect(status().isForbidden());
        mvc.perform(get(base).session(as(member))).andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("MEMBER")).andExpect(jsonPath("$.memberCount").value(3));
        mvc.perform(patch(base).session(as(member)).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(base).session(as(manager)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" 축구 동아리 \",\"relation\":\"CLUB\",\"colorPrimary\":\"#E3976B\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("축구 동아리"))
                .andExpect(jsonPath("$.relation").value("CLUB")).andExpect(jsonPath("$.colorPrimary").value("#e3976b"));
        mvc.perform(patch(base).session(as(owner)).contentType(MediaType.APPLICATION_JSON).content("{\"colorPrimary\":\"red\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(base + "/members").session(as(member))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("OWNER")).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(patch(base + "/members/me").session(as(member)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"골키퍼\"}")).andExpect(status().isNoContent());
        mvc.perform(get(base + "/members").session(as(owner))).andExpect(jsonPath("$[2].nickname").value("골키퍼"));

        // Managers cannot remove other managers; owners can change roles and transfer ownership.
        mvc.perform(delete(base + "/members/" + owner).session(as(manager))).andExpect(status().isForbidden());
        mvc.perform(patch(base + "/members/" + member).session(as(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"MANAGER\"}")).andExpect(status().isForbidden());
        mvc.perform(post(base + "/leave").session(as(owner))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWNER_MUST_TRANSFER"));
        mvc.perform(patch(base + "/members/" + manager).session(as(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"OWNER\"}")).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT role FROM space_member WHERE user_id = ? AND space_id = ?", String.class, owner, space))
                .isEqualTo("MANAGER");
        mvc.perform(delete(base + "/members/" + member).session(as(owner))).andExpect(status().isNoContent());
        mvc.perform(post(base + "/leave").session(as(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.spaceDeleted").value(false));
        // The last member leaving deletes the space.
        mvc.perform(post(base + "/leave").session(as(manager))).andExpect(jsonPath("$.spaceDeleted").value(true));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM space WHERE id = ?", Integer.class, space)).isZero();
    }

    @Test
    void inviteLinksJoinOnceAndExpireWhenRevoked() throws Exception {
        jdbc.update("UPDATE space SET relation = 'COUPLE' WHERE id = ?", space);
        String base = "/api/spaces/" + space + "/invite-links";
        mvc.perform(post(base).session(as(member))).andExpect(status().isForbidden());
        JsonNode link = json(mvc.perform(post(base).session(as(manager))).andExpect(status().isCreated()));
        String token = link.get("token").stringValue();
        mvc.perform(get("/api/invite-links/" + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID")).andExpect(jsonPath("$.spaceTitle").value("동기 모임"))
                .andExpect(jsonPath("$.alreadyMember").value(false));
        mvc.perform(post("/api/invite-links/" + token + "/join").session(as(outsider))).andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyMember").value(false));
        // Couples join as equals: the trigger upgrades MEMBER to MANAGER and announces the join.
        assertThat(jdbc.queryForObject("SELECT role FROM space_member WHERE user_id = ? AND space_id = ?", String.class, outsider, space))
                .isEqualTo("MANAGER");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM space_feed WHERE space_id = ? AND kind = 'MEMBER'", Integer.class, space))
                .isEqualTo(1);
        mvc.perform(post("/api/invite-links/" + token + "/join").session(as(outsider)))
                .andExpect(jsonPath("$.alreadyMember").value(true));
        mvc.perform(delete(base + "/" + link.get("id").longValue()).session(as(owner))).andExpect(status().isNoContent());
        Long late = user("late");
        mvc.perform(post("/api/invite-links/" + token + "/join").session(as(late))).andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("INVITE_LINK_REVOKED"));
        mvc.perform(get("/api/invite-links/nope")).andExpect(status().isNotFound());
        mvc.perform(post("/api/invite-links/" + token + "/join")).andExpect(status().isUnauthorized());
    }

    @Test
    void sharedContentIsVersionedAndFeedsUnreadBadges() throws Exception {
        String base = "/api/spaces/" + space + "/content";
        mvc.perform(get(base).session(as(member))).andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.content").doesNotExist());
        String doc = "{\"version\":1,\"activities\":[{\"id\":\"trip\",\"title\":\"부산\"}]}";
        mvc.perform(put(base).session(as(member)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"content\":" + doc + ",\"summary\":\"활동을 만들었습니다.\",\"module\":\"activities\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.content.activities[0].title").value("부산"));
        // A second writer holding version 0 must not overwrite the first save.
        mvc.perform(put(base).session(as(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"content\":{\"version\":1}}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONTENT_VERSION_CONFLICT"));
        mvc.perform(put(base).session(as(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1,\"content\":{\"version\":2}}")).andExpect(status().isBadRequest());
        mvc.perform(put(base).session(as(outsider)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1,\"content\":{\"version\":1}}")).andExpect(status().isForbidden());

        mvc.perform(get("/api/feed/unread").session(as(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total").value(1)).andExpect(jsonPath("$[0].modules.activities").value(1));
        mvc.perform(get("/api/feed").session(as(owner))).andExpect(jsonPath("$[0].summary").value("member님이 활동을 만들었습니다.".replace("member", jdbc.queryForObject("SELECT name FROM users WHERE id = ?", String.class, member))))
                .andExpect(jsonPath("$[0].unread").value(true));
        // The author never gets their own change as unread; opening the space clears it for others.
        mvc.perform(get("/api/feed/unread").session(as(member))).andExpect(jsonPath("$[0].total").value(0));
        // A muted space keeps its counts but drops out of the notification list.
        mvc.perform(patch("/api/spaces/" + space + "/members/me/notifications").session(as(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"muted\":true}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/feed").session(as(owner))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/spaces/overview").session(as(owner))).andExpect(jsonPath("$[0].muted").value(true))
                .andExpect(jsonPath("$[0].unread").value(1));
        mvc.perform(patch("/api/spaces/" + space + "/members/me/notifications").session(as(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"muted\":false}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/spaces/" + space + "/seen").session(as(owner))).andExpect(status().isNoContent());
        mvc.perform(get("/api/feed/unread").session(as(owner))).andExpect(jsonPath("$[0].total").value(0));
        mvc.perform(get("/api/spaces/" + space + "/feed").session(as(owner))).andExpect(jsonPath("$[0].unread").value(false));
    }

    @Test
    void noticesAndComments() throws Exception {
        String base = "/api/spaces/" + space;
        mvc.perform(post(base + "/notices").session(as(member)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"회비 공지\"}")).andExpect(status().isForbidden());
        mvc.perform(post(base + "/notices").session(as(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"이번 주 경기는 토요일\",\"pinned\":true}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.pinned").value(true));
        mvc.perform(get(base + "/notices").session(as(member))).andExpect(jsonPath("$[0].body").value("이번 주 경기는 토요일"));

        JsonNode c = json(mvc.perform(post(base + "/comments").session(as(member)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"target\":\"activities:trip\",\"body\":\"저 참석해요\"}")).andExpect(status().isCreated()));
        mvc.perform(post(base + "/comments").session(as(member)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"target\":\"../etc\",\"body\":\"x\"}")).andExpect(status().isBadRequest());
        mvc.perform(get(base + "/comments").param("target", "activities:trip").session(as(owner)))
                .andExpect(jsonPath("$[0].body").value("저 참석해요")).andExpect(jsonPath("$[0].mine").value(false));
        mvc.perform(get(base + "/comment-counts").session(as(owner))).andExpect(jsonPath("$['activities:trip']").value(1));
        long commentId = c.get("id").longValue();
        mvc.perform(post(base + "/posts/" + commentId + "/reactions").session(as(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"emoji\":\"👍\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.reactions['👍']").value(1)).andExpect(jsonPath("$.myReactions[0]").value("👍"));
        mvc.perform(get(base + "/comments").param("target", "activities:trip").session(as(member)))
                .andExpect(jsonPath("$[0].reactions['👍']").value(1)).andExpect(jsonPath("$[0].myReactions.length()").value(0));
        mvc.perform(post(base + "/posts/" + commentId + "/reactions").session(as(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"emoji\":\"👍\"}")).andExpect(jsonPath("$.reactions['👍']").doesNotExist());
        mvc.perform(post(base + "/posts/" + commentId + "/reactions").session(as(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"emoji\":\"💩\"}")).andExpect(status().isBadRequest());
        Long other = user("other");
        join(other, space, "MEMBER");
        mvc.perform(delete(base + "/posts/" + c.get("id").longValue()).session(as(other))).andExpect(status().isForbidden());
        mvc.perform(delete(base + "/posts/" + c.get("id").longValue()).session(as(manager))).andExpect(status().isNoContent());
    }

    @Test
    void photosAreSniffedStoredAndProtected() throws Exception {
        String base = "/api/spaces/" + space + "/photos";
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
        mvc.perform(multipart(base).file(new MockMultipartFile("file", "a.png", "image/png", "not an image".getBytes()))
                .session(as(member))).andExpect(status().isBadRequest());
        JsonNode photo = json(mvc.perform(multipart(base).file(new MockMultipartFile("file", "a.jpg", "image/jpeg", png))
                .param("eventId", "trip").param("caption", "광안리").session(as(member))).andExpect(status().isCreated()));
        assertThat(photo.get("contentType").stringValue()).isEqualTo("image/png");
        mvc.perform(get(base).param("eventId", "trip").session(as(owner))).andExpect(jsonPath("$[0].caption").value("광안리"));
        mvc.perform(get(photo.get("url").stringValue()).session(as(owner))).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andExpect(content().bytes(png));
        mvc.perform(get(photo.get("url").stringValue()).session(as(outsider))).andExpect(status().isForbidden());
        Long other = user("other");
        join(other, space, "MEMBER");
        mvc.perform(delete(base + "/" + photo.get("id").longValue()).session(as(other))).andExpect(status().isForbidden());
        mvc.perform(delete(base + "/" + photo.get("id").longValue()).session(as(member))).andExpect(status().isNoContent());
        mvc.perform(get(photo.get("url").stringValue()).session(as(owner))).andExpect(status().isNotFound());
    }

    @Test
    void accountPasswordAndDeletion() throws Exception {
        mvc.perform(patch("/api/account/profile").session(as(member)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"새 이름\"}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/account/password").session(as(member)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-pass\",\"newPassword\":\"newpassword1\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
        mvc.perform(post("/api/account/password").session(as(member)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"password123\",\"newPassword\":\"newpassword1\"}")).andExpect(status().isNoContent());
        assertThat(encoder.matches("newpassword1",
                jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, member))).isTrue();

        mvc.perform(delete("/api/account").session(as(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"password123\"}")).andExpect(status().isConflict());
        Long solo = jdbc.queryForObject("INSERT INTO space (title, created_at) VALUES ('혼자', CURRENT_TIMESTAMP) RETURNING id", Long.class);
        join(member, solo, "OWNER");
        mvc.perform(delete("/api/account").session(as(member)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"newpassword1\"}")).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM space WHERE id = ?", Integer.class, solo)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM space_member WHERE user_id = ?", Integer.class, member)).isZero();
        assertThat(jdbc.queryForObject("SELECT login_id FROM users WHERE id = ?", String.class, member)).startsWith("deleted-");
    }
}
