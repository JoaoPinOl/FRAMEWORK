# 15 - Boas Práticas de Mercado

## O que é

**Boas práticas de mercado** são técnicas, padrões e princípios que profissionais de tecnologia usam em produção. Não é sobre inventar a roda, é sobre reutilizar o aprendizado da comunidade.

Inclui:
- Padrões de código (design patterns)
- Segurança
- Performance
- Observabilidade (logs, métricas)
- Testes
- Documentação
- CI/CD (automação)

## Por que existe / Para que serve

Código "que funciona" é diferente de código "pronto para produção":

```java
// Funciona, mas não é bom
public List<?> buscar() {
    return new ArrayList();
}

// Bom para produção
@GetMapping
@Cacheable("posts")
public ResponseEntity<List<PostResponse>> buscarTodos() {
    return ResponseEntity.ok(service.buscarTodos());
}
```

Diferenças:
- **Segurança**: expõe apenas o necessário
- **Performance**: cacheia resultado
- **Observabilidade**: endpoint documentado
- **Manutenibilidade**: claro e testável

Benefícios:
- Menos bugs em produção
- Código entendível para novos devs
- Evolução mais fácil
- Menos surpresas na madrugada

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── src/main/java/.../
│   ├── controllers/         ← Responsabilidade única
│   ├── services/            ← Lógica, não banco/HTTP
│   ├── repository/          ← Só dados
│   ├── exceptions/          ← Centralizaêrros
│   ├── validators/          ← Validações
│   ├── mappers/             ← Conversões DTO ↔ Entity
│   └── config/              ← Configuração da app
├── src/test/java/           ← TESTES (50%+ cobertura)
├── pom.xml                  ← Sem versões hardcode
├── .gitignore               ← Nunca commit credenciais
├── Dockerfile               ← Containerização
└── docker-compose.yml       ← Ambiente local
```

Princípios:
- **DRY (Don't Repeat Yourself)**: evite duplicação
- **SOLID**: 5 princípios para design
- **Clean Code**: código legível
- **Defensive Programming**: trate erros
- **Security First**: pense em segurança desde início

## Exemplo prático comentado

**1. Estrutura organizada (separação de responsabilidades)**:

```java
// ❌ RUIM: controller mistura tudo
@RestController
public class PostController {
    @PostMapping
    public Post criar(@RequestBody Post post) {
        if (post.getTitulo() == null) {
            throw new Exception();
        }
        String sql = "INSERT INTO posts ...";
        // SQL direto! Teste vai falhar, complexo demais
    }
}

// ✅ BOM: responsabilidades separadas
@RestController
public class PostController {
    @Autowired private PostService service;
    
    @PostMapping
    public ResponseEntity<PostResponse> criar(@RequestBody CriarPostRequest req) {
        Post post = service.criarPost(req);
        return ResponseEntity.status(201).body(new PostResponse(post));
    }
}

@Service
public class PostService {
    @Autowired private PostRepository repo;
    @Autowired private PostValidator validator;
    
    public Post criarPost(CriarPostRequest req) {
        validator.validar(req);  // Validar em layer dedicado
        Post post = mapperDTOtoEntity(req);
        return repo.save(post);
    }
}

// Validação separada
@Component
public class PostValidator {
    public void validar(CriarPostRequest req) {
        if (req.getTitulo() == null || req.getTitulo().isEmpty()) {
            throw new DadosInvalidosException("Título é obrigatório");
        }
    }
}
```

**2. Logging centralizado (observabilidade)**:

```java
package com.descomplica.frameblog.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PostService {
    
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    public Post criarPost(CriarPostRequest req) {
        logger.info("Iniciando criação de post, título: {}", req.getTitulo());
        
        try {
            validator.validar(req);
            Post post = mapperDTOtoEntity(req);
            Post salvo = repository.save(post);
            
            logger.info("Post criado com sucesso, ID: {}", salvo.getId());
            return salvo;
            
        } catch (Exception e) {
            logger.error("Erro ao criar post: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na criação", e);
        }
    }
}
```

No `application.properties`:
```properties
# Log levels por package
logging.level.com.descomplica.frameblog=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=DEBUG

# Formato do log
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.file.name=logs/application.log
```

**3. Testes unitários (confiabilidade)**:

```java
package com.descomplica.frameblog.services;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository repository;

    @InjectMocks
    private PostService service;

    @Test
    void testCriarPostComSucesso() {
        // Arrange (preparar)
        CriarPostRequest request = new CriarPostRequest("Título", "Conteúdo");
        Post post = new Post();
        post.setId(1L);
        when(repository.save(any())).thenReturn(post);

        // Act (executar)
        Post resultado = service.criarPost(request);

        // Assert (verificar)
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        verify(repository).save(any());
    }

    @Test
    void testCriarPostComTituloVazio() {
        // Arrange
        CriarPostRequest request = new CriarPostRequest("", "Conteúdo");

        // Act & Assert
        assertThrows(DadosInvalidosException.class, () -> {
            service.criarPost(request);
        });
    }
}
```

**4. Documentação com Swagger/OpenAPI**:

```java
package com.descomplica.frameblog.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
@Tag(name = "Posts", description = "Gerenciamento de posts do blog")
public class PostController {

