package com.example.reservation_solution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ReservationSolutionApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationSolutionApplication.class, args);
	}

}
