package com.descomplica.frameblog.services.impl;

import com.descomplica.frameblog.models.Tag;
import com.descomplica.frameblog.repository.TagRepository;
import com.descomplica.frameblog.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagRepository tagRepository;

    @Override
    public List<Tag> save(List<Tag> tags) {
        return tagRepository.saveAll(tags);
    }

    @Override
    public List<Tag> getAll() {
        return tagRepository.findAll();
    }

    @Override
    public Tag save(Tag tag) {
        return tagRepository.save(tag);
    }

    @Override
    public Tag get(Long id) {
        return tagRepository.findById(id).orElse(null);
    }

    @Override
    public Tag update(Long id, Tag tag) {
        Tag tagUpdate = tagRepository.findById(id).orElse(null);
        if(Objects.nonNull(tagUpdate)){
            tagUpdate.setName(tag.getName());
            return tagRepository.save(tagUpdate);
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        tagRepository.deleteById(id);
    }
}
