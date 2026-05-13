# 04 - Autenticação e Autorização

## O que é

**Autenticação** responde a: "Quem você é?" (verificar identidade)  
**Autorização** responde a: "O que você pode fazer?" (verificar permissões)

Metáfora:
- **Autenticação** = seu documento de identidade (CPF, passaporte) — prova quem você é
- **Autorização** = sua carteira de motorista — diz que você pode dirigir, mas não pilotar avião

Sem autenticação, qualquer um acessa sua API. Sem autorização, qualquer usuário faria qualquer coisa (admin deletaria posts de outros, etc).

## Por que existe / Para que serve

Cenários do mundo real:
- Um blog só o **autor** pode editar seu próprio post
- Um **admin** pode deletar qualquer post
- Um **visitante** só pode ler (não criar)
- Uma API precisa saber **quem está chamando** para criar logs, limitar requisições, etc

Segurança é crítica:
- Dados privados não podem ser expostos
- Operações sensíveis precisam ser verificadas
- Múltiplos níveis de acesso (comum, premium, admin)

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── pom.xml                         ← spring-boot-starter-security
├── src/main/java/.../
│   ├── config/                     ← SecurityConfig: configuração global
│   │   └── SecurityConfig.java
│   ├── controllers/                ← @PreAuthorize controla acesso
│   │   └── PostController.java
│   ├── models/                     ← User, Role (papéis)
│   │   └── User.java
│   ├── repository/                 ← UserRepository (busca user)
│   ├── services/                   ← UserDetailService (Spring carrega user)
│   │   └── UserDetailsServiceImpl.java
│   └── request/                    ← LoginRequest, TokenRequest
│       └── AuthRequest.java
├── application.properties           ← JWT secret key, timeout
└── (opcional) jwt/                 ← Utilitários para JWT
    └── JwtTokenProvider.java
```

**Fluxo típico com JWT (JSON Web Token)**:
1. Cliente: `POST /auth/login` com email+senha
2. Server valida, gera **token JWT** (assinado, com expiração)
3. Cliente recebe token, armazena (localStorage, sessão)
4. Cliente: `GET /posts` com header `Authorization: Bearer token`
5. Server valida token, se ok, executa endpoint

## Exemplo prático comentado

**Model de User**:
```java
package com.descomplica.frameblog.models;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;
    
    private String senha; // Sempre criptografada!
    
    // Papéis: ROLE_USER, ROLE_ADMIN, ROLE_MODERADOR
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    private Boolean ativo = true;

    // Implementação de UserDetails (obrigatória no Spring Security)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }

    @Override
    public String getPassword() { return senha; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return ativo; }

    // Getters...
    public Long getId() { return id; }
    public String getEmail() { return email; }
}
```

**Enum de Papéis**:
```java
package com.descomplica.frameblog.enums;

