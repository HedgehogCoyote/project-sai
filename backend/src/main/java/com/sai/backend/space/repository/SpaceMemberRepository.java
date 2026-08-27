package com.sai.backend.space.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sai.backend.space.domain.SpaceMember;

public interface SpaceMemberRepository extends JpaRepository<SpaceMember, Long> {
	
	public List<SpaceMember> findByUserId(Long userId);
	public Optional<SpaceMember> findByUserIdAndSpaceId(Long userId, Long spaceId);
	public Integer countBySpaceId(Long spaceId);
	public boolean existsByUserIdAndSpaceId(Long userId, Long spaceId);
}
