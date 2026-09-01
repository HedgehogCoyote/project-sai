package com.sai.backend.space.dto;

import com.sai.backend.space.domain.SpaceInvitation;
import com.sai.backend.space.domain.SpaceInvitationStatus;

public record InvitationResponse(
		Long invititationId,
		Long spaceId,
		String spaceName, 
		Long inviterId,
		String inviterName,
		Long inviteeId,
		String inviteeName,
		SpaceInvitationStatus invitationStatus
		
		) {

	
	public static InvitationResponse from(SpaceInvitation invitation) {
	    return new InvitationResponse(
	            invitation.getId(),
	            invitation.getSpace().getId(),
	            invitation.getSpace().getTitle(),
	            invitation.getInviter().getId(),
	            invitation.getInviter().getName(),
	            invitation.getInvitee().getId(),
	            invitation.getInvitee().getName(),
	            invitation.getStatus()
	    );
	}
}
