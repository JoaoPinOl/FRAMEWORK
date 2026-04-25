package com.descomplica.frameblog.services.impl;

import com.descomplica.frameblog.models.Comment;
import com.descomplica.frameblog.repository.CommentRepository;
import com.descomplica.frameblog.services.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Override
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }
}
