# 03 - Criação de uma API com Java

## O que é

Uma **API (Application Programming Interface)** é um "cardápio de opções" que sua aplicação oferece para o mundo exterior. Imagine um restaurante: você não entrava na cozinha; você lia o cardápio e pedia. A API funciona assim: define **endpoints** (rotas) que recebem dados e retornam respostas.

Em Spring Boot, você cria endpoints usando:
- **@RestController**: marca uma classe que vai responder requisições HTTP
- **@GetMapping**, **@PostMapping**, etc.: indicam qual é a operação (buscar, criar, atualizar, deletar)
- **JSON**: formato dos dados trocados (mais comum nos dias de hoje)

## Por que existe / Para que serve

Antes das APIs:
- Aplicações Web retornavam HTML (para navegador ver)
- Aplicações Desktop tinham seu próprio código para chamadas
- Integração era um caos (cada sistema usava um formato)

APIs modernas:
- **Qualquer cliente** (web, mobile, outro servidor) usa a mesma interface
- **Separação clara**: Backend processa dados, Frontend só exibe
- **Reutilização**: Se tem uma API bem feita, vários clientes aproveitam
- **Escalabilidade**: API roda em um servidor, múltiplos clientes acessam

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── pom.xml                         ← spring-boot-starter-web
├── src/main/java/.../
│   ├── controllers/                ← @RestController: endpoint aqui!
│   │   └── PostController.java
│   ├── request/                    ← Dados que recebemos (JSON → Java)
│   │   └── CriarPostRequest.java
│   ├── response/                   ← Dados que retornamos (Java → JSON)
│   │   └── PostResponse.java
│   ├── models/                     ← Entidades (@Entity, mapeadas ao banco)
│   │   └── Post.java
│   ├── services/                   ← Regra de negócio (validação, transformação)
│   │   └── PostService.java
│   ├── repository/                 ← Acesso ao banco de dados
│   │   └── PostRepository.java
│   └── deserialize/                ← Conversão customizada (JSON → objs)
└── application.properties           ← Configuração: porta, banco, etc
```

**Fluxo de uma requisição:**
1. Cliente: `POST /posts` + JSON `{ "titulo": "Meu post", "conteudo": "..." }`
2. **Controller** (PostController): recebe e valida
3. **Service** (PostService): aplica regras, pode chamar banco ou outras APIs
4. **Repository** (PostRepository): salva no banco
5. **Response**: retorna `{ "id": 1, "titulo": "Meu post", "status": "sucesso" }`

## Exemplo prático comentado

**Model (entidade do banco)**:
```java
package com.descomplica.frameblog.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Esta classe é uma "tabela" no banco de dados
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String conteudo;
    private String autor;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    private Integer visualizacoes = 0;

    // Getters e setters...
    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    // ... restante omitido para clareza
}
```

**Request DTO (o que recebe na requisição)**:
```java
package com.descomplica.frameblog.request;

// DTO = Data Transfer Object
// Representa o JSON que o cliente envia
public class CriarPostRequest {
    private String titulo;
    private String conteudo;
    private String autor;

    // Getters/Setters
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    // ... etc
}
```

**Response DTO (o que retorna de resposta)**:
```java
package com.descomplica.frameblog.response;

import java.time.LocalDateTime;

// Representa o JSON que a API retorna
public class PostResponse {
    private Long id;
    private String titulo;
    private String conteudo;
    private String autor;
    private LocalDateTime criadoEm;
    private Integer visualizacoes;

    public PostResponse(Post post) {
        this.id = post.getId();
        this.titulo = post.getTitulo();
        this.conteudo = post.getConteudo();
        this.autor = post.getAutor();
        this.criadoEm = post.getCriadoEm();
        this.visualizacoes = post.getVisualizacoes();
    }
    // Getters...
}
```

**Repository (acesso ao banco)**:
```java
package com.descomplica.frameblog.repository;

import com.descomplica.frameblog.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// JpaRepository já oferece save(), findAll(), delete(), etc
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // Métodos adicionais customizados
    List<Post> findByAutor(String autor);
}
```

**Service (lógica de negócio)**:
```java
package com.descomplica.frameblog.services;

