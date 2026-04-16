package com.descomplica.frameblog.services;

import com.descomplica.frameblog.models.Tag;

import java.util.List;

public interface TagService {
    List<Tag> save(List<Tag> tags);

    List<Tag> getAll();

    Tag save(Tag tags);

    Tag get(Long id);

    Tag update(Long id, Tag tag);

    void delete(Long id);
}
