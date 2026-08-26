package com.sai.backend.space.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "space")
@Getter
public class Space {
	
	// FIELD 
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "title",
			nullable = false,
			length = 50)
	private String title;
	
	@Column(name = "created_at",
			nullable = false,
			updatable = false)
	private LocalDateTime createdAt;
	
	// Constructor 
	protected Space() {
		
	}
	
	public Space(String title) {
		super();
		this.title = title;
	}
	
	
	
	@PrePersist
	private void prePersist()
	{
		this.createdAt = LocalDateTime.now();
	}
	
	
}
