package com.example.demo.services;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;

final class UserServiceTestUtil {

    private UserServiceTestUtil() {}

    static final User USER_TO_SAVE_1 = new User("Alice", "Smith", User.Gender.FEMALE);
    static final User USER_TO_SAVE_2 = new User("Bob", "Johnson", User.Gender.MALE);
    static final User USER_TO_SAVE_3 = new User("Terry", "Jerry", User.Gender.ATTACK_HELICOPTER);
    static final User PERSISTED_USER_1 = new User(1L, "Alice", "Smith", User.Gender.FEMALE, "alice.smith@example.com");
    static final User PERSISTED_USER_2 = new User(2L, "Bob", "Johnson", User.Gender.MALE, "bob.johnson@example.com");
    static final User PERSISTED_USER_3 = new User(3L, "Terry", "Jerry", User.Gender.ATTACK_HELICOPTER, "terry.jerry@example.com");
    static final User USER_WITHOUT_FIRST_NAME = new User(null, "Giggles", User.Gender.OTHER);
    static final User USER_WITHOUT_LAST_NAME = new User("Chuckles", null, User.Gender.OTHER);
    static final User USER_WITHOUT_GENDER = new User("Riddle", "Riddle", null);

    static void saveAllUsersToDatabase(UserRepository userRepository) {
        userRepository.save(USER_TO_SAVE_1);
        userRepository.save(USER_TO_SAVE_2);
        userRepository.save(USER_TO_SAVE_3);
    }
}
