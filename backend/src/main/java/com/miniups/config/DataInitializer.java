package com.miniups.config;

import com.miniups.model.entity.User;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.UserRole;
import com.miniups.model.enums.TruckStatus;
import com.miniups.repository.UserRepository;
import com.miniups.repository.TruckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Data Initializer - Creates default users and trucks on application startup
 * Only runs if no users exist in the database
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeUsers();
        // TODO: Fix truck initialization issue
        // initializeTrucks();
    }

    private void initializeUsers() {
        if (userRepository.count() == 0) {
            logger.info("No users found in database. Creating default users...");

            // Admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@miniups.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setRole(UserRole.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
            logger.info("Created admin user: admin@miniups.com");

            // Operator user
            User operator = new User();
            operator.setUsername("operator");
            operator.setEmail("operator@miniups.com");
            operator.setPassword(passwordEncoder.encode("operator123"));
            operator.setFirstName("UPS");
            operator.setLastName("Operator");
            operator.setRole(UserRole.OPERATOR);
            operator.setEnabled(true);
            userRepository.save(operator);
            logger.info("Created operator user: operator@miniups.com");

            // Test user
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setEmail("testuser@miniups.com");
            testUser.setPassword(passwordEncoder.encode("testpassword"));
            testUser.setFirstName("Test");
            testUser.setLastName("User");
            testUser.setRole(UserRole.USER);
            testUser.setEnabled(true);
            userRepository.save(testUser);
            logger.info("Created test user: testuser@miniups.com");

            // Test admin user
            User testAdmin = new User();
            testAdmin.setUsername("testadmin");
            testAdmin.setEmail("testadmin@miniups.com");
            testAdmin.setPassword(passwordEncoder.encode("adminpass"));
            testAdmin.setFirstName("Test");
            testAdmin.setLastName("Admin");
            testAdmin.setRole(UserRole.ADMIN);
            testAdmin.setEnabled(true);
            userRepository.save(testAdmin);
            logger.info("Created test admin user: testadmin@miniups.com");

            logger.info("Default users created successfully");
        } else {
            logger.info("Users already exist in database. Skipping user initialization.");
        }
    }

    private void initializeTrucks() {
        if (truckRepository.count() == 0) {
            logger.info("No trucks found in database. Creating initial trucks...");

            // Create 5 initial trucks
            int[][] truckPositions = {{0, 0}, {10, 10}, {20, 20}, {30, 30}, {40, 40}};
            int[] truckCapacities = {100, 150, 200, 120, 180};

            for (int i = 0; i < 5; i++) {
                Truck truck = new Truck();
                // Generate truck ID using database function starting from 1001
                truck.setTruckId(1001 + i);
                truck.setStatus(TruckStatus.IDLE);
                truck.setCurrentX(truckPositions[i][0]);
                truck.setCurrentY(truckPositions[i][1]);
                truck.setCapacity(truckCapacities[i]);
                truckRepository.save(truck);
            }

            logger.info("Created {} initial trucks", 5);
        } else {
            logger.info("Trucks already exist in database. Skipping truck initialization.");
        }
    }
}