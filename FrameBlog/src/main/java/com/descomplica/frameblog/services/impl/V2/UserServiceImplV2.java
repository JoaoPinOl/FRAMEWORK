package com.descomplica.frameblog.services.impl.V2;

import com.descomplica.frameblog.models.V2.UserV2;
import com.descomplica.frameblog.repository.V2.UserRepositoryV2;
import com.descomplica.frameblog.services.v2.UserServiceV2;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class UserServiceImplV2 implements UserServiceV2 {

    @Autowired
    private UserRepositoryV2 userRepositoryV2;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserV2 save(final UserV2 userV2) {
        UserV2 existingUserV2 = userRepositoryV2.findByUsername(userV2.getUsername());

        if(Objects.nonNull(existingUserV2)){
            throw new RuntimeException("Existing user!");
        }

        String passwordHash = passwordEncoder.encode(userV2.getPassword());

        UserV2 entity = new UserV2(userV2.getUserId(), userV2.getName(), userV2.getEmail(), userV2.getPassword(), userV2.getRole(), userV2.getUsername());
        UserV2 newUserV2 = userRepositoryV2.save(entity);
        return new UserV2(newUserV2.getUserId(), newUserV2.getName(), newUserV2.getEmail(), newUserV2.getPassword(), newUserV2.getRole(), newUserV2.getUsername());
    }

    @Override
    public List<UserV2> getAll() {
        return userRepositoryV2.findAll();
    }

    @Override
    public UserV2 get(final Long id) {
        return userRepositoryV2.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    @Override
    public UserV2 update(Long id, UserV2 userV2) {
        UserV2 userV2Update = userRepositoryV2.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found!"));
        if(Objects.nonNull(userV2Update)){
            String passwordHash = passwordEncoder.encode(userV2.getPassword());
            userV2Update.setName(userV2.getName());
            userV2Update.setUsername(userV2.getUsername());
            userV2Update.setEmail(userV2.getEmail());
            userV2Update.setRole(userV2.getRole());
            userV2Update.setPassword(userV2.getPassword());
            return userRepositoryV2.save(userV2Update);
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        UserV2 userV2Delete = userRepositoryV2.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found!"));
        userRepositoryV2.deleteById(id);
    }
}
