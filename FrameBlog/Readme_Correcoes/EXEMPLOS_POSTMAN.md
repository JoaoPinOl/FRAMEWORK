# 📋 Exemplos Prontos para Copiar e Colar no Postman

Copie e cole diretamente no Postman! ✅

---

## 1️⃣ CRIAR USUÁRIO COM ROLE USER

```
Requisição:
POST http://localhost:5000/users/save
Content-Type: application/json

{
  "name": "João Silva Desenvolvedor",
  "email": "joao.silva@descomplica.com.br",
  "password": "MinhaSenh@123",
  "username": "joao_silva",
  "role": "USER"
}

Resposta esperada (200 OK):
{
  "userId": 1,
  "name": "João Silva Desenvolvedor",
  "email": "joao.silva@descomplica.com.br",
  "password": "$2a$10$NdryCompqZvzrHY0g2tFuuk3j9qPxZ5V8kE6qL2fZ1V9U5F1mZ1Ym",
  "role": "USER",
  "username": "joao_silva"
}
```

---

## 1️⃣ Alt. CRIAR USUÁRIO COM ROLE ADMIN

```
Requisição:
POST http://localhost:5000/users/save
Content-Type: application/json

{
  "name": "Admin Sistema",
  "email": "admin@descomplica.com.br",
  "password": "@dm1nS3nh@",
  "username": "admin_sistema",
  "role": "ADMIN"
}

Resposta esperada (200 OK):
{
  "userId": 2,
  "name": "Admin Sistema",
  "email": "admin@descomplica.com.br",
  "password": "$2a$10$KsLxK9dFqQQ9V5K2mNoPduJ5L8xV9Z1Y2P3Q4R5S6T7U8V9W0X1Y2",
  "role": "ADMIN",
  "username": "admin_sistema"
}
```

---

## 2️⃣ FAZER LOGIN (Obter Token)

```
Requisição:
POST http://localhost:5000/login
Content-Type: application/json

{
  "username": "joao_silva",
  "password": "MinhaSenh@123"
}

Resposta esperada (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJGcmFtZUJsb2ciLCJzdWIiOiJqb2FvX3NpbHZhIiwiZXhwIjoxNzE0NDI1NjAwfQ.abc123defghijklmnopqrstuvwxyz..."
}

⚠️ COPIE ESTE TOKEN! Ele será usado nos próximos passos.
```

---

## 3️⃣ ACESSAR RECURSO PROTEGIDO - Listar Todos os Usuários

```
Requisição:
GET http://localhost:5000/users/getAll
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJGcmFtZUJsb2ciLCJzdWIiOiJqb2FvX3NpbHZhIiwiZXhwIjoxNzE0NDI1NjAwfQ.abc123defghijklmnopqrstuvwxyz...

Resposta esperada (200 OK):
[
  {
    "userId": 1,
    "name": "João Silva Desenvolvedor",
    "email": "joao.silva@descomplica.com.br",
    "password": "$2a$10$NdryCompqZvzrHY0g2tFu...",
    "role": "USER",
    "username": "joao_silva"
  },
  {
    "userId": 2,
    "name": "Admin Sistema",
    "email": "admin@descomplica.com.br",
    "password": "$2a$10$KsLxK9dFqQQ9V5K2mNoPd...",
    "role": "ADMIN",
    "username": "admin_sistema"
  }
]
```

---

## 4️⃣ ACESSAR RECURSO PROTEGIDO - Obter Usuário Específico

```
Requisição:
GET http://localhost:5000/users/get
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJGcmFtZUJsb2ciLCJzdWIiOiJqb2FvX3NpbHZhIiwiZXhwIjoxNzE0NDI1NjAwfQ.abc123defghijklmnopqrstuvwxyz...

Resposta esperada (200 OK):
{
  "userId": 1,
  "name": "João Silva Desenvolvedor",
  "email": "joao.silva@descomplica.com.br",
  "password": "$2a$10$NdryCompqZvzrHY0g2tFu...",
  "role": "USER",
  "username": "joao_silva"
}
```

---

## 5️⃣ ATUALIZAR USUÁRIO (Requer ADMIN)

```
Requisição:
POST http://localhost:5000/users/update/1
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJGcmFtZUJsb2ciLCJzdWIiOiJhZG1pbl9zaXN0ZW1hIiwiZXhwIjoxNzE0NDI1NjAwfQ.xyz789...

{
  "name": "João Silva Atualizado",
  "email": "joao.novo@descomplica.com.br",
  "password": "NovaSenh@456",
  "username": "joao_silva",
  "role": "ADMIN"
}

Resposta esperada (200 OK):
{
  "userId": 1,
  "name": "João Silva Atualizado",
  "email": "joao.novo@descomplica.com.br",
  "password": "$2a$10$NvB8N5K7jZaB2c9D5e4F6...",
  "role": "ADMIN",
  "username": "joao_silva"
}

⚠️ NOTA: O usuário que faz a requisição precisa ter role ADMIN!
```

