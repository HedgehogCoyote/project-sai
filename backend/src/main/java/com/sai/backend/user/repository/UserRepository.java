package com.sai.backend.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sai.backend.user.domain.User;

// Interface로 만들고, JpaRepository 상속 -> 도메인 클래스 , ID 자료형 
public interface UserRepository extends JpaRepository<User, Long> {
	
	// findBy필드명 으로하면 자동 생성
	Optional<User> findByLoginId(String loginId);

	List<User> findTop10ByLoginIdContainingIgnoreCaseOrderByLoginId(String loginId);
	
	// existBy필드명 으로 하면 자동 생성
	boolean existsByLoginId(String loginId);

}
