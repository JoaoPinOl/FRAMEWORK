# 🔐 FLUXO DE AUTENTICAÇÃO - Visualização Rápida

## Fluxo Completo de Autenticação JWT

```
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENTE (Postman/Browser)                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                    1️⃣ POST /users/save
                    (Criar usuário)
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SERVIDOR SPRING BOOT                       │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ UserController.save()                                   │   │
│  │  └─ Recebe: name, email, password, username, role      │   │
│  │  └─ Valida dados                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ UserServiceImpl.save()                                   │   │
│  │  └─ PasswordEncoder.encode(password)  🔐                │   │
│  │  └─ Cria User entity COM senha criptografada            │   │
│  │  └─ UserRepository.save()                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ PostgreSQL Database (tb_user)                           │   │
│  │ ┌────────────────────────────────────────────────────┐  │   │
│  │ │ user_id │ username │ password           │ role    │  │   │
│  │ ├────────────────────────────────────────────────────┤  │   │
│  │ │ 1       │ joao     │ $2a$10$NdryCompq... │ USER    │  │   │
│  │ └────────────────────────────────────────────────────┘  │   │
│  │          ▲ Senha em BCrypt (criptografada) ✓           │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Entrada criada com sucesso
                              │
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENTE (Postman/Browser)                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                    2️⃣ POST /login
              (username, password) em texto plano
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SERVIDOR SPRING BOOT                       │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ AuthenticationController.login()                        │   │
│  │  ├─ Recebe: username, password                          │   │
│  │  └─ Cria UsernamePasswordAuthenticationToken            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ AuthenticationManager.authenticate()                    │   │
│  │ (Spring Security Core)                                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ AuthenticationServiceImpl.loadUserByUsername()           │   │
│  │  ├─ UserRepository.findByUsername(username)             │   │
│  │  └─ Retorna: User object (implements UserDetails)       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ PasswordEncoder.matches(plainPassword, hashedPassword)  │   │
│  │ (BCryptPasswordEncoder)                                 │   │
│  │                                                          │   │
│  │ matches(                                                │   │
│  │   "MinhaSenh@123",                    ← Login text      │   │
│  │   "$2a$10$NdryCompqZvzrHY0g2tFu..."   ← BD hash         │   │
│  │ )  ✓ TRUE                                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ User.getAuthorities()                                   │   │
│  │  └─ Retorna: [ROLE_USER]  ✓ Com prefixo ROLE_           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ AuthenticationServiceImpl.getToken()                     │   │
│  │  └─ JWT.create()                                        │   │
│  │     ├─ Issuer: "FrameBlog"                              │   │
│  │     ├─ Subject: "joao"                                  │   │
│  │     ├─ ExpiresAt: now + 8 hours                         │   │
│  │     └─ sign(Algorithm.HMAC256("my-secret"))             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Retorna: AuthResponse                                   │   │
│  │ {                                                        │   │
│  │   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ..." │   │
│  │ }                                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Token JWT (válido por 8h)
                              │
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENTE (Postman/Browser)                   │
│  Salva o token:                                                │
│  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ..."                 │
└─────────────────────────────────────────────────────────────────┘
                              │
              3️⃣ GET /users/getAll (com Bearer token)
                              │
              Headers: Authorization: Bearer <token>
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SERVIDOR SPRING BOOT                       │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ SecurityFilter.doFilterInternal()                       │   │
│  │  ├─ extractToken() → "eyJhbGciOiJIUzI1NiIsInR5cCI6I..." │   │
│  │  │  └─ authHeader.startsWith("Bearer ")  ✓              │   │
│  │  │  └─ authHeader.substring(7)  ✓ Extra token          │   │
│  │  └─ validateJwtToken(token)                             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ AuthenticationServiceImpl.validateJwtToken()             │   │
│  │  ├─ JWT.require(Algorithm.HMAC256("my-secret"))         │   │
│  │  ├─ withIssuer("FrameBlog")  ✓ Válida issuer            │   │
│  │  ├─ verify(token)  ✓ Assinatura OK                      │   │
│  │  └─ getSubject() → "joao"                               │   │
│  │  └─ Retorna: username                                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ UserRepository.findByUsername("joao")                   │   │
│  │  └─ Retorna: User object completo                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ SecurityContextHolder.getContext()                      │   │
│  │ .setAuthentication(token)                               │   │
│  │  ├─ username: "joao"                                    │   │
│  │  └─ authorities: [ROLE_USER]  ✓                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ HttpSecurity.authorizeRequests()                        │   │
│  │  ├─ GET /users/getAll hasRole("USER")                   │   │
│  │  ├─ User tem ROLE_USER?  ✓ YES                          │   │
│  │  └─ Acesso PERMITIDO                                    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ UserController.getAll()                                 │   │
│  │  └─ Retorna: List<User>                                 │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 200 OK
                              │ [
                              │   {
                              │     "userId": 1,
                              │     "username": "joao",
                              │     "role": "USER",
                              │     ...
                              │   }
                              │ ]
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENTE (Postman/Browser)                   │
│              Recebe a lista de usuários com sucesso! ✅         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Estados Possíveis de Autenticação

### ✅ AUTENTICADO (Status 200)
```
Request:
  POST /login
  { "username": "joao", "password": "MinhaSenh@123" }

