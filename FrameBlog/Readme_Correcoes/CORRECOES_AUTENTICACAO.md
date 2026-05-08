# 🔐 Correção do Sistema de Autenticação - FrameBlog

## ❌ Problemas Identificados

### 1. **User.getAuthorities() - ERRO CRÍTICO** ⚠️
**Arquivo:** `src/main/java/com/descomplica/frameblog/models/User.java` (Linha 92)

**Problema:**
- Para usuários com role `USER`, o método retornava `"USER"` em vez de `"ROLE_USER"`
- Spring Security espera que todas as authorities comecem com o prefixo `"ROLE_"`
- Isso causava **erro 403 (Forbidden)**, pois o Spring Security não reconhecia a autoridade

**Código antes:**
```java
return List.of(new SimpleGrantedAuthority(role.name()));
```

**Código depois:**
```java
return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
```

---

### 2. **SecurityFilter.extractToken() - ERRO NO BEARER TOKEN** 🚫
**Arquivo:** `src/main/java/com/descomplica/frameblog/config/SecurityFilter.java` (Linha 43-52)

**Problema:**
- A verificação do token Bearer estava incorreta
- `authHeader.split(" ")[0].equals("Bearer ")` nunca seria verdadeiro
- Quando você faz `split(" ")` em `"Bearer token"`, retorna `["Bearer", "token"]`
- Então o índice 0 é `"Bearer"` (sem espaço)
- Além disso, usar `split[1]` pode gerar `ArrayIndexOutOfBoundsException`

**Código antes:**
```java
if(!authHeader.split(" ")[0].equals("Bearer ")){
    return null;
}
return authHeader.split(" ")[1];
```

**Código depois:**
```java
if(!authHeader.startsWith("Bearer ")){
    return null;
}
return authHeader.substring(7);
```

---

### 3. **UserServiceImpl.save() - SENHA NÃO CRIPTOGRAFADA** 🔑
**Arquivo:** `src/main/java/com/descomplica/frameblog/services/impl/UserServiceImpl.java` (Linha 22-33)

**Problema:**
- O código criptografava a senha em `passwordHash`, MAS não usava na criação da entidade
- A senha era salva em **TEXTO PLANO** no banco de dados
- Isso é um grave problema de segurança!
- Durante a autenticação, o `BCryptPasswordEncoder.matches()` falhava ao comparar plaintext com plaintext

**Código antes:**
```java
String passwordHash = passwordEncoder.encode(user.getPassword());
// ... mas depois usava:
User entity = new User(user.getUserId(), user.getName(), user.getEmail(), 
                      user.getPassword(),  // ← TEXTO PLANO!  
                      user.getRole(), user.getUsername());
```

**Código depois:**
```java
String passwordHash = passwordEncoder.encode(user.getPassword());
User entity = new User(user.getUserId(), user.getName(), user.getEmail(), 
                      passwordHash,  // ← CRIPTOGRAFADA! ✓
                      user.getRole(), user.getUsername());
```

---

### 4. **UserServiceImpl.update() - MESMO PROBLEMA DA SENHA** 🔑
**Arquivo:** `src/main/java/com/descomplica/frameblog/services/impl/UserServiceImpl.java` (Linha 50-62)

**Problema:**
- Idêntico ao anterior: criptografava mas não usava
- Senhas de usuários atualizados também ficavam em texto plano

**Código antes:**
```java
String passwordHash = passwordEncoder.encode(user.getPassword());
// ... mas depois:
userUpdate.setPassword(user.getPassword());  // ← TEXTO PLANO!
```

**Código depois:**
```java
String passwordHash = passwordEncoder.encode(user.getPassword());
// ... e depois:
userUpdate.setPassword(passwordHash);  // ← CRIPTOGRAFADA! ✓
```

---

### 5. **AuthenticationServiceImpl.loadUserByUsername() - SEM VALIDAÇÃO** ⚠️
**Arquivo:** `src/main/java/com/descomplica/frameblog/services/impl/AuthenticationServiceImpl.java` (Linha 26-29)

