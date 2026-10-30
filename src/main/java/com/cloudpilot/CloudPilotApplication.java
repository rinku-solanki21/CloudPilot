package com.cloudpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CloudPilotApplication
 * 
 * Main entry point for the CloudPilot Spring Boot application.
 * Starts the embedded Tomcat web server on port 8080 and initializes
 * all Spring beans (Services, Controllers, and Background Threads).
 * 
 * B.Tech 3rd Semester OOP Project
 */
@SpringBootApplication
public class CloudPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudPilotApplication.class, args);
        System.out.println("=================================================");
        System.out.println("  CloudPilot - Cloud VM Load Balancer & Auto-Scaler");
        System.out.println("  Server running at: http://localhost:8080");
        System.out.println("=================================================");
    }
}
