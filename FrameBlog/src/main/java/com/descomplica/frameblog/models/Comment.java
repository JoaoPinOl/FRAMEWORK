package com.descomplica.frameblog.models;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "Comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long commentId;

    private String content;
    private Date date;

    @ManyToOne
    private Post post;

    @ManyToOne
    private User userV2;


    public Comment() {
    }

    public Comment(final Long commentId, final String content, final Date date, final Post post, final User userV2) {
        this.commentId = commentId;
        this.content = content;
        this.date = date;
        this.post = post;
        this.userV2 = userV2;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public User getUser() {
        return userV2;
    }

    public void setUser(User userV2) {
        this.userV2 = userV2;
    }
}
