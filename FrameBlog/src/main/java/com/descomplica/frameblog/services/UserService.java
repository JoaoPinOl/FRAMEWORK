package com.descomplica.frameblog.services;

import com.descomplica.frameblog.models.User;

import java.util.List;

public interface UserService {
    User save(User user);

    List<User> getAll();

    User get(Long id);

    User update(Long id, User user);

    void delete(Long id);
}

