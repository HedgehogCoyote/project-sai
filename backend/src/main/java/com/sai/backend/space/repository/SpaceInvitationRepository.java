package com.sai.backend.space.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sai.backend.space.domain.SpaceInvitation;

public interface SpaceInvitationRepository extends JpaRepository<SpaceInvitation, Long>
{
	public List<SpaceInvitation> findBySpace_Id(Long spaceId);
	public List<SpaceInvitation> findByInviter_Id(Long inviterId);
	public List<SpaceInvitation> findByInvitee_Id(Long inviteeId);
}
