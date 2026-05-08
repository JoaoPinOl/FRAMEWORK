# 📚 Guia Completo: Testando o Login no Postman

## Pré-requisitos

1. **Postman instalado** (ou usar versão online em postman.com)
2. **Aplicação FrameBlog rodando** na porta 5000
3. **PostgreSQL com o banco `frameblog` criado**

---

## 📖 Guia Passo a Passo

### PASSO 1️⃣ : Criar um Novo Usuário

Antes de fazer login, você precisa criar um usuário no sistema. O importante é que agora a senha será **criptografada automaticamente**.

#### Configuração da Requisição:

1. **Abra o Postman**
2. **Crie uma nova aba (New Request)**
3. **Configure da seguinte forma:**

```
TIPO:              POST
URL:               http://localhost:5000/users/save
Headers:           Content-Type: application/json
```

#### Body (JSON):

```json
{
  "name": "João Desenvolvedor",
  "email": "joao@descomplica.com.br",
  "password": "minhaSenha123",
  "username": "joao_dev",
  "role": "USER"
}
```

#### Resposta Esperada (Status 200):

```json
{
  "userId": 1,
  "name": "João Desenvolvedor",
  "email": "joao@descomplica.com.br",
  "password": "$2a$10$...",  // ← Senha CRIPTOGRAFADA
  "role": "USER",
  "username": "joao_dev"
}
```

⚠️ **IMPORTANTE:** A senha no banco de dados agora está em **BCrypt format** com o prefixo `$2a$10$`

---

### PASSO 2️⃣ : Fazer Login para Obter o Token

Agora que você tem um usuário criado, pode fazer login e receber um token JWT.

#### Configuração da Requisição:

```
TIPO:              POST
URL:               http://localhost:5000/login
Headers:           Content-Type: application/json
```

#### Body (JSON):

```json
{
  "username": "joao_dev",
  "password": "minhaSenha123"
}
```

#### Resposta Esperada (Status 200):

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJGcmFtZUJsb2ciLCJzdWIiOiJqb2FvX2RldiIsImV4cCI6MTcxNDQyNTYwMH0.abc123..."
}
```

#### ✅ Se receber 200 com um token válido:
- ✓ Token JWT gerado com sucesso
- ✓ Autenticação funcionando corretamente
- ✓ Senha foi validada corretamente

#### ❌ Se receber 403 (Forbidden):
- Verifique se a senha está correta
- Verifique se o usuário existe no banco
- Consulte se é usuário inserido manualmente (leia a seção de correção)

#### ❌ Se receber erro em JSON com detalhes:
- Verifique se o username existe
- Verifique se a senha está correta

---

### PASSO 3️⃣ : Usar o Token para Acessar Recursos Protegidos

Agora você pode usar o token para acessar endpoints protegidos.

#### Configuração da Requisição:

```
TIPO:              GET
URL:               http://localhost:5000/users/getAll
Headers:           
  - Content-Type: application/json
  - Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ...
```

⚠️ **IMPORTANTE:** Copie o token completo da resposta do login!

#### No Postman, você pode fazer assim:

1. Vá para a aba **Headers**
2. Crie um novo header:
   - **Key:** `Authorization`
   - **Value:** `Bearer sua_token_aqui`

#### Resposta Esperada (Status 200):

```json
[
  {
    "userId": 1,
    "name": "João Desenvolvedor",
    "email": "joao@descomplica.com.br",
    "password": "$2a$10$...",
    "role": "USER",
    "username": "joao_dev"
  }
]
```

#### ✅ Se receber 200 com a lista de usuários:
- ✓ Token foi validado com sucesso
- ✓ Autorização funcionando (você tem a role USER)
- ✓ Autenticação JWT está 100% operacional

#### ❌ Se receber 401 (Unauthorized):
- Token inválido ou expirou
- Faça login novamente
- Copie o novo token

#### ❌ Se receber 403 (Forbidden):
- Usuário não tem permissão (role)
- Você não tem a role `USER`
- Crie um novo usuário com role correta

---

## 🔑 Exemplo Completo da Conversação

### Request 1 - Criar Usuário (ADMIN)

```
POST /users/save
Content-Type: application/json

{
  "name": "Admin Sistema",
  "email": "admin@descomplica.com.br",
  "password": "admin123",
  "username": "admin_user",
  "role": "ADMIN"
}
```

**Response:** 200 OK

```json
{
  "userId": 1,
  "name": "Admin Sistema",
  "email": "admin@descomplica.com.br",
  "password": "$2a$10$...",
  "role": "ADMIN",
  "username": "admin_user"
}
```

---

### Request 2 - Fazer Login com Admin

```
POST /login
Content-Type: application/json

{
  "username": "admin_user",
  "password": "admin123"
}
```

**Response:** 200 OK

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJGcmFtZUJsb2ciLCJzdWIiOiJhZG1pbl91c2VyIiwiZXhwIjoxNzE0NDI1OTAwfQ.xyz123..."
}
```

