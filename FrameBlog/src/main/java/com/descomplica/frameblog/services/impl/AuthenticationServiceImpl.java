package com.descomplica.frameblog.services.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.descomplica.frameblog.models.User;
import com.descomplica.frameblog.repository.UserRepository;
import com.descomplica.frameblog.request.AuthRequest;
import com.descomplica.frameblog.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        return userRepository.findByUsername(login);
    }

    @Override
    public String getToken(AuthRequest auth){
        User userV2 = userRepository.findByUsername(auth.getUsername());
        return generateToken(userV2);
    }

    public  String generateToken(User userV2) {
        try {
            Algorithm algorithm = Algorithm.HMAC256("my-secret");

            return JWT.create()
                    .withIssuer("FrameBlog")
                    .withSubject(userV2.getUsername())
                    .withExpiresAt(getExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Fail to generate token" +exception.getMessage());
        }
    }

    public String validateJwtToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256("my-secret");

            return JWT.require(algorithm)
                    .withIssuer("FrameBlog")
                    .build()
                    .verify(token)
                    .getSubject();

        } catch (JWTVerificationException exception) {
            return "";
        }
    }
    private Instant getExpirationDate() {
        return LocalDateTime.now()
                .plusHours(8)
                .toInstant(ZoneOffset.of("-03:00"));
    }
}