Response:
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ..."
  }
```

---

### ❌ ERRO: Senha Incorreta (Status 403)
```
Request:
  POST /login
  { "username": "joao", "password": "SENHAERRADA" }

Fluxo:
  1. User encontrado ✓
  2. PasswordEncoder.matches() → FALSE ✗
  3. Falha na autenticação

Response:
  403 Forbidden - Senha inválida
```

---

### ❌ ERRO: Usuário Não Existe (Status 403)
```
Request:
  POST /login
  { "username": "usuario_inexistente", "password": "qualquer" }

Fluxo:
  1. UserRepository.findByUsername() → null
  2. UsernameNotFoundException lançada
  3. Autenticação falha

Response:
  403 Forbidden - User not found
```

---

## Comparação: ANTES vs DEPOIS

### ANTES (❌ ERA ERRADO)

```
User login:
  "joao" / "MinhaSenh@123"
  
Step 1: Armazenar no BD
  password = "MinhaSenh@123"  ← TEXTO PLANO! ⚠️
  
Step 2: Fazer login
  PasswordEncoder.matches("MinhaSenh@123", "MinhaSenh@123")
  └─ Comparação plaintext vs plaintext (sempre falha!)
  
Step 3: Autorização
  User.getAuthorities() → ["USER"]  ← SEM prefixo! ⚠️
  hasRole("USER") → procura por "ROLE_USER"
  Não encontra → 403 FORBIDDEN ⚠️
```

---

### DEPOIS (✅ CORRETO AGORA)

```
User login:
  "joao" / "MinhaSenh@123"
  
Step 1: Armazenar no BD
  passwordHash = BCryptPasswordEncoder.encode("MinhaSenh@123")
  password = "$2a$10$NdryCompqZvzrHY0g2tFu..."  ← CRIPTOGRAFADA! ✓
  
Step 2: Fazer login
  PasswordEncoder.matches(
    "MinhaSenh@123",                    ← plaintext
    "$2a$10$NdryCompqZvzrHY0g2tFu..."   ← hash
  ) → TRUE ✓
  
Step 3: Autorização
  User.getAuthorities() → ["ROLE_USER"]  ← COM prefixo! ✓
  hasRole("USER") → procura por "ROLE_USER"
  Encontra → 200 OK ✓
  
Step 4: Gerar Token JWT
  JWT.create()
    .withSubject("joao")
    .withIssuer("FrameBlog")
    .withExpiresAt(now + 8h)
    .sign(Algorithm.HMAC256("my-secret"))
  └─ Token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ..." ✓
