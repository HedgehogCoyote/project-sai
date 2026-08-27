package com.sai.backend.space.domain;

import java.time.LocalDateTime;

import com.sai.backend.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "space_invitation")
@Getter
public class SpaceInvitation {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "space_id", nullable = false)
	private Space space;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inviter_user_id", nullable = false)
	private User inviter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invitee_user_id", nullable = false)
	private User invitee;
	
	@Column(name = "invited_at", nullable = false)
	private LocalDateTime invitedAt;
	
	@Column(name = "status", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private SpaceInvitationStatus status;
	
	protected SpaceInvitation() {
		
	}
	
	public SpaceInvitation(Space space, User inviter, User invitee) {
		this.space = space;
		this.inviter = inviter;
		this.invitee = invitee;
	}
	
	
	@PrePersist
	private void prePersist() {
		invitedAt = LocalDateTime.now();
		status = SpaceInvitationStatus.PENDING;
	}
	
	
}