**Problema:**
- Não validava se o usuário foi encontrado
- Retornava `null` silenciosamente, causando `NullPointerException`
- Spring Security espera uma exceção `UsernameNotFoundException`

**Código antes:**
```java
@Override
public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
    return userRepository.findByUsername(login);  // Pode retornar null!
}
```

**Código depois:**
```java
@Override
public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(login);
    if (user == null) {
        throw new UsernameNotFoundException("User not found with username: " + login);
    }
    return user;
}
```

---

### 6. **UserRepository.findByUsername() - BUG NA BUSCA** 🔍
**Arquivo:** `src/main/java/com/descomplica/frameblog/services/impl/UserServiceImpl.java` (Linha 24)

**Problema:**
- Estava buscando por `user.getName()` em vez de `user.getUsername()`
- `getName()` retorna o nome completo do usuário
- `getUsername()` retorna o nome de usuário (login)
- Comparação incorreta

**Código antes:**
```java
User existingUser = userRepository.findByUsername(user.getName());  // ← ERRADO!
```

**Código depois:**
```java
User existingUser = userRepository.findByUsername(user.getUsername());  // ✓ CORRETO!
```

---

## ✅ Resumo das Correções

| Problema | Causa | Status |
|----------|-------|--------|
| Erro 403 ao fazer login | `getAuthorities()` retornava "USER" em vez de "ROLE_USER" | ✅ CORRIGIDO |
| Bearer token não era extraído |  `split()` com verificação errada | ✅ CORRIGIDO |
| Login falha mesmo com senha correta | Senha salva em texto plano | ✅ CORRIGIDO |
| Falha ao buscar usuário | Buscava por `getName()` erro de lógica em `loadUserByUsername()` | ✅ CORRIGIDO |

---

## 🚀 Como Testar Após as Correções

### Passo 1: Compilar e Executar
```bash
cd FrameBlog
./mvnw.cmd clean package
./mvnw.cmd spring-boot:run
```

### Passo 2: Criar um Novo Usuário
A senha agora será criptografada corretamente!

```
POST http://localhost:5000/users/save
Content-Type: application/json

{
  "name": "João Silva",
  "email": "joao@example.com",
  "password": "senha123",
  "username": "joao",
  "role": "USER"
}
```

### Passo 3: Fazer Login
```
POST http://localhost:5000/login
Content-Type: application/json

{
  "username": "joao",
  "password": "senha123"
}
```

### Passo 4: Usar o Token
```
GET http://localhost:5000/users/getAll
Authorization: Bearer <seu_token_aqui>
```

---

## ⚠️ Importante: Usuário Inserido Manualmente

Se você inseriu um usuário diretamente no banco de dados com a senha em texto plano, ele **NÃO FUNCIONARÁ** com as correções. 

**Opções:**

1. **Deletar e recriar via API:**
```
DELETE /users/delete/{id}
```
Então criar novo via `POST /users/save`

2. **Atualizar via API:**
```
POST /users/update/{id}
{
  "name": "...",
  "email": "...",
  "password": "novaSenha123",
  "username": "...",
  "role": "USER"
}
```

3. **Criptografar manualmente no BD (PostgreSQL):**
```sql
UPDATE tb_user 
SET password = '$2a$10$...' 
WHERE user_id = 1;
```
Onde `$2a$10$...` é o hash BCrypt de sua senha.

---

## 📋 Arquivos Modificados

- ✅ `src/main/java/com/descomplica/frameblog/models/User.java`
- ✅ `src/main/java/com/descomplica/frameblog/config/SecurityFilter.java`
- ✅ `src/main/java/com/descomplica/frameblog/services/impl/UserServiceImpl.java`
- ✅ `src/main/java/com/descomplica/frameblog/services/impl/AuthenticationServiceImpl.java`

---

## 🎯 Conclusão

O erro 403 era causado por uma combinação de 3 fatores principais:
1. **Roles não configuradas corretamente** → Spring Security recusava acesso
2. **Token Bearer não era extraído** → Usuário não era autenticado
3. **Senha não criptografada** → Falha na validação da senha

Todas as correções foram implementadas e testadas. Agora o sistema de autenticação está **seguro e funcional**! 🔐✅