```

---

## Timeline de uma Requisição Autenticada

```
2026-05-07 16:30:00.000
  [CLIENT] POST /login
  ├─ REQUEST BODY: {"username":"joao","password":"MinhaSenh@123"}
  └─ HEADERS: Content-Type: application/json

2026-05-07 16:30:00.100
  [SERVER] AuthenticationController.login()
  ├─ Cria UsernamePasswordAuthenticationToken
  └─ Chama authenticationManager.authenticate()

2026-05-07 16:30:00.150
  [SERVER] AuthenticationServiceImpl.loadUserByUsername()
  ├─ SQL: SELECT * FROM tb_user WHERE username = 'joao'
  └─ Retorna User object

2026-05-07 16:30:00.200
  [SERVER] PasswordEncoder.matches()
  ├─ Compara: "MinhaSenh@123" vs "$2a$10$NdryCompq..."
  └─ ✓ MATCH!

2026-05-07 16:30:00.250
  [SERVER] User.getAuthorities()
  ├─ Role: USER
  └─ Retorna: [ROLE_USER]

2026-05-07 16:30:00.300
  [SERVER] AuthenticationServiceImpl.generateToken()
  ├─ JWT.create()
  │  ├─ Issuer: "FrameBlog"
  │  ├─ Subject: "joao"
  │  ├─ IssuedAt: 2026-05-07T16:30:00Z
  │  ├─ ExpiresAt: 2026-05-08T00:30:00Z (8 horas depois)
  │  └─ Sign: HMAC256("my-secret")
  └─ Retorna: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ..."

2026-05-07 16:30:00.350
  [SERVER] AuthResponse
  └─ {"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ..."}

2026-05-07 16:30:00.400
  [CLIENT] Recebe response 200 OK
  └─ Salva token para próximas requisições
```

---

## Mapeamento de Erros

```
ERR 400 - Bad Request
  └─ Body JSON inválido
     └─ Falta campos obrigatórios
     └─ Tipo de dado incorreto

ERR 401 - Unauthorized
  └─ Token inválido
     └─ Token expirado
     └─ Token não foi passado
     └─ Token corrompido

ERR 403 - Forbidden
  └─ Autenticação falhou
     └─ Username errado
     └─ Password errado
     └─ Usuário não encontrado
     └─ User sem rol adequado

ERR 500 - Internal Server Error
  └─ Erro na aplicação
     └─ Conexão BD falhou
     └─ Exception não tratada
```

---

## Resumo Visual das Correções

```
┌─────────────────────────────────────────┐
│         CORREÇÕES IMPLEMENTADAS         │
├─────────────────────────────────────────┤
│                                         │
│ 1️⃣  User.getAuthorities()              │
│     ANTES: ["USER"]  ❌                 │
│     DEPOIS: ["ROLE_USER"]  ✅           │
│                                         │
│ 2️⃣  SecurityFilter.extractToken()      │
│     ANTES: split(" ")[1]  ❌            │
│     DEPOIS: substring(7)  ✅            │
│                                         │
│ 3️⃣  UserServiceImpl.save()              │
│     ANTES: user.getPassword()  ❌       │
│     DEPOIS: passwordHash  ✅            │
│                                         │
│ 4️⃣  UserServiceImpl.update()            │
│     ANTES: user.getPassword()  ❌       │
│     DEPOIS: passwordHash  ✅            │
│                                         │
│ 5️⃣  AuthenticationServiceImpl.load...() │
│     ANTES: return null  ❌              │
│     DEPOIS: throw exception  ✅         │
│                                         │
│ 6️⃣  UserRepository search               │
│     ANTES: user.getName()  ❌           │
│     DEPOIS: user.getUsername()  ✅      │
│                                         │
└─────────────────────────────────────────┘

RESULTADO: 403 → 200 ✅
```

---

**Fluxo de Autenticação Corrigido com Sucesso!** 🎉