    @GetMapping
    @Operation(
        summary = "Buscar todos os posts",
        description = "Retorna a lista de todos os posts publicados"
    )
    public ResponseEntity<List<PostResponse>> buscarTodos() {
        return ResponseEntity.ok(service.buscarTodos());
    }

    @PostMapping
    @Operation(summary = "Criar novo post")
    public ResponseEntity<PostResponse> criar(@RequestBody CriarPostRequest request) {
        Post post = service.criarPost(request);
        return ResponseEntity.status(201).body(new PostResponse(post));
    }
}
```

No `application.properties`:
```properties
springdoc.api-title=FrameBlog API
springdoc.api-version=1.0.0
springdoc.api-description=Documentação interativa da API
springdoc.swagger-ui.path=/swagger-ui.html
```

Acessa em: http://localhost:8080/swagger-ui.html

**5. CI/CD com GitHub Actions** (.github/workflows/test.yml):

```yaml
name: Tests & Build

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: 17
    
    - name: Build with Maven
      run: mvn clean build
    
    - name: Run Tests
      run: mvn test
    
    - name: Code Coverage
      run: mvn jacoco:report
    
    - name: Push Docker Image
      if: github.ref == 'refs/heads/main'
      run: |
        mvn dockerfile:build dockerfile:push
```

**6. Segurança (OWASP Top 10)**:

```java
@RestController
public class SecurityController {

    @GetMapping("/user/{id}")
    public ResponseEntity<UserResponse> buscarUsuario(@PathVariable Long id) {
        // ✅ Verificar autorização: user vê apenas seu próprio perfil
        Long userLogado = getCurrentUserId();
        
        if (!id.equals(userLogado)) {
            throw new AcessoNegadoException("Acesso negado");
        }
        
        User user = repository.findById(id).orElseThrow(
            () -> new UsuarioNaoEncontradoException(id)
        );
        
        // Nunca retorne senha
        return ResponseEntity.ok(new UserResponse(user));
    }
}
```

**7. Versionamento de dependências** (pom.xml):

```xml
<!-- ❌ RUIM: versão hardcode -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.1.5</version>
</dependency>

<!-- ✅ BOM: versão herdada de parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.5</version>
</parent>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- Versão vem do parent, automático -->
</dependency>
```

**8. Dockerfile para containerização**:

```dockerfile
# Multi-stage: primeiro build, depois runtime
FROM maven:3.9-eclipse-temurin-17 as builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-slim
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Resumo em tópicos

- **Separação de responsabilidades**: Controller, Service, Repository não se misturam
- **Logging**: registre tudo (debug facilitado)
- **Testes**: 50%+ cobertura mínimo (confiabilidade)
- **Documentação**: Swagger/OpenAPI (claro para consumidores)
- **CI/CD**: automação (menos erros humanos)
- **Segurança**: OWASP Top 10 (proteção)
- **Performance**: cache, índice, query optimization (user satisfaction)
- **Monitoring**: métricas e alertas (produção observável)

## Dúvidas frequentes (FAQ)

**P: Por onde começo com boas práticas?**  
R: 1. Testes (TDD). 2. Logging. 3. Tratamento de erro. 4. Documentação. 5. Segurança. Não tente tudo de uma vez.

**P: Teste unitário ou integração?**  
R: **Ambos**. Unitário (rápido, isolado): 70%. Integração (realista, conecta BD): 25%. E2E (UI): 5%.

**P: Qual ferramenta de CI/CD usar?**  
R: **GitHub Actions** (grátis, integrado). **GitLab CI**. **Jenkins** (complexo, on-premise). Para começar, GitHub Actions.

---

## Conclusão

Parabéns! Você completou os 15 módulos do curso FrameBlog! Agora você conhece:
- Fundação: ferramentas, API REST, banco de dados
- Segurança: autenticação, autorização
- Escalabilidade: cache, mensageria, load balancing
- Resiliência: tratamento de erro, circuit breaker
- Arquitetura: microsserviços, discovery, gateway
- Qualidade: boas práticas, testes, documentação

**Próximos passos**:
1. Implemente em um projeto real
2. Coloque em produção (AWS, Azure, GCP)
3. Meça performance (APM: New Relic, DataDog)
4. Estude padrões avançados (Event Sourcing, CQRS, DDD)

**Boa sorte! 🚀**

