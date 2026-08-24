package com.sai.backend.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sai.backend.auth.dto.LoginRequest;
import com.sai.backend.auth.dto.MeResponse;
import com.sai.backend.auth.dto.SignupRequest;
import com.sai.backend.auth.exception.DuplicateLoginIdException;
import com.sai.backend.auth.exception.InvalidLoginException;
import com.sai.backend.auth.service.AuthService;
import com.sai.backend.common.session.SessionConst;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // ------- 회원가입

    @Test
    void 회원가입_요청_성공() throws Exception {
        Long mockUserId = 1L;

        given(authService.signup(any(SignupRequest.class)))
                .willReturn(mockUserId);

        mockMvc.perform(
                post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSignupJson())
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$").value(1));
    }

    @Test
    void 회원가입_요청_실패_중복계정() throws Exception {
        given(authService.signup(any(SignupRequest.class)))
                .willThrow(new DuplicateLoginIdException());

        mockMvc.perform(
                post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSignupJson())
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"));
    }

    @Test
    void 회원가입_요청_실패_잘못된_포맷() throws Exception {
        mockMvc.perform(
                post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "홍길동",
                                  "loginId": "abc",
                                  "password": "1234",
                                  "phoneNumber": "010123",
                                  "email": "invalid-email"
                                }
                                """)
        )
        .andExpect(status().isBadRequest());
    }

    // ------- 로그인

    @Test
    void 로그인요청_성공() throws Exception {
        Long mockUserId = 1L;
        MockHttpSession session = new MockHttpSession();

        given(authService.login(any(LoginRequest.class)))
                .willReturn(mockUserId);

        mockMvc.perform(
                post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLoginJson())
        )
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.userId").value(1));

        assertEquals(
                mockUserId,
                session.getAttribute(SessionConst.LOGIN_USER_ID)
        );
    }

    @Test
    void 로그인요청_실패() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new InvalidLoginException());

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLoginJson())
        )
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_LOGIN"));
    }

    // ------- 로그아웃

    @Test
    void 로그아웃_성공() throws Exception {
        Long mockUserId = 1L;

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, mockUserId);

        mockMvc.perform(
                post("/api/auth/logout")
                        .session(session)
        )
        .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
    }

    // ------- 현재 사용자

    @Test
    void requestMeWithoutLoginThenReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTHORIZATION_NEEDED"));
    }

    @Test
    void requestMeWithLoginThenReturnMeResponse() throws Exception {
        Long mockUserId = 1L;

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, mockUserId);

        MeResponse response = new MeResponse(
                "test@example.com",
                "홍길동",
                "010-1234-5678",
                "hong1234"
        );

        given(authService.getMe(mockUserId))
                .willReturn(response);

        mockMvc.perform(
                get("/api/auth/me")
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.name").value("홍길동"))
        .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
        .andExpect(jsonPath("$.loginId").value("hong1234"));
    }

    private String validSignupJson() {
        return """
                {
                  "name": "홍길동",
                  "loginId": "hong1234",
                  "password": "password123!",
                  "phoneNumber": "010-1234-5678",
                  "email": "hong@example.com"
                }
                """;
    }

    private String validLoginJson() {
        return """
                {
                  "loginId": "hong1234",
                  "password": "password123!"
                }
                """;
    }
}
