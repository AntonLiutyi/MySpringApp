package com.example.demo.services.impl;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserServiceImplAdvanced implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public List<User> listUsers(boolean ascending) {
        if (ascending) {
            return listUsers();
        } else {
            return userRepository.findAllByOrderByIdDesc();
        }
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void saveUsers(User... users) {
        userRepository.saveAll(Arrays.asList(users));
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void deleteUsers() {
        userRepository.deleteAll();
    }
}
