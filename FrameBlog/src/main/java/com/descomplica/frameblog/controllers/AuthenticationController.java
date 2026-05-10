package com.descomplica.frameblog.controllers;

import com.descomplica.frameblog.request.AuthRequest;
import com.descomplica.frameblog.response.AuthResponse;
import com.descomplica.frameblog.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthenticationController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping(path = "/login")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public AuthResponse login(@RequestBody final AuthRequest auth) {
        try {
            UsernamePasswordAuthenticationToken userAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(auth.getUsername(), auth.getPassword());

            authenticationManager.authenticate(userAuthenticationToken);

            return new AuthResponse(authenticationService.getToken(auth));

        } catch (Exception e) {
            System.out.println(">>> ERRO NO LOGIN: " + e.getClass().getName());
            System.out.println(">>> MENSAGEM: " + e.getMessage());
            throw e;
        }
    }
}

