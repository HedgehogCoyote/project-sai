package com.sai.backend.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sai.backend.auth.dto.LoginRequest;
import com.sai.backend.auth.dto.LoginResponse;
import com.sai.backend.auth.dto.MeResponse;
import com.sai.backend.auth.dto.SignupRequest;
import com.sai.backend.auth.exception.UnauthorizedException;
import com.sai.backend.auth.service.AuthService;
import com.sai.backend.common.session.SessionConst;

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
		
		session.setAttribute(SessionConst.LOGIN_USER_ID, userId);
		
		LoginResponse response = new LoginResponse(userId);
		
		return ResponseEntity
				.status(HttpStatus.ACCEPTED)
				.body(response);
	
	}
	
	@PostMapping("/logout")
	public ResponseEntity<?> logout(
			HttpSession session
			)
	{
		session.invalidate();
		
		return ResponseEntity // Response Entity는 머지, no Content, build는 또 뭐지?
				.noContent()
				.build();
		
		
	}
	
	@GetMapping("/me")
	public ResponseEntity<?> me(
			HttpSession session
			)
	{
		Long sessionUserId = (Long) session.getAttribute(SessionConst.LOGIN_USER_ID);
		
		// 없으면 
		if(sessionUserId == null)
		{
			throw new UnauthorizedException();
		}
		
		MeResponse me = authService.getMe(
				(Long) session.getAttribute(SessionConst.LOGIN_USER_ID));
		
		
		
		return ResponseEntity.status(HttpStatus.OK)
				.body(me);
	}
	
}
