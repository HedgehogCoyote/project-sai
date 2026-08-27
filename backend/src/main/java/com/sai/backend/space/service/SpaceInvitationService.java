package com.sai.backend.space.service;

import org.springframework.stereotype.Service;

import com.sai.backend.auth.exception.UserNotFoundException;
import com.sai.backend.space.domain.Space;
import com.sai.backend.space.domain.SpaceInvitation;
import com.sai.backend.space.domain.SpaceMember;
import com.sai.backend.space.dto.SpaceInvitationRequest;
import com.sai.backend.space.exception.SpaceMemberAlreadyExist;
import com.sai.backend.space.exception.SpaceNotFoundException;
import com.sai.backend.space.exception.SpacePermissionDeniedException;
import com.sai.backend.space.repository.SpaceInvitationRepository;
import com.sai.backend.space.repository.SpaceMemberRepository;
import com.sai.backend.space.repository.SpaceRepository;
import com.sai.backend.user.domain.User;
import com.sai.backend.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpaceInvitationService {

	private final UserRepository userRepository;
	private final SpaceRepository spaceRepository;
	private final SpaceInvitationRepository spaceInvitationRepository;
	private final SpaceMemberRepository spaceMemberRepository;
	
	@Transactional
	public Long invite(Long inviterUserId, SpaceInvitationRequest spaceInvitationRequest)
	{
		User inviterUser = userRepository.findById(inviterUserId)
				.orElseThrow( () -> new UserNotFoundException());
		
		User inviteeUser = userRepository.findById(spaceInvitationRequest.inviteeUserId())
				.orElseThrow( () -> new UserNotFoundException());
		
		Space space = spaceRepository.findById(spaceInvitationRequest.spaceId())
				.orElseThrow( () -> new SpaceNotFoundException());
		
		// 이미 Invitation 에 있냐 없냐는 테스트 하지 않음.
		
		// 검증 - 일단, SpaceMember 중 하나면 초대 가능하게 끔
		
		SpaceMember spaceMember = spaceMemberRepository.findByUserIdAndSpaceId(inviterUserId, spaceInvitationRequest.spaceId())
				.orElseThrow( () -> new SpacePermissionDeniedException());
		
		
		// TODO : 나중에 정책적으로 OWNER 만 할지, MANAGER만 관리하게 할지 등 고려하기 
		
		// 만약 이미 있으면 
		if(spaceMemberRepository.existsByUserIdAndSpaceId(
				spaceInvitationRequest.inviteeUserId(), spaceInvitationRequest.spaceId()))
		{
			throw new SpaceMemberAlreadyExist();
		}
		
		
		SpaceInvitation spaceInvitation = new SpaceInvitation(space,
				inviterUser, inviteeUser);
		
		spaceInvitationRepository.save(spaceInvitation);
		
		return spaceInvitation.getId();
		
		
	}
	
}