import com.descomplica.frameblog.models.Post;
import com.descomplica.frameblog.repository.PostRepository;
import com.descomplica.frameblog.request.CriarPostRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    @Autowired
    private PostRepository repository;

    // Criar post: valida, transforma Request em Model, salva
    public Post criarPost(CriarPostRequest request) {
        // Validação: não pode estar vazio
        if (request.getTitulo() == null || request.getTitulo().isEmpty()) {
            throw new IllegalArgumentException("Título não pode ser vazio");
        }

        // Cria o model (Post) a partir do request (CriarPostRequest)
        Post post = new Post();
        post.setTitulo(request.getTitulo());
        post.setConteudo(request.getConteudo());
        post.setAutor(request.getAutor());

        // Salva no banco via repository
        return repository.save(post);
    }

    // Buscar todos: repository faz a query
    public List<Post> buscarTodos() {
        return repository.findAll();
    }

    // Buscar por ID: retorna Optional (pode não existir)
    public Optional<Post> buscarPorId(Long id) {
        return repository.findById(id);
    }
}
```

**Controller (endpoints da API)**:
```java
package com.descomplica.frameblog.controllers;

import com.descomplica.frameblog.models.Post;
import com.descomplica.frameblog.request.CriarPostRequest;
import com.descomplica.frameblog.response.PostResponse;
import com.descomplica.frameblog.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService service;

    // GET /posts → retorna todos
    @GetMapping
    public ResponseEntity<List<PostResponse>> buscarTodos() {
        List<Post> posts = service.buscarTodos();
        List<PostResponse> responses = posts.stream()
                .map(PostResponse::new) // Converte Post → PostResponse
                .toList();
        return ResponseEntity.ok(responses);
    }

    // GET /posts/:id → retorna um específico
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(post -> ResponseEntity.ok(new PostResponse(post)))
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /posts → cria novo
    @PostMapping
    public ResponseEntity<PostResponse> criar(@RequestBody CriarPostRequest request) {
        try {
            Post post = service.criarPost(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new PostResponse(post));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /posts/:id → atualiza
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> atualizar(
            @PathVariable Long id,
            @RequestBody CriarPostRequest request) {
        // Lógica de atualizar...
        return ResponseEntity.ok(new PostResponse(updated));
    }

    // DELETE /posts/:id → deleta
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Conexão com Spring Boot

Spring Boot facilita criação de APIs com:

| Feature | O que faz |
|---|---|
| **@RestController** | Marca classe que retorna JSON/dados, não HTML |
| **@RequestMapping** | Define caminho base dos endpoints |
| **@GetMapping, @PostMapping** | Mapeia método HTTP → método Java |
| **@PathVariable** | Extrai valor da URL (`/posts/{id}` → `id`) |
| **@RequestBody** | Converte JSON da requisição → objeto Java |
| **ResponseEntity** | Controla status HTTP (200, 201, 404, etc) |
| **spring-boot-starter-web** | Inclui Spring MVC, Tomcat, Jackson (JSON) |

No `application.properties`:
```properties
# Porta da API
server.port=8080

# Formato de data (para serializar LocalDateTime em JSON)
spring.jackson.serialization.write-dates-as-timestamps=false

# Validações automáticas
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

## Resumo em tópicos

- **API REST** = Interface que define operações que sua aplicação oferece
- **@RestController** = Marca classe que responde com dados (geralmente JSON)
- **@GetMapping, @PostMapping, @PutMapping, @DeleteMapping** = HTTP GET/POST/PUT/DELETE
- **Request DTO** = Classe que representa dados que vêm DO cliente
- **Response DTO** = Classe que representa dados que VOÃ PARA o cliente
- **Controllers** → **Services** → **Repositories** = Divisão clara de responsabilidades
- **@PathVariable** = Valor da URL (ex: `/posts/123`); **@RequestBody** = Corpo da requisição

## Dúvidas frequentes (FAQ)

**P: Por que usar DTOs (Request/Response) em vez de enviar a entidade diretamente?**  
R: DTOs desacoplam a API da estrutura do banco. Se mudar a tabela, a API não quebra. Além disso, controla o que é exposto: talvez não queira retornar senha de usuário em Response.

**P: Qual é a diferença entre @RestController e @Controller?**  
R: `@Controller` retorna HTML (view tradicional). `@RestController` retorna dados (JSON/XML) diretamente. Use `@RestController` para APIs.

**P: ResponseEntity é sempre necessário?**  
R: Não, é opcional. O Spring converte automaticamente. Mas `ResponseEntity` te dá controle fino: status HTTP, headers customizados. Use quando precisar disso.

