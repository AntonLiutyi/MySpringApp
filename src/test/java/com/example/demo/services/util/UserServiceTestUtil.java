package com.example.demo.services.util;

import com.example.demo.models.User;

public final class UserServiceTestUtil {

    private UserServiceTestUtil() {}

    public static final User USER_TO_SAVE_1 = new User("Alice", "Smith", User.Gender.FEMALE);
    public static final User USER_TO_SAVE_2 = new User("Bob", "Johnson", User.Gender.MALE);
    public static final User USER_TO_SAVE_3 = new User("Terry", "Jerry", User.Gender.ATTACK_HELICOPTER);
    public static final User PERSISTED_USER_1 = new User(1L, "Alice", "Smith", User.Gender.FEMALE, "alice.smith@example.com");
    public static final User PERSISTED_USER_2 = new User(2L, "Bob", "Johnson", User.Gender.MALE, "bob.johnson@example.com");
    public static final User PERSISTED_USER_3 = new User(3L, "Terry", "Jerry", User.Gender.ATTACK_HELICOPTER, "terry.jerry@example.com");
    public static final User USER_WITHOUT_FIRST_NAME = new User(null, "Giggles", User.Gender.OTHER);
    public static final User USER_WITHOUT_LAST_NAME = new User("Chuckles", null, User.Gender.OTHER);
    public static final User USER_WITHOUT_GENDER = new User("Riddle", "Riddle", null);
}
