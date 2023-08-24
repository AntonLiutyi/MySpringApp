package com.example.demo.services;

import com.example.demo.models.User;

import java.util.List;

public interface UserService {

    List<User> listUsers();

    void saveUser(User user);

    void deleteUser(Long id);
}
