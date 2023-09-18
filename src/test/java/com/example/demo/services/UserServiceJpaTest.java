package com.example.demo.services;

import com.example.demo.models.User;
import com.example.demo.services.impl.UserServiceJpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.demo.services.util.UserServiceTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@ComponentScan(basePackages = {"com.example.demo.services"})
public class UserServiceJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserServiceJpa userService;

    private User userToSave1;
    private User userToSave2;
    private User userToSave3;

    /**
     * Some save operations may set the user ID.
     * It is necessary to reset the ID back to null to prevent further problems.
     */
    @BeforeEach
    public void resetUsersToSave() {
        userToSave1 = USER_TO_SAVE_1.clone();
        userToSave2 = USER_TO_SAVE_2.clone();
        userToSave3 = USER_TO_SAVE_3.clone();
    }

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
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2, PERSISTED_USER_3), users);
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
        assertEquals(PERSISTED_USER_2, user);
    }

    @Test
    public void Should_ThrowNullPointerException_When_ListOfIdsIsNull() {
        assertThrows(NullPointerException.class, () -> userService.findAllUsersByIds(null));
    }

    @Test
    public void Should_FindOnlyUsersWithMatchingIds_When_IdsAreValid() {
        saveUsersToDatabase();
        List<User> users = userService.findAllUsersByIds(List.of(0L, 3L, 5L, 8L));
        assertEquals(List.of(PERSISTED_USER_3), users);
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
        assertEquals(List.of(PERSISTED_USER_2, PERSISTED_USER_3), users.get());
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
        assertEquals(PERSISTED_USER_1, user.get());
    }

    @Test
    public void Should_NotSaveUserAndThrowNullPointerException_When_UserIsNull() {
        assertThrows(NullPointerException.class, () -> userService.saveUser(null));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveUserAndThrowIllegalArgumentException_When_UserFirstNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userService.saveUser(USER_WITHOUT_FIRST_NAME));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveUserAndThrowIllegalArgumentException_When_UserLastNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userService.saveUser(USER_WITHOUT_LAST_NAME));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveUserAndThrowIllegalArgumentException_When_UserGenderIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userService.saveUser(USER_WITHOUT_GENDER));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_SaveAllUsers_When_UsersAreValid() {
        assertDoesNotThrow(() -> userService.saveUsers(List.of(userToSave1, userToSave2, userToSave3)));
        assertEquals(3L, countUsersInDatabase());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2, PERSISTED_USER_3), users);
    }

    @Test
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
            add(USER_WITHOUT_FIRST_NAME);
        }};
        assertThrows(IllegalArgumentException.class, () -> userService.saveUsers(usersToSave));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveAnyUsersAndThrowIllegalArgumentException_When_SomeUsersLastNameIsNull() {
        List<User> usersToSave = new ArrayList<>() {{
            add(userToSave2);
            add(userToSave3);
            add(USER_WITHOUT_LAST_NAME);
        }};
        assertThrows(IllegalArgumentException.class, () -> userService.saveUsers(usersToSave));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotSaveAnyUsersAndThrowIllegalArgumentException_When_SomeUsersGenderIsNull() {
        List<User> usersToSave = new ArrayList<>() {{
            add(userToSave3);
            add(userToSave1);
            add(USER_WITHOUT_GENDER);
        }};
        assertThrows(IllegalArgumentException.class, () -> userService.saveUsers(usersToSave));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_UpdateUser_When_UserIsValidAndExistInDatabase() {
        User userToUpdate = userService.saveUser(userToSave2);
        assertNotEquals(PERSISTED_USER_1, userToUpdate);
        userToUpdate.setFirstName("Alice");
        userToUpdate.setLastName("Smith");
        userToUpdate.setGender(User.Gender.FEMALE);
        AtomicReference<User> updatedUser = new AtomicReference<>();
        assertDoesNotThrow(() -> updatedUser.set(userService.updateUser(userToUpdate)));
        assertEquals(1L, countUsersInDatabase());
        assertEquals(PERSISTED_USER_1, updatedUser.get());
    }

    @Test
    public void Should_SaveUser_When_UserIsValidAndNotExistInDatabase() {
        AtomicReference<User> user = new AtomicReference<>();
        assertDoesNotThrow(() -> user.set(userService.updateUser(userToSave1)));
        assertEquals(1L, countUsersInDatabase());
        assertEquals(PERSISTED_USER_1, user.get());
    }

    @Test
    public void Should_NotUpdateOrSaveUserAndThrowNullPointerException_When_UserIsNull() {
        assertThrows(NullPointerException.class, () -> userService.updateUser(null));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotUpdateOrSaveUserAndThrowIllegalArgumentException_When_UserFirstNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(USER_WITHOUT_FIRST_NAME));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotUpdateOrSaveUserAndThrowIllegalArgumentException_When_UserLastNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(USER_WITHOUT_LAST_NAME));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_NotUpdateOrSaveUserAndThrowIllegalArgumentException_When_UserGenderIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(USER_WITHOUT_GENDER));
        assertEquals(0L, countUsersInDatabase());
    }

    @Test
    public void Should_DeleteUserById_When_UserExistsInDatabase() {
        saveUsersToDatabase();
        userService.deleteUser(2L);
        assertEquals(2L, countUsersInDatabase());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_3), users);
    }

    @Test
    public void Should_NotDeleteAnyUsers_When_ThereAreNoUserWithProvidedIdInDatabase() {
        saveUsersToDatabase();
        userService.deleteUser(5L);
        assertEquals(3L, countUsersInDatabase());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2, PERSISTED_USER_3), users);
    }

    @Test
    public void Should_NotDeleteAnyUsersAndThrowIllegalArgumentException_When_ProvidedIdIsNull() {
        saveUsersToDatabase();
        assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(null));
        assertEquals(3L, countUsersInDatabase());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2, PERSISTED_USER_3), users);
    }

    @Test
    public void Should_NotDeleteAnyUsersAndThrowNullPointerException_When_ListOfIdsIsNull() {
        saveUsersToDatabase();
        assertThrows(NullPointerException.class, () -> userService.deleteAllUsersByIds(null));
        assertEquals(3L, countUsersInDatabase());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2, PERSISTED_USER_3), users);
    }

    @Test
    public void Should_DeleteOnlyUsersWithMatchingIds_When_ListOfIdsIsProvided() {
        saveUsersToDatabase();
        userService.deleteAllUsersByIds(List.of(0L, 3L, 5L, 8L));
        assertEquals(2L, countUsersInDatabase());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2), users);
    }

    @Test
    public void Should_DeleteOnlyUsersWithMatchingIdsAndNotThrowException_When_ListOfIdsIsProvidedAndSomeIdsAreNull() {
        saveUsersToDatabase();
        List<Long> userIds = new ArrayList<>() {{
            add(0L);
            add(2L);
            add(null);
            add(3L);
            add(5L);
        }};
        Assertions.assertDoesNotThrow(() -> userService.deleteAllUsersByIds(userIds));
        assertEquals(1L, countUsersInDatabase());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1), users);
    }

    @Test
    public void Should_DeleteAllUsers_When_DatabaseIsNotEmpty() {
        saveUsersToDatabase();
        userService.deleteAllUsers();
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
