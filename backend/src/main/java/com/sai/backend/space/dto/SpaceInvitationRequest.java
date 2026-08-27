package com.sai.backend.space.dto;

public record SpaceInvitationRequest(
		Long spaceId,
		Long inviteeUserId
		) 
{
}
