package com.descomplica.frameblog.services;

import com.descomplica.frameblog.models.Post;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface PostService {
    Post save(Post post);

    List<Post> getAll();

    Post get(Long id);

    Post update(Long id, Post post);

    void delete(Long id);
}
