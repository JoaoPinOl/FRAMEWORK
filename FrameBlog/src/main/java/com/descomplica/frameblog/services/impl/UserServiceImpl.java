package com.descomplica.frameblog.services.impl;

import com.descomplica.frameblog.models.User;
import com.descomplica.frameblog.repository.UserRepository;
import com.descomplica.frameblog.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User save(final User user) {
        User existingUser = userRepository.findByUsername(user.getName());
        if(Objects.nonNull(existingUser)){
            throw new RuntimeException("Existing User!");
        }
        String passwordHash = passwordEncoder.encode(user.getPassword());

        User entity =  new User(user.getUserId(), user.getName(), user.getEmail(), user.getPassword(), user.getRole(), user.getUsername());
        User newUser = userRepository.save(entity);
        return new User(newUser.getUserId(), newUser.getName(), newUser.getEmail(), newUser.getPassword(), newUser.getRole(), newUser.getUsername());
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    public User get(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User update(Long id, User user) {
        User userUpdate = userRepository.findById(id).orElse(null);
        if(Objects.nonNull(userUpdate)){
            String passwordHash = passwordEncoder.encode(user.getPassword());
            userUpdate.setName(user.getName());
            userUpdate.setUsername(user.getUsername());
            userUpdate.setEmail(user.getEmail());
            userUpdate.setRole(user.getRole());
            userUpdate.setPassword(user.getPassword());
            return userRepository.save(userUpdate);
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
