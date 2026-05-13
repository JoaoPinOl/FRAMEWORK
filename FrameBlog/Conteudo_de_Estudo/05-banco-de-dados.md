# 05 - Acrescentando Banco de Dados

## O que é

Um **banco de dados** é o "arquivo permanente" da sua aplicação. Sem ele, quando o servidor reinicia, todos os dados desaparecem. Um banco de dados:
- **Persistência**: guarda dados mesmo depois de desligar
- **Estrutura**: organiza dados em tabelas (linhas e colunas)
- **Consultas**: permite buscar, atualizar, até deletar dados
- **Relacionamentos**: conecta dados entre tabelas (um usuário → muitos posts)

Bancos populares: MySQL, PostgreSQL, MongoDB, MariaDB.

## Por que existe / Para que serve

Imagine um blog sem banco de dados:
```java
List<Post> posts = new ArrayList<>();  // Só enquanto servidor rodando!
posts.add(new Post("Meu primeiro post"));
// Depois de reiniciar... perdido!
```

Com banco de dados:
```
┌─────────────────┐
│   Aplicação     │
│   (Java)        │
└────────┬────────┘
         │
      HTTP
         │
┌────────▼──────────┐
│  Banco de Dados   │
│  (MySQL/Postgres) │
│                   │
│ ┌─────────────┐   │
│ │ posts       │   │
│ │ id|título   │   │
│ │ 1 |"Bem-vindo"   │
│ │ 2 |"Tutorial"│   │
│ └─────────────┘   │
└───────────────────┘
```

Dados persistem, múltiplas aplicações podem acessar simultaneamente, backup é possível.

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── pom.xml                         ← spring-boot-starter-data-jpa, driver MySQL
├── src/main/resources/
│   ├── application.properties       ← Dados de conexão ao banco
│   └── db/migration/                ← Versionamento do banco (Flyway)
│       ├── V1__Create_database.sql  ← Criar tabelas
│       └── V2__Add_columns.sql      ← Modificar estrutura
├── src/main/java/.../
│   ├── models/                      ← @Entity: classes mapeadas a tabelas
│   │   ├── Post.java
│   │   ├── User.java
│   │   └── Comment.java
│   ├── repository/                  ← JpaRepository: queries automáticas
│   │   ├── PostRepository.java
│   │   └── UserRepository.java
│   └── services/                    ← Lógica usando dados do banco
│       └── PostService.java
```

**Tipos de relacionamentos**:
- **One-to-Many** (Um-para-Muitos): Um usuário tem muitos posts
- **Many-to-One** (Muitos-para-Um): Muitos posts pertencem a um usuário
- **Many-to-Many** (Muitos-para-Muitos): Posts pode ter muitas tags, tags podem estar em muitos posts

## Exemplo prático comentado

**Model de User com relacionamento**:
```java
package com.descomplica.frameblog.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String senha;

    // UM usuário TEM MUITOS posts
    // fetch = FetchType.LAZY: não carrega posts automaticamente (melhor performance)
    @OneToMany(mappedBy = "autor", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Post> posts = new ArrayList<>();

    public Long getId() { return id; }
    public List<Post> getPosts() { return posts; }
}
```

**Model de Post com relacionamento**:
```java
package com.descomplica.frameblog.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String conteudo;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    // MUITOS posts PERTENCEM A UM autor
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")  // Coluna na tabela posts
    private User autor;

    // UM post TEM MUITOS comentários
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comentarios = new ArrayList<>();

    public Long getId() { return id; }
    public User getAutor() { return autor; }
    public List<Comment> getComentarios() { return comentarios; }
}
```

**Model de Comment**:
```java
package com.descomplica.frameblog.models;

import jakarta.persistence.*;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String conteudo;

    // MUITOS comentários PERTENCEM A UM post
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User autor;

    public Long getId() { return id; }
    public Post getPost() { return post; }
}
```

**Repository**:
```java
package com.descomplica.frameblog.repository;

