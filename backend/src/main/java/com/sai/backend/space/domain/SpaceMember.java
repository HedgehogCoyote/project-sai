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
@Table(name = "space_member")
@Getter
public class SpaceMember {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "space_id", nullable = false)
	private Space space;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "role",
			nullable = false,
			length = 20)
	private SpaceMemberRole role;
	
	@Column(name = "joined_at", 
			updatable = false,
			nullable = false)
	private LocalDateTime joinedAt;
	
	
	
	protected SpaceMember() {
		
	}
	
	public SpaceMember(Space space, User user, SpaceMemberRole role) {
		this.space = space;
		this.user = user;
		this.role = role;
	}
	
	@PrePersist
	public void PrePersist() {
		this.joinedAt = LocalDateTime.now();
	}
	
	
	
	
}
