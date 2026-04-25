package com.descomplica.frameblog.services;

import com.descomplica.frameblog.request.AuthRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface AuthenticationService extends UserDetailsService {



    String getToken(AuthRequest auth);

    String validateJwtToken(String token);
}
