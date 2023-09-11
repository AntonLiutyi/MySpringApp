package com.example.demo.services;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.impl.UserServiceRedis;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

import static com.example.demo.services.util.UserServiceTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

@EnableCaching
@SpringBootTest
@AutoConfigureTestDatabase
public class UserServiceRedisTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserServiceRedis userService;

    @Autowired
    private CacheManager cacheManager;

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceRedisTest.class);

    @BeforeAll
    @SuppressWarnings("resource")
    public static void startRedisContainer() {
        try {
            int redisPort = Integer.parseInt(System.getProperty("redis.port", "6379"));
            GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(redisPort);
            redisContainer.start();
            System.setProperty("spring.data.redis.host", redisContainer.getHost());
            System.setProperty("spring.data.redis.port", redisContainer.getMappedPort(redisPort).toString());
        } catch (Exception e) {
            LOG.error("Starting of Redis container failed with following message: " + e.getMessage());
        }
    }

    @AfterEach
    public void cleanupCache() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = Objects.requireNonNull(cacheManager.getCache(cacheName));
            cache.clear();
        });
    }

    @AfterEach
    public void resetDatabase(ApplicationContext applicationContext) throws SQLException {
        userRepository.deleteAll();
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        Connection c = dataSource.getConnection();
        Statement s = c.createStatement();
        s.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    public void Should_FindNoUsers_When_DatabaseIsEmpty() {
        List<User> users = userService.listUsers();
        assertEquals(0, users.size());
    }

    @Test
    public void Should_FindAllUsers_When_DatabaseIsNotEmpty() {
        saveUsersToDatabase();
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2, PERSISTED_USER_3), users);
    }

    @Test
    public void Should_SaveUser_When_UserIsValid() {
        User user = userService.saveUser(USER_TO_SAVE_1);
        assertEquals(1L, userRepository.count());
        assertEquals(PERSISTED_USER_1, user);
    }

    @Test
    public void Should_NotSaveUserAndThrowInvalidDataAccessApiUsageException_When_UserIsNull() {
        assertThrows(InvalidDataAccessApiUsageException.class, () -> userService.saveUser(null));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_NotSaveUserAndThrowDataIntegrityViolationException_When_UserFirstNameIsNull() {
        assertThrows(DataIntegrityViolationException.class, () -> userService.saveUser(USER_WITHOUT_FIRST_NAME));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_NotSaveUserAndThrowDataIntegrityViolationException_When_UserLastNameIsNull() {
        assertThrows(DataIntegrityViolationException.class, () -> userService.saveUser(USER_WITHOUT_LAST_NAME));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_NotSaveUserAndThrowDataIntegrityViolationException_When_UserGenderIsNull() {
        assertThrows(DataIntegrityViolationException.class, () -> userService.saveUser(USER_WITHOUT_GENDER));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_DeleteUserById_When_UserExistsInDatabase() {
        saveUsersToDatabase();
        userService.deleteUser(2L);
        assertEquals(2L, userRepository.count());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_3), users);
    }

    @Test
    public void Should_NotDeleteAnyUsers_When_ThereAreNoUserWithProvidedIdInDatabase() {
        saveUsersToDatabase();
        userService.deleteUser(5L);
        assertEquals(3L, userRepository.count());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2, PERSISTED_USER_3), users);
    }

    @Test
    public void Should_NotDeleteAnyUsersAndThrowInvalidDataAccessApiUsageException_When_ProvidedIdIsNull() {
        saveUsersToDatabase();
        assertThrows(InvalidDataAccessApiUsageException.class, () -> userService.deleteUser(null));
        assertEquals(3L, userRepository.count());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2, PERSISTED_USER_3), users);
    }

    @Test
    public void Should_NotExistUsersAllCache_When_ListUsersIsNotCalled() {
        assertNull(Objects.requireNonNull(cacheManager.getCache("users")).get("all"));
    }

    @Test
    public void Should_ExistUsersAllCache_When_ListUsersIsCalled() {
        userService.listUsers();
        assertNotNull(Objects.requireNonNull(cacheManager.getCache("users")).get("all"));
    }

    @Test
    public void Should_NotExistUsersAllCache_When_ReloadUsersIsCalledAfterListUsersCall() {
        saveUsersToDatabase();
        userService.listUsers();
        userService.reloadUsers();
        assertNull(Objects.requireNonNull(cacheManager.getCache("users")).get("all"));
    }

    @Test
    public void Should_SaveUserWithUniqueIdInCache_When_SaveUserIsCalled() {
        User user1 = userService.saveUser(USER_TO_SAVE_1);
        assertNotNull(Objects.requireNonNull(cacheManager.getCache("users")).get(user1.getId()));
        User user2 = userService.saveUser(USER_TO_SAVE_2);
        assertNotNull(Objects.requireNonNull(cacheManager.getCache("users")).get(user2.getId()));
        User user3 = userService.saveUser(USER_TO_SAVE_3);
        assertNotNull(Objects.requireNonNull(cacheManager.getCache("users")).get(user3.getId()));
    }

    @Test
    public void Should_DeleteUserWithUniqueIdFromCache_When_DeleteUserIsCalled() {
        User user1 = userService.saveUser(USER_TO_SAVE_1);
        User user2 = userService.saveUser(USER_TO_SAVE_2);
        userService.deleteUser(user1.getId());
        assertNull(Objects.requireNonNull(cacheManager.getCache("users")).get(user1.getId()));
        userService.deleteUser(user2.getId());
        assertNull(Objects.requireNonNull(cacheManager.getCache("users")).get(user2.getId()));
    }

    private void saveUsersToDatabase() {
        userRepository.save(USER_TO_SAVE_1);
        userRepository.save(USER_TO_SAVE_2);
        userRepository.save(USER_TO_SAVE_3);
    }
}