---

## 6️⃣ DELETAR USUÁRIO (Requer ADMIN)

```
Requisição:
DELETE http://localhost:5000/users/delete/1
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJGcmFtZUJsb2ciLCJzdWIiOiJhZG1pbl9zaXN0ZW1hIiwiZXhwIjoxNzE0NDI1NjAwfQ.xyz789...

Resposta esperada (204 No Content)
(Sem corpo de resposta)

⚠️ NOTA: O usuário que faz a requisição precisa ter role ADMIN!
```

---

## 🔑 TOKENS DE EXEMPLO (Para Copiar)

### Token de ADMIN (válido por 8 horas)
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJGcmFtZUJsb2ciLCJzdWIiOiJhZG1pbl9zaXN0ZW1hIiwiZXhwIjoxNzE0NDI1NjAwfQ.ADMIN_TOKEN_EXAMPLE_12345
```

**Usar em todos os requests com:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJGcmFtZUJsb2ciLCJzdWIiOiJhZG1pbl9zaXN0ZW1hIiwiZXhwIjoxNzE0NDI1NjAwfQ.ADMIN_TOKEN_EXAMPLE_12345
```

---

## 🔴 CENÁRIOS DE ERRO

### Erro 1: Senha incorreta
```
Requisição:
POST http://localhost:5000/login
Content-Type: application/json

{
  "username": "joao_silva",
  "password": "SENHAERRADA"
}

Resposta esperada (403 Forbidden):
Erro de autenticação - senha inválida
```

### Erro 2: Usuário não existe
```
Requisição:
POST http://localhost:5000/login
Content-Type: application/json

{
  "username": "usuario_inexistente",
  "password": "qualquer_senha"
}

Resposta esperada (403 Forbidden):
User not found with username: usuario_inexistente
```

### Erro 3: Token inválido
```
Requisição:
GET http://localhost:5000/users/getAll
Authorization: Bearer TOKEN_INVALIDO_FAKE_12345

Resposta esperada (401 Unauthorized):
Erro - Token inválido
```

### Erro 4: Sem token (acesso negado)
```
Requisição:
GET http://localhost:5000/users/getAll
(SEM header de Authorization)

Resposta esperada (403 Forbidden):
Acesso negado - token não fornecido
```

### Erro 5: Sem permissão (ADMIN required)
```
Requisição:
POST http://localhost:5000/users/update/1
Content-Type: application/json
Authorization: Bearer TOKEN_DE_USER_NORMAL

{
  "name": "Novo Nome",
  "email": "novo@test.com",
  "password": "nova_senha",
  "username": "joao_silva",
  "role": "USER"
}

Resposta esperada (403 Forbidden):
Acesso negado - requer role ADMIN
```

---

## 📋 CHECKLIST DE TESTES

Copie este checklist e marque conforme progride:

```
□ Criar usuário com role USER
  - Status 200?
  - Senha está em BCrypt ($2a$10$)?
  
□ Fazer login com USER
  - Status 200?
  - Token retornado começa com "eyJ"?
  
□ Acessar /users/getAll com token USER
  - Status 200?
  - Retorna lista de usuários?
  
□ Criar usuário com role ADMIN
  - Status 200?
  
□ Fazer login com ADMIN
  - Status 200?
  - Token diferente do anterior?
  
□ Atualizar usuário (com token ADMIN)
  - Status 200?
  
□ Deletar usuário (com token ADMIN)
  - Status 204?
  
□ Tentar login com USER em endpoint ADMIN-only
  - Status 403?
  
□ Usar token expirado
  - Status 401?
```

---

## 💡 DICAS FINAIS

1. **Salve os tokens em um arquivo de texto** enquanto testa
2. **Use as mesmas credenciais** para tornar mais fácil lembrar
3. **Crie várias coleções** no Postman para diferentes cenários
4. **Teste os erros** também para garantir que o sistema está seguro
5. **Monitore os logs** da aplicação enquanto testa

---

## 🎯 Fluxo Recomendado de Testes

**Dia 1:**
- ✅ Criar usuário USER
- ✅ Fazer login
- ✅ Acessar recurso protegido

**Dia 2:**
- ✅ Criar usuário ADMIN
- ✅ Fazer login como ADMIN
- ✅ Atualizar usuário
- ✅ Deletar usuário

**Dia 3:**
- ✅ Testar cenários de erro
- ✅ Testar com tokens expirados
- ✅ Testar com roles incorretas

---

Tudo pronto para testar! Boa sorte! 🚀

