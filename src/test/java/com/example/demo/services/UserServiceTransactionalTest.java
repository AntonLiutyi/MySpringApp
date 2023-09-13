package com.example.demo.services;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.impl.UserServiceTransactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static com.example.demo.services.util.UserServiceTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
public class UserServiceTransactionalTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserServiceTransactional userService;

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
    public void Should_FindNoUserIds_When_DatabaseIsEmpty() {
        List<Long> userIds = userService.getAllUserIds();
        assertEquals(0, userIds.size());
    }

    @Test
    public void Should_FindAllUserIds_When_DatabaseIsNotEmpty() {
        saveUsersToDatabase();
        List<Long> userIds = userService.getAllUserIds();
        assertEquals(List.of(1L, 2L, 3L), userIds);
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
    public void Should_SaveAllUsers_When_UsersAreValid() {
        userService.saveUsers(List.of(USER_TO_SAVE_1, USER_TO_SAVE_2, USER_TO_SAVE_3));
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2, PERSISTED_USER_3), users);
    }

    @Test
    public void Should_ThrowInvalidDataAccessApiUsageException_When_ListOfUsersIsNull() {
        assertThrows(InvalidDataAccessApiUsageException.class, () -> userService.saveUsers(null));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_NotSaveAnyUsersAndThrowInvalidDataAccessApiUsageException_When_SomeUsersAreNull() {
        List<User> usersToSave = new ArrayList<>() {{
            add(USER_TO_SAVE_1);
            add(null);
            add(USER_TO_SAVE_3);
        }};
        assertThrows(InvalidDataAccessApiUsageException.class, () -> userService.saveUsers(usersToSave));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_NotSaveAnyUsersAndThrowDataIntegrityViolationException_When_SomeUsersFirstNameIsNull() {
        List<User> usersToSave = new ArrayList<>() {{
            add(USER_TO_SAVE_1);
            add(USER_TO_SAVE_2);
            add(USER_WITHOUT_FIRST_NAME);
        }};
        assertThrows(DataIntegrityViolationException.class, () -> userService.saveUsers(usersToSave));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_NotSaveAnyUsersAndThrowDataIntegrityViolationException_When_SomeUsersLastNameIsNull() {
        List<User> usersToSave = new ArrayList<>() {{
            add(USER_TO_SAVE_2);
            add(USER_TO_SAVE_3);
            add(USER_WITHOUT_LAST_NAME);
        }};
        assertThrows(DataIntegrityViolationException.class, () -> userService.saveUsers(usersToSave));
        assertEquals(0L, userRepository.count());
    }

    @Test
    public void Should_NotSaveAnyUsersAndThrowDataIntegrityViolationException_When_SomeUsersGenderIsNull() {
        List<User> usersToSave = new ArrayList<>() {{
            add(USER_TO_SAVE_3);
            add(USER_TO_SAVE_1);
            add(USER_WITHOUT_GENDER);
        }};
        assertThrows(DataIntegrityViolationException.class, () -> userService.saveUsers(usersToSave));
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
    public void Should_NotDeleteAnyUsersAndThrowInvalidDataAccessApiUsageException_When_ListOfIdsIsNull() {
        saveUsersToDatabase();
        assertThrows(InvalidDataAccessApiUsageException.class, () -> userService.deleteAllUsersByIds(null));
        assertEquals(3L, userRepository.count());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1, PERSISTED_USER_2, PERSISTED_USER_3), users);
    }

    @Test
    public void Should_DeleteOnlyUsersWithMatchingIds_When_ListOfIdsIsProvided() {
        saveUsersToDatabase();
        userService.deleteAllUsersByIds(List.of(0L, 3L, 5L, 8L));
        assertEquals(2L, userRepository.count());
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
        assertDoesNotThrow(() -> userService.deleteAllUsersByIds(userIds));
        assertEquals(1L, userRepository.count());
        List<User> users = userService.listUsers();
        assertEquals(List.of(PERSISTED_USER_1), users);
    }

    @Test
    public void Should_DeleteAllUsers_When_DatabaseIsNotEmpty() {
        saveUsersToDatabase();
        userService.deleteAllUsers();
        assertEquals(0L, userRepository.count());
    }

    private void saveUsersToDatabase() {
        userRepository.save(USER_TO_SAVE_1);
        userRepository.save(USER_TO_SAVE_2);
        userRepository.save(USER_TO_SAVE_3);
    }
}