public enum Role {
    ROLE_USER,       // Usuário comum
    ROLE_ADMIN,      // Administrador
    ROLE_MODERADOR   // Moderador de conteúdo
}
```

**Config de Segurança**:
```java
package com.descomplica.frameblog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: criptografa senha com "salt" aleatório, muito seguro
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Sessão stateless (JWT não usa sessão)
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            
            // Autorização por endpoint
            .authorizeHttpRequests()
                // Públicos
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/posts/**").permitAll() // GET é público
                
                // Apenas admin
                .requestMatchers("DELETE", "/posts/**").hasRole("ADMIN")
                
                // Qualquer autenticado
                .requestMatchers("POST", "/posts/**").authenticated()
                
                // Tudo mais: rejeita
                .anyRequest().denyAll()
            .and()
            
            // Desabilita CSRF (não necessário com JWT)
            .csrf().disable()
            
            // Adiciona filtro JWT
            .addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter();
    }
}
```

**Controller de Autenticação**:
```java
package com.descomplica.frameblog.controllers;

import com.descomplica.frameblog.request.LoginRequest;
import com.descomplica.frameblog.response.TokenResponse;
import com.descomplica.frameblog.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        String token = authService.autenticar(request.getEmail(), request.getSenha());
        if (token != null) {
            return ResponseEntity.ok(new TokenResponse(token));
        }
        return ResponseEntity.status(401).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Logout geralmente é feito no cliente (deletar token)
        // No servidor, pode invalidar token em blacklist
        return ResponseEntity.ok().build();
    }
}
```

**Service de Autenticação**:
```java
package com.descomplica.frameblog.services;

import com.descomplica.frameblog.models.User;
import com.descomplica.frameblog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtProvider;

    public String autenticar(String email, String senhaRaw) {
        // Busca usuário no banco
        User user = userRepository.findByEmail(email);
        
        if (user == null) {
            return null; // Usuário não existe
        }

        // Verifica senha: compara senhaRaw com hash armazenado
        if (!passwordEncoder.matches(senhaRaw, user.getPassword())) {
            return null; // Senha incorreta
        }

        // Gera token JWT válido por 1 hora
        return jwtProvider.gerarToken(user, 3600); // 3600 segundos
    }
}
```

**Provider de JWT**:
```java
package com.descomplica.frameblog.jwt;

import com.descomplica.frameblog.models.User;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:minha-chave-muito-secreta-e-longa}")
    private String secretKey;

    @Value("${app.jwt.expiration:3600000}")
    private int expirationMs;

    public String gerarToken(User user, int validadeSegundos) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + validadeSegundos * 1000);

        return Jwts.builder()
                .setSubject(user.getEmail()) // Identificador único
                .setIssuedAt(agora)
                .setExpiration(expiracao)
                .claim("id", user.getId())
                .claim("roles", user.getAuthorities())
                .signWith(SignatureAlgorithm.HS512, secretKey) // Assinatura
                .compact();
    }

    public String extrairEmail(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

**Controller protegido**:
```java
@RestController
@RequestMapping("/posts")
public class PostController {

    @GetMapping
    public ResponseEntity<List<PostResponse>> buscarTodos() {
        // Público: qualquer um pode ler
        return ResponseEntity.ok(posts);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")  // Só autenticados
    public ResponseEntity<PostResponse> criar(@RequestBody CriarPostRequest request) {
        // Criar é para usuários autenticados
        return ResponseEntity.status(201).body(post);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")  // Só admin
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        // Deletar é só para admin
        return ResponseEntity.noContent().build();
    }
}
```

## Conexão com Spring Boot

Spring Security oferece autenticação/autorização:

| Conceito | Descrição |
|---|---|
| **spring-boot-starter-security** | Starter que adiciona segurança |
| **UserDetails** | Interface que Spring carrega para verificar usuário |
| **PasswordEncoder** | Criptografa senhas (BCrypt recomendado) |
| **@PreAuthorize** | Anotação para verificar permitão antes de executar |
| **JWT (JSON Web Token)** | Token assinado que cliente envia em cada requisição |
| **SecurityConfig** | Configuração centralizada de segurança |

No `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.12.3</version>
</dependency>
```

No `application.properties`:
```properties
# Chave secreta para assinar JWT (USE UMA LONGA E COMPLEXA!)
app.jwt.secret=sua-chave-super-secreta-com-pelo-menos-32-caracteres
app.jwt.expiration=3600000  # 1 hora em ms
```

## Resumo em tópicos

- **Autenticação** = Verificar identidade (login com email+senha)
- **Autorização** = Verificar permissão (pode fazer ação X?)
- **JWT** = Token assinado que cliente envia em cada requisição
- **BCrypt** = Algoritmo para criptografar senhas (nunca armazene em texto plano!)
- **@PreAuthorize** = Anotação que bloqueia executar método se usuário não tem role
- **UserDetails** = Interface que Spring Security usa para carregar dados de usuário
- **PasswordEncoder** = Compara senha digitada com hash armazenado

## Dúvidas frequentes (FAQ)

**P: Qual é a diferença entre session-based (tradicional) e JWT?**  
R: **Session**: servidor armazena info do usuário em memória. Cliente recebe sessionId. **JWT**: token autossuficiente, servidor não armazena nada. JWT é melhor para APIs (stateless, escalável).

**P: Onde devo guardar o token JWT no cliente (web/mobile)?**  
R: **Web**: localStorage (mas cuidado com XSS). **Mobile**: gerenciador de segurança do SO. **Nunca** em cookie sem `HttpOnly` flag.

**P: O que acontece se alguém roubar meu JWT?**  
R: Terá acesso até expirar (por isso expirações curtas: 1-24h). Por isso sempre use HTTPS. Para mais segurança, implemente refresh tokens (token de curta vida + refresh token de longa vida).

