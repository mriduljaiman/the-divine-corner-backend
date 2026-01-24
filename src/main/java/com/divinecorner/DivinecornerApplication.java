package com.divinecorner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DivinecornerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DivinecornerApplication.class, args);
	}

}
