package com.sai.backend.space.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sai.backend.space.domain.SpaceInvitation;
import com.sai.backend.space.domain.SpaceInvitationStatus;

public interface SpaceInvitationRepository extends JpaRepository<SpaceInvitation, Long>
{
	public List<SpaceInvitation> findBySpace_Id(Long spaceId);
	public List<SpaceInvitation> findByInviter_Id(Long inviterId);
	public List<SpaceInvitation> findByInvitee_Id(Long inviteeId);
	public boolean existsBySpace_IdAndInviter_IdAndInvitee_IdAndStatus(
			Long spaceId, Long inviterId, Long inviteeId, SpaceInvitationStatus status);
}
