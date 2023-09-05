package com.example.demo.services.impl;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Cacheable(value = "users", key = "'all'", condition = "@cacheConfig.isCacheEnabled()")
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Override
    @CachePut(value = "users", key = "#user.id", condition = "@cacheConfig.isCacheEnabled()")
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    @CacheEvict(value = "users", key = "#id", condition = "@cacheConfig.isCacheEnabled()")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @CacheEvict(value = "users", key = "'all'", condition = "@cacheConfig.isCacheEnabled()")
    public void reloadUsers() {
        // do nothing
    }
}