import com.descomplica.frameblog.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    // Queries automáticas: Spring Data gera SQL
    List<Post> findByAutor(User autor);
    
    List<Post> findByTituloContainingIgnoreCase(String titulo);
    
    List<Post> findByAutorOrderByIdDesc(User autor);
}
```

**Service usando banco de dados**:
```java
package com.descomplica.frameblog.services;

import com.descomplica.frameblog.models.Post;
import com.descomplica.frameblog.models.User;
import com.descomplica.frameblog.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostRepository repository;

    // Salvar novo post no banco
    public Post criarPost(Post post) {
        return repository.save(post);  // INSERT INTO posts ...
    }

    // Buscar todos do banco
    public List<Post> buscarTodos() {
        return repository.findAll();  // SELECT * FROM posts
    }

    // Buscar por autor
    public List<Post> buscarPorAutor(User autor) {
        // Query customizada (Spring Data gera SQL automaticamente)
        return repository.findByAutor(autor);  // SELECT * FROM posts WHERE user_id = ?
    }

    // Atualizar no banco
    public void atualizarPost(Post post) {
        repository.save(post);  // UPDATE posts SET ...
    }

    // Deletar do banco
    public void deletarPost(Long id) {
        repository.deleteById(id);  // DELETE FROM posts WHERE id = ?
    }
}
```

**Migration SQL (primeiro setup do banco)**:

Arquivo: `src/main/resources/db/migration/V1__Create_database.sql`
```sql
-- Cria tabela de usuários
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Cria tabela de posts
CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    conteudo LONGTEXT NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Cria tabela de comentários
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conteudo TEXT NOT NULL,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id)
);

-- Índices para melhorar performance
CREATE INDEX idx_posts_user ON posts(user_id);
CREATE INDEX idx_comments_post ON comments(post_id);
```

## Conexão com Spring Boot

Spring Boot oferece JPA (Java Persistence API):

| Conceito | O que faz |
|---|---|
| **@Entity** | Marca classe como tabela do banco |
| **@Id** | Define coluna como chave primária |
| **@GeneratedValue** | Auto-incrementar IDs |
| **@OneToMany, @ManyToOne** | Define relacionamentos |
| **JpaRepository** | Interface que oferece query métodos automáticos |
| **Flyway/Liquibase** | Versionamento do banco (migrations) |

No `pom.xml`:
```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Driver MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- Flyway (versionamento do banco) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

No `application.properties`:
```properties
# Dados de conexão
spring.datasource.url=jdbc:mysql://localhost:3306/frameblog
spring.datasource.username=root
spring.datasource.password=sua-senha

# Automático: cria tabelas a partir das @Entity
spring.jpa.hibernate.ddl-auto=validate  # ou 'update' em dev

# Mostra SQL que está executando (debug)
spring.jpa.show-sql=true

# Formata SQL para ler melhor
spring.jpa.properties.hibernate.format_sql=true
```

## Resumo em tópicos

- **Banco de Dados** = Armazenamento permanente de dados em tabelas estruturadas
- **@Entity** = Marca classe Java que corresponde a uma tabela no banco
- **@ManyToOne** = Muitos registros apontam para um (relação de posse)
- **@OneToMany** = Um registro tem múltiplos (relação inversa)
- **JpaRepository** = Interface que oferece CRUD (Create, Read, Update, Delete)
- **Flyway** = Controla versão do banco (migrations: V1, V2, V3...)
- **Spring Data** = Gera queries SQL automaticamente baseado em nomes de métodos

## Dúvidas frequentes (FAQ)

**P: Qual é a diferença entre FetchType.LAZY e FetchType.EAGER?**  
R: **LAZY** (padrão): só carrega dados quando você acessa (melhor performance). **EAGER**: carrega tudo de uma vez. Use LAZY por default, mude para EAGER só se necessário.

**P: Como apago um usuário se ele tem múltiplos posts?**  
R: Use `cascade = CascadeType.ALL` no @OneToMany. Assim, deletar usuário deleta posts automaticamente. Cuidado: isso é permanente!

**P: O que é uma "N+1 query"?**  
R: Quando você busca 100 posts (1 query), mas depois itera e acessa o autor de cada um (100 queries). Total: 101 queries! Evite com JOIN FETCH ou batch fetching.

