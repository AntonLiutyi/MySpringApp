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

    private final User userToSave1 = new User("Alice", "Smith", User.Gender.FEMALE);
    private final User userToSave2 = new User("Bob", "Johnson", User.Gender.MALE);
    private final User userToSave3 = new User("Terry", "Jerry", User.Gender.ATTACK_HELICOPTER);
    private final User persistedUser1 = new User(1L, "Alice", "Smith", User.Gender.FEMALE, "alice.smith@example.com");
    private final User persistedUser2 = new User(2L, "Bob", "Johnson", User.Gender.MALE, "bob.johnson@example.com");
    private final User persistedUser3 = new User(3L, "Terry", "Jerry", User.Gender.ATTACK_HELICOPTER, "terry.jerry@example.com");
    private final User userWithoutFirstName = new User(null, "Giggles", User.Gender.OTHER);
    private final User userWithoutLastName = new User("Chuckles", null, User.Gender.OTHER);
    private final User userWithoutGender = new User("Riddle", "Riddle", null);

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
    public void Should_FindNoUsers_When_RepositoryIsEmpty() {
        List<User> users = userService.listUsers();
        assertEquals(0, users.size());
    }

    @Test
    public void Should_FindAllUsers_When_RepositoryIsNotEmpty() {
        saveAllUsersToDatabase();
        List<User> users = userService.listUsers();
        assertEquals(List.of(persistedUser1, persistedUser2, persistedUser3), users);
    }

    @Test
    public void Should_SaveUser_When_UserIsValid() {
        User user = userService.saveUser(userToSave1);
        assertEquals(1L, userRepository.count());
        assertEquals(persistedUser1, user);
    }

    @Test
    public void Should_NotSaveUser_When_UserIsNull() {
        assertThrows(InvalidDataAccessApiUsageException.class, () -> userService.saveUser(null));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_NotSaveUser_When_UserFirstNameIsNull() {
        assertThrows(DataIntegrityViolationException.class, () -> userService.saveUser(userWithoutFirstName));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_NotSaveUser_When_UserLastNameIsNull() {
        assertThrows(DataIntegrityViolationException.class, () -> userService.saveUser(userWithoutLastName));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_NotSaveUser_When_UserGenderIsNull() {
        assertThrows(DataIntegrityViolationException.class, () -> userService.saveUser(userWithoutGender));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_DeleteUserById_When_UserExistsInRepository() {
        saveAllUsersToDatabase();
        userService.deleteUser(2L);
        assertEquals(2L, userRepository.count());
        List<User> users = userService.listUsers();
        assertEquals(List.of(persistedUser1, persistedUser3), users);
    }

    @Test
    public void Should_NotDeleteAnyUsers_When_ThereAreNoUserWithProvidedIdInRepository() {
        saveAllUsersToDatabase();
        userService.deleteUser(5L);
        assertEquals(3L, userRepository.count());
        List<User> users = userService.listUsers();
        assertEquals(List.of(persistedUser1, persistedUser2, persistedUser3), users);
    }

    @Test
    public void Should_NotDeleteAnyUsers_When_ProvidedIdIsNull() {
        saveAllUsersToDatabase();
        assertThrows(InvalidDataAccessApiUsageException.class, () -> userService.deleteUser(null));
        assertEquals(3L, userRepository.count());
        List<User> users = userService.listUsers();
        assertEquals(List.of(persistedUser1, persistedUser2, persistedUser3), users);
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
    public void Should_NotExistUsersAllCache_When_ReloadUsersIsCalledAfterListUsers() {
        saveAllUsersToDatabase();
        userService.listUsers();
        userService.reloadUsers();
        assertNull(Objects.requireNonNull(cacheManager.getCache("users")).get("all"));
    }

    @Test
    public void Should_SaveUserWithUniqueIdInCache_When_SaveUserIsCalled() {
        User user1 = userService.saveUser(userToSave1);
        assertNotNull(Objects.requireNonNull(cacheManager.getCache("users")).get(user1.getId()));
        User user2 = userService.saveUser(userToSave2);
        assertNotNull(Objects.requireNonNull(cacheManager.getCache("users")).get(user2.getId()));
        User user3 = userService.saveUser(userToSave3);
        assertNotNull(Objects.requireNonNull(cacheManager.getCache("users")).get(user3.getId()));
    }

    @Test
    public void Should_DeleteUserFromCache_When_DeleteUserIsCalled() {
        User user1 = userService.saveUser(userToSave1);
        User user2 = userService.saveUser(userToSave2);
        userService.deleteUser(user1.getId());
        assertNull(Objects.requireNonNull(cacheManager.getCache("users")).get(user1.getId()));
        userService.deleteUser(user2.getId());
        assertNull(Objects.requireNonNull(cacheManager.getCache("users")).get(user2.getId()));
    }

    private void saveAllUsersToDatabase() {
        userRepository.save(userToSave1);
        userRepository.save(userToSave2);
        userRepository.save(userToSave3);
    }
}
