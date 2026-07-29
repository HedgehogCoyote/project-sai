package com.sai.backend.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sai.backend.auth.dto.LoginRequest;
import com.sai.backend.auth.dto.LoginResponse;
import com.sai.backend.auth.dto.SignupRequest;
import com.sai.backend.auth.service.AuthService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController // REST 컨트롤러 임을 명시
@RequestMapping("/api/auth") // /api/auth로 요청오면 핸들
@RequiredArgsConstructor // Lombok, final 생성자 생성
public class AuthController {

	private final AuthService authService;
	
	@PostMapping("/signup") // /api/auth/signup을 핸들링함
	public ResponseEntity<?> signup(
			// ?? 이건 잘 모르겠는데 SignupRequest로 받아온다는거인듯
			@Valid @RequestBody SignupRequest request
			)
	{
		
		
		Long userId = authService.signup(request);
		
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(userId);
		
		/*catch(IllegalArgumentException e)
		{
			return ResponseEntity
					.status(HttpStatus.CONFLICT)
					.body(e.getMessage());
		}
		*/
		
	}
	
	@PostMapping("/login")
	public ResponseEntity<?> login(
			@Valid @RequestBody LoginRequest loginRequest,
			HttpSession session
			)
	{
		
		Long userId = authService.login(loginRequest);
		
		session.setAttribute("LOGIN_USER_ID", userId);
		
		LoginResponse response = new LoginResponse(userId);
		
		return ResponseEntity
				.status(HttpStatus.ACCEPTED)
				.body(response);
	
	}
	
	
}
