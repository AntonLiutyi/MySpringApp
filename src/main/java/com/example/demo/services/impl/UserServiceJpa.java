package com.example.demo.services.impl;

import com.example.demo.models.User;
import com.example.demo.services.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@Transactional
public class UserServiceJpa implements UserService {

    @PersistenceContext
    private EntityManager entityManager;

    private final Class<User> entityClass = User.class;

    @Override
    public List<User> listUsers() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = criteriaBuilder.createQuery(entityClass);
        Root<User> root = query.from(entityClass);
        query.select(root);
        return entityManager.createQuery(query).getResultList();
    }

    public User findUser(Long id) {
        return entityManager.find(entityClass, id);
    }

    public List<User> findAllUsersByIds(Collection<Long> ids) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = criteriaBuilder.createQuery(entityClass);
        Root<User> root = query.from(entityClass);
        Predicate predicate = root.get("id").in(ids);
        query.where(predicate);
        return entityManager.createQuery(query).getResultList();
    }

    public List<Long> getAllUserIds() {
        TypedQuery<Long> query = entityManager.createQuery("SELECT u.id FROM User u", Long.class);
        return query.getResultList();
    }

    @Override
    public User saveUser(User user) {
        validateUser(user);
        User newUser = user.clone();
        entityManager.persist(newUser);
        return newUser;
    }

    public void saveUsers(Collection<User> users) {
        validateUsers(users);
        for (User user : users) {
            entityManager.persist(user.clone());
        }
    }

    public User updateUser(User user) {
        validateUser(user);
        return entityManager.merge(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = findUser(id);
        if (user != null) {
            entityManager.remove(user);
        }
    }

    public void deleteAllUsersByIds(Collection<Long> ids) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaDelete<User> query = criteriaBuilder.createCriteriaDelete(entityClass);
        Root<User> root = query.from(entityClass);
        Predicate predicate = root.get("id").in(ids);
        query.where(predicate);
        entityManager.createQuery(query).executeUpdate();
    }

    public void deleteAllUsers() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaDelete<User> query = criteriaBuilder.createCriteriaDelete(entityClass);
        query.from(entityClass);
        entityManager.createQuery(query).executeUpdate();
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new NullPointerException("User cannot be null.");
        }
        if (user.getFirstName() == null) {
            throw new IllegalArgumentException("User's first name cannot be null.");
        }
        if (user.getLastName() == null) {
            throw new IllegalArgumentException("User's last name cannot be null.");
        }
        if (user.getGender() == null) {
            throw new IllegalArgumentException("User's gender cannot be null.");
        }
    }

    private void validateUsers(Collection<User> users) {
        for (User user : users) {
            validateUser(user);
        }
    }
}
