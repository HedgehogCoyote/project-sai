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
import com.sai.backend.space.dto.DenySpaceRequest;
import com.sai.backend.space.dto.InvitationResponse;
import com.sai.backend.space.dto.JoinSpaceRequest;
import com.sai.backend.space.dto.SpaceInvitationRequest;
import com.sai.backend.space.service.SpaceInvitationService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class SpaceInvitationController {
	
	private final SpaceInvitationService spaceInvitationService;
	
	@PostMapping
	public ResponseEntity<Long> invite(
			@Valid 
			@RequestBody SpaceInvitationRequest invitationRequest,
			HttpSession httpSession)
	{
		
		Long userId 
			= (Long) httpSession.getAttribute(SessionConst.LOGIN_USER_ID);
		
		if(userId == null) {
			throw new UnauthorizedException();
		}
		
		
		Long spaceInvitationId = 
				spaceInvitationService.invite(userId, invitationRequest);
		
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(spaceInvitationId);
	}
	
	@PostMapping("/join")
	public ResponseEntity<Long> joinSpaceByInvitation(
			@Valid 
			@RequestBody
			JoinSpaceRequest joinSpaceRequest,
			HttpSession httpSession
			)
	{
		
		
		Long userId 
			= (Long) httpSession.getAttribute(SessionConst.LOGIN_USER_ID);
		
		if(userId == null) {
			throw new UnauthorizedException();
		}
		
		Long spaceMemberId =
				spaceInvitationService.joinSpace(userId, joinSpaceRequest);
		
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(spaceMemberId);
		
	}
	
	@PostMapping("/deny")
	public ResponseEntity<Long> denySpaceByInvitation(
			@Valid 
			@RequestBody
			DenySpaceRequest denySpaceRequest,
			HttpSession httpSession
			)
	{
		
		
		Long userId 
			= (Long) httpSession.getAttribute(SessionConst.LOGIN_USER_ID);
		
		if(userId == null) {
			throw new UnauthorizedException();
		}
		
		Long invitationId =
				spaceInvitationService.denySpaceInvitation(userId, denySpaceRequest.invitationId());
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(invitationId);
		
	}
	
	
	@GetMapping("/received")
	public ResponseEntity<List<InvitationResponse>> getReceivedInvitation(
			HttpSession httpSession
			)
	{
		
		Long userId
			= (Long) httpSession.getAttribute(SessionConst.LOGIN_USER_ID);
		
		if(userId == null) {
			throw new UnauthorizedException();
		}
		
		List<InvitationResponse> receivedInvitationResponseList 
			= spaceInvitationService.getReceivedInvitationList(userId);
		
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(receivedInvitationResponseList);
	}
	
	@GetMapping("/sent")
	public ResponseEntity<List<InvitationResponse>> getSentInvitation(
			HttpSession httpSession
			)
	{
		
		Long userId
			= (Long) httpSession.getAttribute(SessionConst.LOGIN_USER_ID);
		
		if(userId == null) {
			throw new UnauthorizedException();
		}
		
		List<InvitationResponse> sentInvitationResponseList 
			= spaceInvitationService.getSentInvitationList(userId);
		
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(sentInvitationResponseList);
	}
	
	
}
