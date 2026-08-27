package com.sai.backend.space.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sai.backend.auth.exception.UserNotFoundException;
import com.sai.backend.space.domain.Space;
import com.sai.backend.space.domain.SpaceMember;
import com.sai.backend.space.domain.SpaceMemberRole;
import com.sai.backend.space.dto.CreateSpaceRequest;
import com.sai.backend.space.dto.ParticipatingSpacesResponse;
import com.sai.backend.space.repository.SpaceMemberRepository;
import com.sai.backend.space.repository.SpaceRepository;
import com.sai.backend.user.domain.User;
import com.sai.backend.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpaceService {

	private final SpaceRepository spaceRepository;
	private final UserRepository userRepository;
	private final SpaceMemberRepository spaceMemberRepository;
	
	@Transactional
	public Long createSpace(CreateSpaceRequest createSpaceRequest, Long userId) {
		
		User requestUser = userRepository.findById(userId).orElseThrow(
				() -> new UserNotFoundException() 
			);
	
		
		Space space = new Space(createSpaceRequest.title());
		
		// TODO : 한사람당 최대 Space, 혹은 빈도 수를 제한 둘 것인지 체크
		spaceRepository.save(space);
		
		
		SpaceMember spaceMember = 
				new SpaceMember(space, requestUser, SpaceMemberRole.OWNER);
		
		
		spaceMemberRepository.save(spaceMember);
		
		
		return space.getId();
	}
	
	public List<ParticipatingSpacesResponse> getMySpaceList(Long userId){
		
		// spaceMember에서 userId가 일치하는 것을 가져온다
		return  spaceMemberRepository.findByUserId(userId)
			.stream()
			.map(spaceMember -> new ParticipatingSpacesResponse (
						spaceMember.getSpace().getId(), 
						spaceMember.getSpace().getTitle(),
						spaceMember.getRole().toString(),
						spaceMemberRepository.countBySpaceId(spaceMember.getSpace().getId())
					)
					)
			.toList();
	
	}
	
}
