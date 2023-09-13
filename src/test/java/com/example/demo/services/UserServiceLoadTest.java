package com.example.demo.services;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.impl.UserServiceRedis;
import com.example.demo.services.impl.UserServiceTransactional;
import com.example.demo.services.util.UserServiceOperationThread;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import static com.example.demo.services.util.UserServiceTestUtil.USER_TO_SAVE_3;
import static org.junit.jupiter.api.Assertions.*;

@EnableCaching
@SpringBootTest
@Disabled
public class UserServiceLoadTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserServiceRedis userServiceRedis;

    @Autowired
    private UserServiceTransactional userServiceTransactional;

    @Autowired
    private CacheManager cacheManager;

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceLoadTest.class);
    private static final String USERS_CACHE_NAME = "users";
    private static final String USERS_CACHE_KEY = "all";

    @BeforeAll
    public static void setUpContainers() {
        startMysqlContainer();
        startRedisContainer();
    }

    @Test
    public void testHighLoadPerformanceLinearScenario() {
        LOG.info("High load performance linear test is started.");

        int numberOfUsers = 100;
        int numberOfCycles = 1000;

        // Add users to the database
        long startTime = System.nanoTime();
        saveUsersToDatabase(numberOfUsers);
        long endTime = System.nanoTime();
        long elapsedTimeInMillis = (endTime - startTime) / 1_000_000;
        LOG.info("Elapsed time for adding {} users to the database: {} milliseconds.", numberOfUsers, elapsedTimeInMillis);

        // Ensure all users have been saved and put them into the cache
        assertEquals(numberOfUsers, userRepository.count());
        assertNull(Objects.requireNonNull(cacheManager.getCache(USERS_CACHE_NAME)).get(USERS_CACHE_KEY));
        userServiceRedis.listUsers();
        assertNotNull(Objects.requireNonNull(cacheManager.getCache(USERS_CACHE_NAME)).get(USERS_CACHE_KEY));

        // Start loading database from a cacheable service
        startTime = System.nanoTime();
        for (int i = 0; i < numberOfCycles; i++) {
            List<User> ignored = userServiceRedis.listUsers();
        }
        endTime = System.nanoTime();
        elapsedTimeInMillis = (endTime - startTime) / 1_000_000;
        LOG.info("Elapsed time for cacheable service: {} milliseconds.", elapsedTimeInMillis);

        // Start loading database from a non-cacheable service
        startTime = System.nanoTime();
        for (int i = 0; i < numberOfCycles; i++) {
            List<User> ignored = userServiceTransactional.listUsers();
        }
        endTime = System.nanoTime();
        elapsedTimeInMillis = (endTime - startTime) / 1_000_000;
        LOG.info("Elapsed time for non-cacheable service: {} milliseconds.", elapsedTimeInMillis);

        // Delete all users from the database
        userRepository.deleteAll();
        assertEquals(0, userRepository.count());

        LOG.info("High load performance linear test is finished.");
    }

    @Test
    public void testHighLoadPerformanceMultithreadedScenario() throws InterruptedException {
        LOG.info("High load performance multithreaded test is started.");

        int numberOfUsers = 100;
        int numberOfThreads = 11;
        int numberOfCycles = 1000;
        double listUsersProbability = 1.0;
        double saveUserProbability = 0.01;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // Add users to the database
        long startTime = System.nanoTime();
        saveUsersToDatabase(numberOfUsers);
        long endTime = System.nanoTime();
        long elapsedTimeInMillis = (endTime - startTime) / 1_000_000;
        LOG.info("Elapsed time for adding {} users to the database: {} milliseconds.", numberOfUsers, elapsedTimeInMillis);

        // Ensure all users have been saved and put them into the cache
        assertEquals(numberOfUsers, userRepository.count());
        assertNull(Objects.requireNonNull(cacheManager.getCache(USERS_CACHE_NAME)).get(USERS_CACHE_KEY));
        userServiceRedis.listUsers();
        assertNotNull(Objects.requireNonNull(cacheManager.getCache(USERS_CACHE_NAME)).get(USERS_CACHE_KEY));

        // Start threads that will call the listUsers method
        for (int i = 0; i < numberOfThreads - 1; i++) {
            UserService service = i % 2 == 0 ? userServiceRedis : userServiceTransactional;
            String suffix = i % 2 == 0 ? "cacheable" : "non-cacheable";
            UserServiceOperationThread listUsersThread = new UserServiceOperationThread(latch, numberOfCycles, listUsersProbability, () -> {
                service.listUsers();
                return null;
            });
            listUsersThread.setName("ListUsersThread-" + suffix + "-" + i);
            listUsersThread.start();
        }

        // Start thread that will call the saveUser method
        UserServiceOperationThread saveUserThread = new UserServiceOperationThread(latch, numberOfCycles, saveUserProbability, () -> {
            userServiceRedis.saveUser(createNewUser());
            userServiceRedis.reloadUsers();
            return null;
        });
        saveUserThread.setName("SaveUserThread");
        saveUserThread.start();

        // Wait until each thread is finished
        latch.await();

        // Delete all users from the database
        userRepository.deleteAll();
        assertEquals(0, userRepository.count());

        LOG.info("High load performance multithreaded test is finished.");
    }

    private void saveUsersToDatabase(int numberOfUsers) {
        for (int i = 0; i < numberOfUsers; i++) {
            userRepository.save(createNewUser());
        }
    }

    private User createNewUser() {
        return USER_TO_SAVE_3.clone();
    }

    @SuppressWarnings("resource")
    private static void startMysqlContainer() {
        try {
            int mysqlPort = Integer.parseInt(System.getProperty("mysql.port", "3306"));
            String dbName = System.getProperty("db.schema", "test_db");
            String username = System.getProperty("db.user", "test");
            String password = System.getProperty("db.password", "test");
            MySQLContainer<?> mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"))
                    .withExposedPorts(mysqlPort)
                    .withDatabaseName(dbName)
                    .withUsername(username)
                    .withPassword(password);
            mysqlContainer.start();
            System.setProperty("spring.datasource.url", mysqlContainer.getJdbcUrl());
            System.setProperty("spring.datasource.username", mysqlContainer.getUsername());
            System.setProperty("spring.datasource.password", mysqlContainer.getPassword());
        } catch (Exception e) {
            LOG.error("An exception occurred during starting of MySQL container: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("resource")
    private static void startRedisContainer() {
        try {
            int redisPort = Integer.parseInt(System.getProperty("redis.port", "6379"));
            GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest"))
                    .withExposedPorts(redisPort);
            redisContainer.start();
            System.setProperty("spring.data.redis.host", redisContainer.getHost());
            System.setProperty("spring.data.redis.port", redisContainer.getMappedPort(redisPort).toString());
        } catch (Exception e) {
            LOG.error("An exception occurred during starting of Redis container: {}", e.getMessage(), e);
        }
    }
}
