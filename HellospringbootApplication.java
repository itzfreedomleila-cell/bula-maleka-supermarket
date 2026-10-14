package net.codejava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the Bula Maleka Supermarket System. Spring finds every module because
 * they all live in sub-packages of net.codejava.
 */
@SpringBootApplication
public class HellospringbootApplication {

	public static void main(String[] args) {
		SpringApplication.run(HellospringbootApplication.class, args);
	}

}
