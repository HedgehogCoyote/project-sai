package com.sai.backend.space.controller;

import com.sai.backend.common.session.SessionConst;
import com.sai.backend.space.domain.Space;
import com.sai.backend.space.dto.CreateSpaceRequest;
import com.sai.backend.space.dto.ParticipatingSpacesResponse;
import com.sai.backend.space.service.SpaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpaceController.class)
public class SpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpaceService spaceService;


    @Test
    void 비로그인_상태_스페이스_생성() throws Exception
    {

        // given(spaceService.createSpace(createSpaceRequest, unloginId))

        mockMvc.perform(post("/api/spaces/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateSpaceJson()))
                .andExpect(status().isUnauthorized());

    }

    /*    @Test
    void 잘못된ID_스페이스_생성_실패() throws Exception
    {
        Long wrongId = -9999999L;
    }*/
    @Test
    void 로그인_상태_스페이스_생성_성공() throws Exception {
        Long mockUserId = 1L;

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, mockUserId);

        Long mockSpaceId = 1L;

        given(spaceService.createSpace(any(CreateSpaceRequest.class), eq(mockUserId)))
                .willReturn(mockSpaceId);

        mockMvc.perform(
                post("/api/spaces")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateSpaceJson())
        )
                .andExpect(status().isCreated());
    }

    private String validCreateSpaceJson() throws Exception
    {
        return """
                {
                    "title" : "Hello My Little"
                }
                """;
    }

    private String invalidCreateSpaceJson() throws Exception
    {
        return """
                {
                    "title" : "fkdkeqwe@@@dkdkekqoreqwpeopqwpeoqwpoeopqwopeqopweopqwpoeopqwopeqwopeopqwpoeopqweopqwopeopqwop"
                }
                """;
    }

    @Test
    void 스페이스_이름_50자초과_생성() throws Exception
    {
        Long mockUserId = 1L;

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, mockUserId);

        mockMvc.perform(
                        post("/api/spaces")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidCreateSpaceJson())
                )
                .andExpect(status().isBadRequest());

        then(spaceService).shouldHaveNoInteractions();
    }
    @Test
    void 비로그인_상태_스페이스_목록_요청() throws Exception
    {

        mockMvc.perform(get("/api/spaces/my"))
                .andExpect(status().isUnauthorized());
        // Service가 Return 안하면! 그런듯
        then(spaceService).shouldHaveNoInteractions();
    }

    @Test
    void 로그인_상태_스페이스_목록_요청() throws Exception
    {
        Long userId = 1L;

        List<ParticipatingSpacesResponse> participatingSpaceList =
                new ArrayList<ParticipatingSpacesResponse>();

        participatingSpaceList.add(new ParticipatingSpacesResponse(1L, "안녕 에리", "OWNER", 1));
        participatingSpaceList.add(new ParticipatingSpacesResponse(2L, "안녕 태호", "OWNER", 1));
        participatingSpaceList.add(new ParticipatingSpacesResponse(3L, "안녕 민서", "OWNER", 1));

        given(spaceService.getMySpaceList(userId))
                .willReturn(participatingSpaceList);

        mockMvc.perform(get("/api/spaces/my")
                        .sessionAttr(SessionConst.LOGIN_USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spaceId").value(1L))
                .andExpect(jsonPath("$[0].title").value("안녕 에리"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[0].count").value(1))

                .andExpect(jsonPath("$[1].spaceId").value(2L))
                .andExpect(jsonPath("$[1].title").value("안녕 태호"))
                .andExpect(jsonPath("$[1].role").value("OWNER"))
                .andExpect(jsonPath("$[1].count").value(1))

                .andExpect(jsonPath("$[2].spaceId").value(3L))
                .andExpect(jsonPath("$[2].title").value("안녕 민서"))
                .andExpect(jsonPath("$[2].role").value("OWNER"))
                .andExpect(jsonPath("$[2].count").value(1));
    }

}
