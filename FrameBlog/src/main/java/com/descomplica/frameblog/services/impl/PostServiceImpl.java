package com.descomplica.frameblog.services.impl;

import com.descomplica.frameblog.models.Post;
import com.descomplica.frameblog.repository.PostRepository;
import com.descomplica.frameblog.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostRepository postRepository;

    @Override
    public Post save(Post post) {
        return postRepository.save(post);
    }

    @Override
    public List<Post> getAll() {
        return postRepository.findAll();
    }

    @Override
    public Post get(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    @Override
    public Post update(Long id, Post post) {
        Post postUpdate = postRepository.findById(id).orElse(null);
        if(Objects.nonNull(postUpdate)){
            postUpdate.setTitle(post.getTitle());
            postUpdate.setContent(post.getContent());
            postUpdate.setDate(post.getDate());
            postUpdate.setUserId(post.getUserId());
            return postRepository.save(postUpdate);
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        postRepository.deleteById(id);
    }
}
