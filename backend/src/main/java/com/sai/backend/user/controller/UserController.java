package com.sai.backend.user.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sai.backend.auth.exception.UnauthorizedException;
import com.sai.backend.common.session.SessionConst;
import com.sai.backend.user.dto.SearchUserListByLoginIdRequest;
import com.sai.backend.user.dto.SearchUserListByLoginIdResponse;
import com.sai.backend.user.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	
	@GetMapping("/search")
	public ResponseEntity<List<SearchUserListByLoginIdResponse>> searchUserByLoginId(
			HttpSession session,
			@Valid @ModelAttribute
			SearchUserListByLoginIdRequest searchUserListByLoginIdRequest)
	{
		
		Long userId = (Long) session.getAttribute(SessionConst.LOGIN_USER_ID);
		
		if(userId == null)
		{
			throw new UnauthorizedException();
		}
		
		List<SearchUserListByLoginIdResponse> searchResponse
			= userService.searchUser(searchUserListByLoginIdRequest);
		
		return ResponseEntity.ok(searchResponse);
		
	}
	
	
	
}
