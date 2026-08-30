package com.sai.backend.space.service;

import com.sai.backend.auth.exception.UserNotFoundException;
import com.sai.backend.space.domain.Space;
import com.sai.backend.space.domain.SpaceMember;
import com.sai.backend.space.domain.SpaceMemberRole;
import com.sai.backend.space.dto.CreateSpaceRequest;
import com.sai.backend.space.dto.ParticipatingSpacesResponse;
import com.sai.backend.space.repository.SpaceMemberRepository;
import com.sai.backend.space.repository.SpaceRepository;
import com.sai.backend.user.domain.*;
import com.sai.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class SpaceServiceTest {

    @MockitoBean
    public SpaceRepository spaceRepository;

    @MockitoBean
    public SpaceMemberRepository spaceMemberRepository;

    @MockitoBean
    public UserRepository userRepository;


    @InjectMocks
    private SpaceService spaceService;

    @Test
    public void 스페이스_정상생성_레포지터리_체크() throws Exception{

        Long goodId = 1L;

        User mockUser = new User(
                "kang",
                "testuser",
                "encoded",
                "010-1234-5678",
                "test@example.com");

        // Space mockSpace = new Space("My Little");

        given(userRepository.findById(1L))
                .willReturn(Optional.of(mockUser));

        CreateSpaceRequest createSpaceRequest =
                new CreateSpaceRequest("My Little");

        spaceService.createSpace(createSpaceRequest, goodId);

        then(spaceRepository).should().save(any(Space.class));


    }

    @Test
    public void 잘못된_ID의_스페이스_생성요청() throws Exception{

        Long wrongId = -999L;

        given(userRepository.findById(wrongId))
                .willReturn(Optional.empty());

        CreateSpaceRequest createSpaceRequest =
                new CreateSpaceRequest("My Little");

        assertThrows(
                UserNotFoundException.class,
                () -> spaceService.createSpace(createSpaceRequest, wrongId)
        );

        then(spaceRepository).shouldHaveNoInteractions();

    }

    @Test
    public void 스페이스_생성시_오너_체크() throws Exception {
        Long goodId = 1L;

        User mockUser = new User(
                "kang",
                "testuser",
                "encoded",
                "010-1234-5678",
                "test@example.com");


        given(userRepository.findById(goodId))
                .willReturn(Optional.of(mockUser));

        CreateSpaceRequest createSpaceRequest =
                new CreateSpaceRequest("My Little");

        spaceService.createSpace(createSpaceRequest, goodId);

        // !!!! 복습
        then(spaceRepository).should()
                .save(argThat(space ->
                        space.getTitle().equals("My Little")
                ));

        then(spaceMemberRepository).should()
                .save(argThat(member ->
                        member.getUser().equals(mockUser)
                                && member.getRole() == SpaceMemberRole.OWNER
                                && member.getSpace().getTitle().equals("My Little")
                ));
    }

    @Test
    void 참여중인_스페이스를_조회해_DTO로_반환한다(){

        Long mockUserId = 1L;

        Space mockSpace = mock(Space.class);
        SpaceMember mockSpaceMember = mock(SpaceMember.class);

        given(mockSpace.getId()).willReturn(10L);
        given(mockSpace.getTitle()).willReturn("My Little");

        given(mockSpaceMember.getSpace()).willReturn(mockSpace);
        given(mockSpaceMember.getRole()).willReturn(SpaceMemberRole.OWNER);

        given(spaceMemberRepository.findByUserId(mockUserId))
                .willReturn(List.of(mockSpaceMember));

        given(spaceMemberRepository.countBySpaceId(10L))
                .willReturn(3);

        // when
        List<ParticipatingSpacesResponse> result =
                spaceService.getMySpaceList(mockUserId);

        // then
        then(spaceMemberRepository).should()
                .findByUserId(mockUserId);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).spaceId());
        assertEquals("My Little", result.get(0).title());
        assertEquals("OWNER", result.get(0).role());
        assertEquals(3, result.get(0).spaceMemberCount());
    }


}
