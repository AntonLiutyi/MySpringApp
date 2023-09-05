package com.example.demo.services.impl;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceTransactional implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public List<Long> getAllUserIds() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void saveUsers(Collection<User> users) {
        userRepository.saveAll(users);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void deleteAllUsersByIds(Collection<Long> ids) {
        userRepository.deleteAllByIdIn(ids);
    }

    public void deleteAllUsers() {
        userRepository.deleteAll();
    }
}
