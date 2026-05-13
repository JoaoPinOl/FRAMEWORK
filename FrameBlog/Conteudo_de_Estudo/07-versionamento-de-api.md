# 07 - Versionamento de API

## O que é

**Versionamento de API** é a estratégia de manter múltiplas "versões" da sua API funcionando simultaneamente. Imagine que você tem `GET /posts` e quer mudar a estrutura da resposta: alguns clientes antigos quebrariam. Versioning deixa ambas funcionarem.

Estratégias comuns:
- **URL**: `/v1/posts` vs `/v2/posts`
- **Header**: `Accept: application/vnd.api+json;version=2`
- **Query Parameter**: `/posts?version=2`

## Por que existe / Para que serve

Cenário real:
- Você tem 10.000 aplicações clientes usando sua API (app mobile, website, integrações)
- Quer fazer uma mudança grande (remover campo, estrutura diferente)
- **Não pode quebrar** os clientes existentes

Sem versionamento:
```
V1: GET /posts → { id, titulo, conteudo, autor }
    (milhares de clientes dependem disso)

Mudança: { id, titulo, conteudo, autor, categoria } ← novo campo
Problema: Clientes antigos não sabem o que fazer com "categoria"
```

Com versionamento:
```
V1: GET /v1/posts → { id, titulo, conteudo, autor }
    (clientes antigos continuam usando)

V2: GET /v2/posts → { id, titulo, conteudo, autor, categoria }
    (clientes novos usam versão nova)
```

Benefícios:
- **Zero quebra de compatibilidade**
- **Evolução gradual**: novos clientes usam v2, antigos migram quando prontos
- **Suporte**: pode descontinuar v1 após período de transição

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── src/main/java/.../
│   ├── controllers/
│   │   ├── v1/
│   │   │   └── PostControllerV1.java      ← @RequestMapping("/v1/posts")
│   │   ├── v2/
│   │   │   └── PostControllerV2.java      ← @RequestMapping("/v2/posts")
│   │   └── PostController.java            ← versão padrão/mais nova
│   ├── response/
│   │   ├── PostResponseV1.java
│   │   └── PostResponseV2.java
│   └── services/
│       └── PostService.java               ← serviço compartilhado
└── application.properties                 ← versão default
```

Estrutura: Controllers diferentes para cada versão, reutilizando Services.

## Exemplo prático comentado

**Versão 1 - Response simples**:
```java
package com.descomplica.frameblog.response;

public class PostResponseV1 {
    private Long id;
    private String titulo;
    private String conteudo;
    private String autor;

    public PostResponseV1(Post post) {
        this.id = post.getId();
        this.titulo = post.getTitulo();
        this.conteudo = post.getConteudo();
        this.autor = post.getAutor().getNome();
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getConteudo() { return conteudo; }
    public String getAutor() { return autor; }
}
```

**Versão 2 - Response com mais campos**:
```java
package com.descomplica.frameblog.response;

import java.time.LocalDateTime;

public class PostResponseV2 {
    private Long id;
    private String titulo;
    private String conteudo;
    private String autor;
    private LocalDateTime criadoEm;          // ← NOVO em V2
    private Integer comentarios;             // ← NOVO em V2
    private String categoria;                // ← NOVO em V2

    public PostResponseV2(Post post) {
        this.id = post.getId();
        this.titulo = post.getTitulo();
        this.conteudo = post.getConteudo();
        this.autor = post.getAutor().getNome();
        this.criadoEm = post.getCriadoEm();
        this.comentarios = post.getComentarios().size();
        this.categoria = post.getCategoria().getNome();
    }

    // Getters...
}
```

**Controller V1**:
```java
package com.descomplica.frameblog.controllers.v1;

import com.descomplica.frameblog.response.PostResponseV1;
import com.descomplica.frameblog.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/posts")  // ← Versão 1
public class PostControllerV1 {

    @Autowired
    private PostService service;

