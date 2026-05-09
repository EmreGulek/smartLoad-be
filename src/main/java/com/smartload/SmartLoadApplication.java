package com.smartload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SmartLoad backend entry point.
 *
 * Phase 0: Health endpoint only.
 * Phase 1+<: REST controllers, JPA persistence, Excel ingest, bin packing service.
 *
 * See MASTER-PLAN.md for the phase roadmap.
 */
@SpringBootApplication
public class SmartLoadApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLoadApplication.class, args);
    }
}
