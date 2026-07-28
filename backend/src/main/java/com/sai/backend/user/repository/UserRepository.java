package com.sai.backend.user.repository;

import java.util.Optional;

import com.sai.backend.user.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;

// Interface로 만들고, JpaRepository 상속 -> 도메인 클래스 , ID 자료형 
public interface UserRepository extends JpaRepository<User, Long> {
	
	// findBy필드명 으로하면 자동 생성
	public Optional<User> findByLoginId(String loginId);
	
	// existBy필드명 으로 하면 자동 생성
	public boolean existsByLoginId(String loginId);

}