---

### Request 3 - Acessar Recurso Protegido com Token

```
GET /users/getAll
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJGcmFtZUJsb2ciLCJzdWIiOiJhZG1pbl91c2VyIiwiZXhwIjoxNzE0NDI1OTAwfQ.xyz123...
```

**Response:** 200 OK

```json
[
  {
    "userId": 1,
    "name": "Admin Sistema",
    "email": "admin@descomplica.com.br",
    "password": "$2a$10$...",
    "role": "ADMIN",
    "username": "admin_user"
  }
]
```

---

##⚙️ Dicas Úteis do Postman

### 1. **Salvar Requisições em Coleções**

1. Após criar as requisições, clique em **Save** (Ctrl+S)
2. Crie uma coleção chamada "FrameBlog Auth"
3. Salve todas as requisições

### 2. **Usar Variáveis Globais para o Token**

Isso facilita reutilizar o token em várias requisições:

**Após receber o token do login:**

1. Vá para a aba **Tests** da requisição de login
2. Adicione este script:

```javascript
var jsonData = pm.response.json();
pm.globals.set("token", jsonData.token);
```

3. Agora em outras requisições, use:
```
Authorization: Bearer {{token}}
```

### 3. **Criar Um Fluxo de Teste Automatizado**

1. Crie uma coleção com as 3 requisições
2. Va para **Collection** > **Edit** > **Pre-request Script** global
3. Você pode configurar scripts para executar:
   - Request 1: Criar usuário
   - Request 2: Fazer login (salvar token)
   - Request 3: Usar recurso protegido

### 4. **Modo Runner (Execução em Sequência)**

1. Clique em **Runner**
2. Selecione sua coleção "FrameBlog Auth"
3. Clique em **Run**
4. Veja as requisições sendo executadas em sequência

---

## 🚨 Troubleshooting - Erros Comuns

### ❌ Erro 403 - Forbidden

**Causa 1:** Roles não estão no formato correto
- Verifique se na resposta do login o usuário tem o role correto
- Deve ser `"role": "USER"` ou `"role": "ADMIN"`, não `"role": "ROLE_USER"`

**Causa 2:** Token inválido ou expirado
- Gere um novo token fazendo login novamente
- Tokens expiram em 8 horas

**Solução:**
```
1. Faça login novamente
2. Copie o novo token
3. Use o novo token nas requisições
```

---

### ❌ Erro 401 - Unauthorized

**Causa:** Token não foi passado corretamente no header

**Verificação:**
- Certifique-se de que o header está assim:
  ```
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
  ```
- ⚠️ Não esqueça o `Bearer ` (com espaço)
- ⚠️ Copie o token inteiro, não adicione nada extra

---

### ❌ Erro 400 - Bad Request

**Causa:** Body JSON está malformado

**Verificação:**
- Verifique se o JSON é válido no JSONLint
- Verifique se todos os campos obrigatórios estão presentes:
  - Para criar usuário: `name`, `email`, `password`, `username`, `role`
  - Para login: `username`, `password`

---

### ❌ Erro 500 - Internal Server Error

**Causa:** Erro no servidor

**Solução:**
1. Verifique os logs da aplicação
2. Verifique se o PostgreSQL está conectado
3. Reinicie a aplicação

```
Logs esperados ao fazer login bem-sucedido:
[INFO] ... POST request to "/login"
[INFO] Hibernate: select u1_0.user_id, u1_0.email, u1_0.name, u1_0.password, u1_0.role, u1_0.username from tb_user u1_0 where u1_0.username = ?
[INFO] User authenticated successfully
```

---

## 📝 Checklist de Sucesso

Após seguir todos os passos, você deve ter:

- ✅ Usuário criado com sucesso
- ✅ Senha criptografada em BCrypt
- ✅ Login retorna um token JWT válido
- ✅ Token começa com `eyJ...` (base64)
- ✅ Você consegue acessar endpoints protegidos
- ✅ Status 200 ao fazer requisições autenticadas
- ✅ Todos os logs aparecem corretamente na aplicação

---

## 🎓 Entendendo o Fluxo

```
┌─────────────────┐
│   Usuario       │
└────────┬────────┘
         │
         ├─ POST /users/save ─────────────────────────────┐
         │  (cria usuário)                                 │
         │                                                 ├─→ POST /login
         ├─ POST /login ──────────────────────────────────┤
         │  (gera token JWT)                               │
         │  (válido por 8 horas)                           │
         │                                                 ├─→ GET /users/getAll
         ├─ GET /users/getAll (com token) ────────────────┤
         │  (acessa recurso protegido)                     │
         │                                                 │
         └─────────────────────────────────────────────────┘
              Fluxo de Autenticação JWT
```

---

Pronto! Agora você tem um guia completo para testar o login no Postman! 🚀

