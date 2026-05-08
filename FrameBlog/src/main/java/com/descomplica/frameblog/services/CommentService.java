package com.descomplica.frameblog.services;

import com.descomplica.frameblog.models.Comment;

public interface CommentService {
    Comment send(Comment comment);
}