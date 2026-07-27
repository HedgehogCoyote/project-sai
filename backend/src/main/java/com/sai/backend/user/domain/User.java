package com.sai.backend.user.domain;


import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.persistence.Column;

import jakarta.persistence.PrePersist;

@Entity
@Table(name = "users")
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "name",
			nullable = false,
			length = 50
			)
	private String name;
	
	
	@Column(
			name = "login_id",
			nullable = false,
			unique = true,
			length = 50
			)
	private String loginId;
	
	@Column(name = "password_hash",
			nullable = false,
			length = 255)
	private String passwordHash;
	
	@Column(name = "created_at",
			nullable = false,
			updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "phone_number",
			nullable = true,
			length = 20)
	private String phoneNumber;
	
	@Column(name = "email",
			nullable = true,
			length = 255)
	private String email;
	
	protected User()
	{
		
	}
	
	
	public User(String name, String loginId, String passwordHash, String phoneNumber, String email) {
		super();
		this.name = name;
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.phoneNumber = phoneNumber;
		this.email = email;
	}


	public Long getId() {
		return id;
	}


	public String getName() {
		return name;
	}


	public String getLoginId() {
		return loginId;
	}


	public String getPasswordHash() {
		return passwordHash;
	}


	public LocalDateTime getCreatedAt() {
		return createdAt;
	}


	public String getPhoneNumber() {
		return phoneNumber;
	}


	public String getEmail() {
		return email;
	}
	
	@PrePersist
	private void prePersist()
	{
		this.createdAt = LocalDateTime.now();
	}
	

	
	
	
}
