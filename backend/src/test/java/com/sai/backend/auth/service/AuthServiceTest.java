package com.sai.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sai.backend.auth.dto.LoginRequest;
import com.sai.backend.auth.dto.SignupRequest;
import com.sai.backend.auth.exception.DuplicateLoginIdException;
import com.sai.backend.auth.exception.InvalidLoginException;
import com.sai.backend.auth.exception.UserNotFoundException;
import com.sai.backend.user.domain.User;
import com.sai.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // ----- 회원가입 -----

    @Test
    void 회원가입시_성공() {
        SignupRequest signRequest = new SignupRequest(
                "강태호",
                "testuser",
                "Test1234!",
                "010-1234-5678",
                "test@example.com"
        );

        User mockUser = mock(User.class);

        given(userRepository.existsByLoginId("testuser"))
                .willReturn(false);

        given(passwordEncoder.encode("Test1234!"))
                .willReturn("ENCODED_VALUE");

        given(userRepository.save(any(User.class)))
                .willReturn(mockUser);

        given(mockUser.getId())
                .willReturn(1L);

        Long resultId = authService.signup(signRequest);

        assertEquals(1L, resultId);
    }

    @Test
    void 중복_회원가입시_실패() {
        SignupRequest signRequest = new SignupRequest(
                "강태호",
                "testuser",
                "Test1234!",
                "010-1234-5678",
                "test@example.com"
        );

        given(userRepository.existsByLoginId("testuser"))
                .willReturn(true);

        assertThrows(
                DuplicateLoginIdException.class,
                () -> authService.signup(signRequest)
        );
    }

    // ----- 로그인 -----

    @Test
    void 로그인_성공() {
        LoginRequest loginRequest = new LoginRequest(
                "testuser",
                "Test1234!"
        );

        User mockUser = mock(User.class);

        given(userRepository.findByLoginId("testuser"))
                .willReturn(Optional.of(mockUser));

        given(mockUser.getPasswordHash())
                .willReturn("ENCODED");

        given(passwordEncoder.matches("Test1234!", "ENCODED"))
                .willReturn(true);

        given(mockUser.getId())
                .willReturn(1L);

        assertEquals(1L, authService.login(loginRequest));
    }

    @Test
    void 로그인_실패_잘못된_아이디() {
        LoginRequest loginRequest = new LoginRequest(
                "wrongId",
                "Test1234!"
        );

        given(userRepository.findByLoginId("wrongId"))
                .willReturn(Optional.empty());

        assertThrows(
                InvalidLoginException.class,
                () -> authService.login(loginRequest)
        );
    }

    @Test
    void 로그인_실패_잘못된_패스워드() {
        LoginRequest loginRequest = new LoginRequest(
                "testuser",
                "wrongPassword"
        );

        User mockUser = mock(User.class);

        given(userRepository.findByLoginId("testuser"))
                .willReturn(Optional.of(mockUser));

        given(mockUser.getPasswordHash())
                .willReturn("ENCODED");

        given(passwordEncoder.matches("wrongPassword", "ENCODED"))
                .willReturn(false);

        assertThrows(
                InvalidLoginException.class,
                () -> authService.login(loginRequest)
        );
    }

    // ----- 현재 사용자 조회 -----

    @Test
    void 잘못된_사용자_조회() {
        Long wrongId = 999L;

        given(userRepository.findById(wrongId))
                .willReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> authService.getMe(wrongId)
        );
    }
}
