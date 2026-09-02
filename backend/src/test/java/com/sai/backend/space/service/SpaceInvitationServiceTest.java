package com.sai.backend.space.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sai.backend.space.domain.Space;
import com.sai.backend.space.dto.SpaceInvitationRequest;
import com.sai.backend.space.exception.SelfInvitationException;
import com.sai.backend.space.repository.SpaceRepository;
import com.sai.backend.user.domain.User;
import com.sai.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class SpaceInvitationServiceTest {

	
	@Mock
	private UserRepository userRepository;
	
	@Mock
	private SpaceRepository spaceRepository;
	
	@InjectMocks
	private SpaceInvitationService spaceInvitationService;
	
	@Test
	public void 스스로_초대하기() throws Exception {
		
		Long myId = 1L;
		Long spaceId = 1L;
		
        User mockUser = new User(
                "kang",
                "testuser",
                "encoded",
                "010-1234-5678",
                "test@example.com");
		
        Space space = new Space("My Little Home");
        
        given(userRepository.findById(myId))
        	.willReturn(Optional.of(mockUser));
        
        given(spaceRepository.findById(spaceId))
        	.willReturn(Optional.of(space));
        
        SpaceInvitationRequest invitationRequest =
        		new SpaceInvitationRequest(spaceId, myId);
        
		assertThrows(
				SelfInvitationException.class,
				() -> spaceInvitationService.invite(myId,  invitationRequest)
				);
		
	}
	
}
