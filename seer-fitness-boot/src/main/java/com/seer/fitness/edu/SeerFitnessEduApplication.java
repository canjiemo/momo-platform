package com.seer.fitness.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.seer.fitness"})
public class SeerFitnessEduApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeerFitnessEduApplication.class, args);
    }

}
