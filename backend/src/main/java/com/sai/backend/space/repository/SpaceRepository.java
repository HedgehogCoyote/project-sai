package com.sai.backend.space.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sai.backend.space.domain.Space;

public interface SpaceRepository extends JpaRepository<Space, Long>{
	

}
