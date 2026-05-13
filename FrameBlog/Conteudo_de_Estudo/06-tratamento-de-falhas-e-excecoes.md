# 06 - Tratamento de Falhas e Exceções

## O que é

**Tratamento de exceções** é o plano B da sua aplicação: "E se algo der errado?" Em vez de o programa simplesmente falhar e retornar uma mensagem críptica, você **captura o erro, trata e retorna uma resposta amigável**.

Exceções comuns:
- Usuário não encontrado (404)
- Email duplicado (400)
- Acesso não autorizado (403)
- Erro interno do servidor (500)
- Banco de dados desconectado (500)

## Por que existe / Para que serve

Sem tratamento de exceções:
```
GET /posts/999
→ NullPointerException!
→ Cliente recebe HTML com stack trace (feio e inseguro)
```

Com tratamento:
```
GET /posts/999
→ Captura erro
→ Cliente recebe JSON: { "erro": "Post não encontrado", "status": 404 }
```

Benefícios:
- **Mensagens legíveis**: cliente entende o que deu errado
- **Logs**: registra o erro para debug
- **Segurança**: não expõe stack trace (hacker pode explorar)
- **Respostacoerente**: toda API segue o mesmo formato

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── src/main/java/.../
│   ├── exceptions/                     ← Suas exceções customizadas
│   │   ├── PostNaoEncontradoException.java
│   │   ├── UsuarioDuplicadoException.java
│   │   └── AcessoNegadoException.java
│   ├── controllers/                    ← try-catch em endpoints
│   │   └── PostController.java
│   ├── services/                       ← Lança exceções quando dado inválido
│   │   └── PostService.java
│   ├── config/
│   │   └── GlobalExceptionHandler.java ← Intercepta TODAS as exceções
│   └── response/
│       └── ErroResponse.java           ← Formato padrão de erro
```

**Fluxo**:
1. Cliente → Endpoint
2. Controller chama Service
3. Service valida dados
4. Se inválido, **lança exceção** customizada
5. **GlobalExceptionHandler** captura
6. Retorna JSON com erro formatado

## Exemplo prático comentado

**Exceções customizadas**:
```java
package com.descomplica.frameblog.exceptions;

// Exceção quando post não existe
public class PostNaoEncontradoException extends RuntimeException {
    public PostNaoEncontradoException(Long id) {
        super("Post com ID " + id + " não encontrado");
    }
}

// Exceção quando email já existe (duplicado)
public class UsuarioDuplicadoException extends RuntimeException {
    public UsuarioDuplicadoException(String email) {
        super("Email " + email + " já está cadastrado");
    }
}

// Exceção quando usuário sem permissão
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}

// Exceção genérica de validação
public class DadosInvalidosException extends RuntimeException {
    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
```

**Response padronizado de erro**:
```java
package com.descomplica.frameblog.response;

import java.time.LocalDateTime;

public class ErroResponse {
    private int status;
    private String mensagem;
    private LocalDateTime timestamp;
    private String caminho;  // Qual endpoint falhou

    public ErroResponse(int status, String mensagem, String caminho) {
        this.status = status;
        this.mensagem = mensagem;
        this.timestamp = LocalDateTime.now();
        this.caminho = caminho;
    }

