package com.seer.fitness.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = {"com.seer.fitness"})
public class SeerFitnessEduApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeerFitnessEduApplication.class, args);
    }

}
