package com.sai.backend;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Component
public class CheckDBConnection implements ApplicationRunner {
	
	   private final JdbcTemplate jdbcTemplate;

	   public CheckDBConnection(JdbcTemplate jdbcTemplate) {
	        this.jdbcTemplate = jdbcTemplate;
	    }

	@Override
	public void run(ApplicationArguments args) {
	        Integer countEmail = jdbcTemplate.queryForObject(
	                "SELECT COUNT(email) FROM USERS LIMIT 1",
	                Integer.class
	                
	        );

	        System.out.println("Email 개수: " + countEmail);
	}
}