    @GetMapping
    public ResponseEntity<List<PostResponseV1>> buscarTodos() {
        return ResponseEntity.ok(
            service.buscarTodos().stream()
                .map(PostResponseV1::new)
                .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseV1> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(post -> ResponseEntity.ok(new PostResponseV1(post)))
                .orElse(ResponseEntity.notFound().build());
    }
}
```

**Controller V2**:
```java
package com.descomplica.frameblog.controllers.v2;

import com.descomplica.frameblog.response.PostResponseV2;
import com.descomplica.frameblog.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v2/posts")  // ← Versão 2
public class PostControllerV2 {

    @Autowired
    private PostService service;

    @GetMapping
    public ResponseEntity<List<PostResponseV2>> buscarTodos() {
        return ResponseEntity.ok(
            service.buscarTodos().stream()
                .map(PostResponseV2::new)
                .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseV2> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(post -> ResponseEntity.ok(new PostResponseV2(post)))
                .orElse(ResponseEntity.notFound().build());
    }

    // V2 pode ter novos endpoints
    @GetMapping("/top")
    public ResponseEntity<List<PostResponseV2>> buscarTopPosts() {
        return ResponseEntity.ok(
            service.buscarTodosOrdenado().stream()
                .limit(10)
                .map(PostResponseV2::new)
                .toList()
        );
    }
}
```

**Como cliente consumiria**:
```
Antigo (V1): GET /v1/posts
Resposta:
[
  { "id": 1, "titulo": "Bem-vindo", "conteudo": "...", "autor": "João" }
]

Novo (V2): GET /v2/posts
Resposta:
[
  {
    "id": 1,
    "titulo": "Bem-vindo",
    "conteudo": "...",
    "autor": "João",
    "criadoEm": "2024-05-13T10:30:00",    ← NOVO
    "comentarios": 5,                     ← NOVO
    "categoria": "Tutorial"               ← NOVO
  }
]
```

## Conexão com Spring Boot

Spring Boot **não versionamento automático**, você implementa via:

| Estratégia | Como |
|---|---|
| **URL Path** | `/v1/posts`, `/v2/posts` (recomendado) |
| **Header Accept** | `@RequestMapping(produces = "application/vnd.api+json;version=2")` |
| **Query Parameter** | `/posts?version=2` (menos comum) |

URL path é mais comum porque:
- Mais fácil cachear (CDN entende)
- URLs distintas (SEO / logging)
- Visível em documentação da API

No pom.xml (opcional - para documentar versões):
```xml
<!-- Springdoc OpenAPI (Swagger) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.0</version>
</dependency>
```

No `application.properties`:
```properties
# Informações da API (aparece em /v3/api-docs)
springdoc.api-title=FrameBlog API
springdoc.api-version=2.0
springdoc.swagger-ui.operations-sorter=method
```

## Resumo em tópicos

- **API Versioning** = Suportar múltiplas versões simultaneamente
- **URL Path** = `/v1/posts`, `/v2/posts` (estratégia mais comum)
- **Compatibilidade** = Nunca quebra clientes antigos quando faz mudanças
- **Controllers distintos** = Cada versão seu próprio controller
- **Services compartilhados** = Lógica reutilizada entre versões
- **Response DTOs** = Estrutura diferente para cada versão
- **Período de deprecation** = V1 é suportada por X tempo, depois removida

## Dúvidas frequentes (FAQ)

**P: Por quanto tempo devo manter uma versão antiga?**  
R: Depende. Recomendado 6 meses a 2 anos. Comunique a data de encerramento com antecedência. Use headers `Deprecation: true` para avisar clientes.

**P: Preciso duplicar TODO o código em v2?**  
R: Não! Reutilize Services, Repository, Models. Só mude Controllers e Response DTOs. Code reuse máximo.

**P: URL path é melhor ou Header Accept?**  
R: **URL path** é mais simples e cacheavél. Reserve **Header Accept** para tipos de conteúdo (JSON vs XML), não para versões.


