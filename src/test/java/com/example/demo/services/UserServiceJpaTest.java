package com.example.demo.services;

import com.example.demo.models.User;
import com.example.demo.services.impl.UserServiceJpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ComponentScan(basePackages = {"com.example.demo.services"})
public class UserServiceJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserServiceJpa userService;

    private final User userToSave1 = new User("Alice", "Smith", User.Gender.FEMALE);
    private final User userToSave2 = new User("Bob", "Johnson", User.Gender.MALE);
    private final User userToSave3 = new User("Terry", "Jerry", User.Gender.ATTACK_HELICOPTER);
    private final User persistedUser1 = new User(1L, "Alice", "Smith", User.Gender.FEMALE, "alice.smith@example.com");
    private final User persistedUser2 = new User(2L, "Bob", "Johnson", User.Gender.MALE, "bob.johnson@example.com");
    private final User persistedUser3 = new User(3L, "Terry", "Jerry", User.Gender.ATTACK_HELICOPTER, "terry.jerry@example.com");
    private final User userWithoutFirstName = new User(null, "Giggles", User.Gender.OTHER);
    private final User userWithoutLastName = new User("Chuckles", null, User.Gender.OTHER);
    private final User userWithoutGender = new User("Riddle", "Riddle", null);

    @AfterEach
    public void resetDatabase() {
        entityManager.getEntityManager()
                .createNativeQuery("ALTER TABLE users ALTER COLUMN id RESTART WITH 1")
                .executeUpdate();
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
        assertEquals(List.of(persistedUser1, persistedUser2, persistedUser3), users);
    }

    @Test
    public void Should_ThrowIllegalArgumentException_When_IdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userService.findUser(null));
    }

    @Test
    public void Should_NotFindUser_When_ThereAreNoUserWithProvidedIdInDatabase() {
        saveUsersToDatabase();
        assertNull(userService.findUser(5L));
    }

    @Test
    public void Should_FindUserWithMatchingId_When_IdIsValid() {
        saveUsersToDatabase();
        User user = userService.findUser(2L);
        assertEquals(persistedUser2, user);
    }

    @Test
    public void Should_ThrowNullPointerException_When_ListOfIdsIsNull() {
        assertThrows(NullPointerException.class, () -> userService.findAllUsersByIds(null));
    }

    @Test
    public void Should_FindOnlyUsersWithMatchingIds_When_IdsAreValid() {
        saveUsersToDatabase();
        List<User> users = userService.findAllUsersByIds(List.of(0L, 3L, 5L, 8L));
        assertEquals(List.of(persistedUser3), users);
    }

    @Test
    public void Should_FindOnlyUsersWithMatchingIdsAndNotThrowException_When_SomeIdsAreNull() {
        saveUsersToDatabase();
        List<Long> userIds = new ArrayList<>() {{
            add(0L);
            add(2L);
            add(null);
            add(3L);
            add(5L);
        }};
        AtomicReference<List<User>> users = new AtomicReference<>();
        Assertions.assertDoesNotThrow(() -> users.set(userService.findAllUsersByIds(userIds)));
        assertEquals(List.of(persistedUser2, persistedUser3), users.get());
    }

    @Test
    public void Should_FindNoUserIds_When_DatabaseIsEmpty() {
        List<Long> userIds = userService.getAllUserIds();
        assertEquals(0, userIds.size());
    }

    @Test
    public void Should_FindAllUserIds_When_DatabaseIsNotEmpty() {
        saveUsersToDatabase();
        List<Long> userIds = userService.getAllUserIds();
        assertEquals(countUsersInDatabase(), userIds.size());
        assertEquals(List.of(1L, 2L, 3L), userIds);
    }

    @Test
    public void Should_SaveUser_When_UserIsValid() {
        AtomicReference<User> user = new AtomicReference<>();
        assertDoesNotThrow(() -> user.set(userService.saveUser(userToSave1)));
        assertEquals(1L, countUsersInDatabase());
        assertEquals(persistedUser1, user.get());
    }

    @Test
    public void Should_NotSaveUserAndThrowNullPointerException_When_UserIsNull() {
        assertThrows(NullPointerException.class, () -> userService.saveUser(null));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveUserAndThrowIllegalArgumentException_When_UserFirstNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userService.saveUser(userWithoutFirstName));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveUserAndThrowIllegalArgumentException_When_UserLastNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userService.saveUser(userWithoutLastName));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveUserAndThrowIllegalArgumentException_When_UserGenderIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userService.saveUser(userWithoutGender));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_SaveAllUsers_When_UsersAreValid() {
        assertDoesNotThrow(() -> userService.saveUsers(List.of(userToSave1, userToSave2, userToSave3)));
        assertEquals(3L, countUsersInDatabase());
        List<User> users = userService.listUsers();
        assertEquals(List.of(persistedUser1, persistedUser2, persistedUser3), users);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void Should_ThrowNullPointerException_When_ListOfUsersIsNull() {
        assertThrows(NullPointerException.class, () -> userService.saveUsers(null));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveAnyUsersAndThrowNullPointerException_When_SomeUsersAreNull() {
        List<User> usersToSave = new ArrayList<>() {{
            add(userToSave1);
            add(null);
            add(userToSave3);
        }};
        assertThrows(NullPointerException.class, () -> userService.saveUsers(usersToSave));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveAnyUsersAndThrowIllegalArgumentException_When_SomeUsersFirstNameIsNull() {
        List<User> usersToSave = new ArrayList<>() {{
            add(userToSave1);
            add(userToSave2);
            add(userWithoutFirstName);
        }};
        assertThrows(IllegalArgumentException.class, () -> userService.saveUsers(usersToSave));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveAnyUsersAndThrowIllegalArgumentException_When_SomeUsersLastNameIsNull() {
        List<User> usersToSave = new ArrayList<>() {{
            add(userToSave2);
            add(userToSave3);
            add(userWithoutLastName);
        }};
        assertThrows(IllegalArgumentException.class, () -> userService.saveUsers(usersToSave));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveAnyUsersAndThrowIllegalArgumentException_When_SomeUsersGenderIsNull() {
        List<User> usersToSave = new ArrayList<>() {{
            add(userToSave3);
            add(userToSave1);
            add(userWithoutGender);
        }};
        assertThrows(IllegalArgumentException.class, () -> userService.saveUsers(usersToSave));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_UpdateUser_When_UserIsValidAndExistInDatabase() {
//        saveUsersToDatabase();
//        User userForUpdate1 = userToSave1;
//        userForUpdate1.setEmail("alice.smith@example.com");
//        User userForUpdate2 = new User(2L, "Bob", "Johnson", User.Gender.MALE, "bob.johnson@example.com");
//        User userForUpdate3 = new User("Bob", "Johnson", User.Gender.MALE);
//        userForUpdate3.setEmail("bob.johnson@example.com");
//        User user1 = userService.updateUser(userForUpdate1);
//        User user2 = userService.updateUser(userForUpdate2);
//        User user3 = userService.updateUser(userForUpdate3);
//        User user4 = userService.updateUser(new User("Name", "Surname", User.Gender.OTHER));
//        userService.listUsers().forEach(System.out::println);
    }

    @Test
    public void Should_SaveUser_When_UserIsValidAndNotExistInDatabase() {

    }

    @Test
    public void Should_NotUpdateOrSaveUserAndThrowNullPointerException_When_UserIsNull() {
        assertThrows(NullPointerException.class, () -> userService.updateUser(null));
        assertEquals(0L, countUsersInDatabase());
    }

    private void saveUsersToDatabase() {
        entityManager.persist(userToSave1);
        entityManager.persist(userToSave2);
        entityManager.persist(userToSave3);
    }

    private Long countUsersInDatabase() {
        return entityManager.getEntityManager()
                .createQuery("SELECT COUNT(u) FROM User u", Long.class)
                .getSingleResult();
    }
}
