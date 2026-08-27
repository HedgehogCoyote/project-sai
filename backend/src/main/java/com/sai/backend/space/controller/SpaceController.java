package com.sai.backend.space.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sai.backend.auth.exception.UnauthorizedException;
import com.sai.backend.common.session.SessionConst;
import com.sai.backend.space.dto.CreateSpaceRequest;
import com.sai.backend.space.dto.CreateSpaceResponse;
import com.sai.backend.space.dto.ParticipatingSpacesResponse;
import com.sai.backend.space.service.SpaceService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController // REST 컨트롤러 임을 명시
@RequestMapping("/api/spaces")
@RequiredArgsConstructor

public class SpaceController {
	
	private final SpaceService spaceService;

	
	@PostMapping
	public ResponseEntity<CreateSpaceResponse> createSpace(
			@Valid @RequestBody CreateSpaceRequest createSpaceRequest,
			HttpSession httpSession){
		
		Long userId = 
				(Long) httpSession.getAttribute(SessionConst.LOGIN_USER_ID);
		
		// 세션 user Id가 잘못 됨 
		if(userId == null) {
			throw new UnauthorizedException();
		}
		
		Long createdSpaceId = 
				spaceService.createSpace(createSpaceRequest, userId);
		
		CreateSpaceResponse createSpaceResponse = new 
				CreateSpaceResponse(createdSpaceId);
		
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(createSpaceResponse);
	}
	
	@GetMapping("/my")
	public ResponseEntity<List<ParticipatingSpacesResponse>> getParticipatingSpaces(
			HttpSession httpSession
			)
	{
		Long userId = 
				(Long) httpSession.getAttribute(SessionConst.LOGIN_USER_ID);
		
		// 세션 user Id가 잘못 됨 
		if(userId == null) {
			throw new UnauthorizedException();
		}
		
		List<ParticipatingSpacesResponse> participatingSpacesResponse =
				spaceService.getMySpaceList(userId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(participatingSpacesResponse);
		
	}
	
}
