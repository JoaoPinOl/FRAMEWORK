package com.descomplica.frameblog.config;

import com.descomplica.frameblog.models.User;
import com.descomplica.frameblog.repository.UserRepository;
import com.descomplica.frameblog.services.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println(">>> FILTRO EXECUTANDO: " + request.getMethod() + " " + request.getRequestURI());

        String token = extractToken(request);
        System.out.println(">>> TOKEN EXTRAIDO: " + token);

        if(token != null){
            try {
                String username = authenticationService.validateJwtToken(token);
                System.out.println(">>> USERNAME DO TOKEN: " + username);

                if(username != null && !username.isEmpty()) {
                    User user = userRepository.findByUsername(username);
                    System.out.println(">>> USUARIO ENCONTRADO: " + user);
                    var authenticationToken = new UsernamePasswordAuthenticationToken(
                            username, null, user.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (Exception e) {
                System.out.println(">>> ERRO NO FILTRO: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");

        if(authHeader == null){
            return null;
        }
        if(!authHeader.startsWith("Bearer ")){
            return null;
        }
        return authHeader.substring(7);
    }

}
