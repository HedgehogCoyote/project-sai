package com.sai.backend.space.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.sai.backend.auth.exception.UserNotFoundException;
import com.sai.backend.space.domain.Space;
import com.sai.backend.space.domain.SpaceInvitation;
import com.sai.backend.space.domain.SpaceInvitationStatus;
import com.sai.backend.space.domain.SpaceMember;
import com.sai.backend.space.domain.SpaceMemberRole;
import com.sai.backend.space.dto.InvitationResponse;
import com.sai.backend.space.dto.JoinSpaceRequest;
import com.sai.backend.space.dto.SpaceInvitationRequest;
import com.sai.backend.space.exception.InvalidInvitationStatusException;
import com.sai.backend.space.exception.InvitationPermissionDenied;
import com.sai.backend.space.exception.SelfInvitationException;
import com.sai.backend.space.exception.SpaceInvitationAlreadyExistException;
import com.sai.backend.space.exception.SpaceInvitationNotFoundException;
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
	public Long invite(Long inviterUserId, SpaceInvitationRequest spaceInvitationRequest) {
		User inviterUser = userRepository.findById(inviterUserId).orElseThrow(() -> new UserNotFoundException());

		User inviteeUser = userRepository.findById(spaceInvitationRequest.inviteeUserId())
				.orElseThrow(() -> new UserNotFoundException());

		Space space = spaceRepository.findById(spaceInvitationRequest.spaceId())
				.orElseThrow(() -> new SpaceNotFoundException());

		// 이미 Invitation 에 있냐 없냐는 테스트 하지 않음.

		// 검증 - 일단, SpaceMember 중 하나면 초대 가능하게 끔

		SpaceMember spaceMember = spaceMemberRepository
				.findByUserIdAndSpaceId(inviterUserId, spaceInvitationRequest.spaceId())
				.orElseThrow(() -> new SpacePermissionDeniedException());

		// 근데, 이미 그 사람이 보낸게 있으면 보내지 않음.
		if (spaceInvitationRepository.existsBySpace_IdAndInviter_IdAndInvitee_IdAndStatus(space.getId(), inviterUserId,
				spaceInvitationRequest.inviteeUserId(), SpaceInvitationStatus.PENDING)

		) {
			throw new SpaceInvitationAlreadyExistException();
		}
		
		
		if (inviterUser.getId().equals(inviteeUser.getId()))
		{
			throw new SelfInvitationException();
		}
		

		// TODO : 나중에 정책적으로 OWNER 만 할지, MANAGER만 관리하게 할지 등 고려하기

		// 만약 이미 있으면
		if (spaceMemberRepository.existsByUserIdAndSpaceId(spaceInvitationRequest.inviteeUserId(),
				spaceInvitationRequest.spaceId())) {
			throw new SpaceMemberAlreadyExist();
		}

		SpaceInvitation spaceInvitation = new SpaceInvitation(space, inviterUser, inviteeUser);

		spaceInvitationRepository.save(spaceInvitation);

		return spaceInvitation.getId();

	}

	@Transactional
	public Long denySpaceInvitation(Long requestUserId, Long invitationId) {
		SpaceInvitation spaceInvitation = spaceInvitationRepository.findById(invitationId)
				.orElseThrow(() -> new SpaceInvitationNotFoundException());

		if (!Objects.equals(spaceInvitation.getStatus(), SpaceInvitationStatus.PENDING)) {
			throw new InvalidInvitationStatusException();
		}

		User toJoinUser = userRepository.findById(spaceInvitation.getInvitee().getId())
				.orElseThrow(() -> new UserNotFoundException());

		if (!Objects.equals(toJoinUser.getId(), requestUserId)) {
			throw new InvitationPermissionDenied();
		}

		spaceInvitation.denied();

		return spaceInvitation.getId();
	}

	@Transactional
	public Long joinSpace(Long requestUserId, JoinSpaceRequest joinSpaceRequest) {

		SpaceInvitation spaceInvitation = spaceInvitationRepository.findById(joinSpaceRequest.invitationId())
				.orElseThrow(() -> new SpaceInvitationNotFoundException());

		if (!Objects.equals(spaceInvitation.getStatus(), SpaceInvitationStatus.PENDING)) {
			throw new InvalidInvitationStatusException();
		}

		// Space
		// Space가 이미 삭제되었을 수도 있음
		Space toJoinSpace = spaceRepository.findById(spaceInvitation.getSpace().getId())
				.orElseThrow(() -> new SpaceNotFoundException());

		User toJoinUser = userRepository.findById(spaceInvitation.getInvitee().getId())
				.orElseThrow(() -> new UserNotFoundException());

		if (!Objects.equals(toJoinUser.getId(), requestUserId)) {
			throw new InvitationPermissionDenied();
		}

		// 혹은 이미 들어가 있을 수도 있음 -> 중복 초대
		if (spaceMemberRepository.existsByUserIdAndSpaceId(toJoinUser.getId(), toJoinSpace.getId())) {
			throw new SpaceMemberAlreadyExist();
		}

		// SpaceMember에 추가

		SpaceMember joinedSpaceMember = new SpaceMember(toJoinSpace, toJoinUser, SpaceMemberRole.MEMBER);

		spaceMemberRepository.save(joinedSpaceMember);

		// 마지막에 ACCEPT 하고 저장
		spaceInvitation.accept();
		spaceInvitationRepository.save(spaceInvitation);

		return joinedSpaceMember.getId();

	}

	public List<InvitationResponse> getSentInvitationList(Long userId) {

		List<SpaceInvitation> invitationListByInviteeId = spaceInvitationRepository.findByInviter_Id(userId);

		return invitationListByInviteeId.stream().map(InvitationResponse::from).toList();

	}

	public List<InvitationResponse> getReceivedInvitationList(Long userId) {

		List<SpaceInvitation> invitationListByInviteeId = spaceInvitationRepository.findByInvitee_Id(userId);

		return invitationListByInviteeId.stream().map(InvitationResponse::from).toList();

	}

}
