package com.descomplica.frameblog.services.v2;

import com.descomplica.frameblog.models.V2.UserV2;

import java.util.List;

public interface UserServiceV2 {

    UserV2 save(final UserV2 userV2);

    List<UserV2> getAll();

    UserV2 get(final Long id);

    UserV2 update(Long id, UserV2 userV2);

    void delete(Long id);
}