    public int getStatus() { return status; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getCaminho() { return caminho; }
}
```

**Handler global de exceções** (intercepta todas):
```java
package com.descomplica.frameblog.config;

import com.descomplica.frameblog.exceptions.*;
import com.descomplica.frameblog.response.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;

// @RestControllerAdvice: aplica tratamento de erro a TODOS os controllers
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Captura PostNaoEncontradoException
    @ExceptionHandler(PostNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handlePostNaoEncontrado(
            PostNaoEncontradoException ex,
            HttpServletRequest request) {
        
        ErroResponse erro = new ErroResponse(
            404,
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    // Captura UsuarioDuplicadoException
    @ExceptionHandler(UsuarioDuplicadoException.class)
    public ResponseEntity<ErroResponse> handleUsuarioDuplicado(
            UsuarioDuplicadoException ex,
            HttpServletRequest request) {
        
        ErroResponse erro = new ErroResponse(
            400,  // Bad Request
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // Captura AcessoNegadoException
    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> handleAcessoNegado(
            AcessoNegadoException ex,
            HttpServletRequest request) {
        
        ErroResponse erro = new ErroResponse(
            403,  // Forbidden
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    // Captura DadosInvalidosException
    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroResponse> handleDadosInvalidos(
            DadosInvalidosException ex,
            HttpServletRequest request) {
        
        ErroResponse erro = new ErroResponse(
            400,
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // Captura QUALQUER exceção não prevista
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleErroGenerico(
            Exception ex,
            HttpServletRequest request) {
        
        // Log para debug (nunca exponha ao cliente!)
        ex.printStackTrace();

        ErroResponse erro = new ErroResponse(
            500,
            "Erro interno do servidor. Contate suporte.",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
```

**Service lançando exceções**:
```java
package com.descomplica.frameblog.services;

import com.descomplica.frameblog.exceptions.*;
import com.descomplica.frameblog.models.Post;
import com.descomplica.frameblog.models.User;
import com.descomplica.frameblog.repository.PostRepository;
import com.descomplica.frameblog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    public Post criarPost(Post post, Long userId) {
        // Validação 1: título vazio?
        if (post.getTitulo() == null || post.getTitulo().trim().isEmpty()) {
            throw new DadosInvalidosException("Título não pode ser vazio");
        }

        // Validação 2: usuário existe?
        User autor = userRepository.findById(userId)
                .orElseThrow(() -> new PostNaoEncontradoException(userId));

        post.setAutor(autor);
        return postRepository.save(post);
    }

    public Post buscarPorId(Long id) {
        // Se não encontrar, lança exceção customizada
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNaoEncontradoException(id));
    }

    public void deletarPost(Long id, Long usuarioId) {
        Post post = buscarPorId(id);  // Lança PostNaoEncontradoException se não existir

        // Verificação: pode deletar?
        if (!post.getAutor().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Você não pode deletar post de outro usuário");
        }

        postRepository.delete(post);
    }
}
```

**Controller usando exceções**:
```java
package com.descomplica.frameblog.controllers;

import com.descomplica.frameblog.models.Post;
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

    @PostMapping
    public ResponseEntity<Post> criar(@RequestBody Post post) {
        // Não precisa de try-catch! GlobalExceptionHandler captura
        Post criado = service.criarPost(post, 1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> buscar(@PathVariable Long id) {
        // Se falhar, GlobalExceptionHandler retorna 404 + JSON
        Post post = service.buscarPorId(id);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        // Se sem permissão, GlobalExceptionHandler retorna 403 + JSON
        service.deletarPost(id, 1L);
        return ResponseEntity.noContent().build();
    }
}
```

## Conexão com Spring Boot

Spring Boot oferece tratamento de exceções via:

| Conceito | Funciona |
|---|---|
| **@RestControllerAdvice** | Centraliza tratamento de erro para todos os controllers |
| **@ExceptionHandler** | Mapeia qual exceção cai em qual método |
| **ResponseEntity** | Controla status HTTP e corpo da resposta |
| **@Valid** | Validação automática de DTOs |
| **BindingResult** | Coleta erros de validação |

No `pom.xml`:
```xml
<!-- Validação automática -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## Resumo em tópicos

- **Exceção** = Evento inesperado que interrompe fluxo normal
- **try-catch** = Captura exceção e executa código alternativo
- **throw** = Lança exceção para quem chamou tratar
- **@RestControllerAdvice** = Intercepta ALL exceções e retorna erro formatado
- **@ExceptionHandler** = Mapeia qual exceção vai para qual método
- **HTTP Status** = 400 (Bad Request), 404 (Not Found), 500 (Server Error)
- **Nunca exponha stack trace** = Registra em logs, retorna mensagem amigável ao cliente

## Dúvidas frequentes (FAQ)

**P: Devo lançar exceção ou retornar ResponseEntity com erro?**  
R: **Lançar exceção** é melhor (código mais limpo). GlobalExceptionHandler captura e formata. Reserve ResponseEntity para casos especiais onde o erro é esperado.

**P: Qual status HTTP retornar?**  
R: **400** = dados inválidos (culpa cliente). **404** = não encontrado. **401** = não autenticado. **403** = autenticado mas sem permissão. **500** = erro no servidor (nunca culpa cliente).

**P: Preciso de uma classe Exception customizada para cada erro?**  
R: Não obrigatório, mas recomendado. Facilita tratamento específico. Se muitos erros, agrupe em categorias (BadRequestException, NotFoundException, etc).

