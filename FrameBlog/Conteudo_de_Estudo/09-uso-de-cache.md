# 09 - Uso de Cache

## O que é

**Cache** é uma "memória rápida" intermediária. Em vez de buscar o mesmo dado repetidamente do banco (lento), você o armazena na memória (rápido). Próxima vez que precisa, pega de lá.

Metáfora: Livro na sua mesa vs livro na biblioteca.
- **Sem cache**: sempre va à biblioteca buscar (lento)
- **Com cache**: livro na mesa (rápido)

Cache é especialmente útil para dados:
- Que **mudam pouco** (posts, categorias, configurações)
- Que são **consultados frequentemente** (top posts, trending)
- Que vêm de **operações lentas** (cálculos, APIs externas)

## Por que existe / Para que serve

Sem cache (100 usuários acessando blog simultaneamente):
```
100 requisições → 100 queries ao banco → banco sobrecarregado
```

Com cache:
```
100 requisições → 1 query ao banco, 99 vêm do cache → banco respira
```

Benefícios:
- **Performance**: 10-1000x mais rápido
- **Reduz carga**: banco não fica abarrotado
- **Economia**: menos CPU/memória de servidor
- **Experiência**: usuários veem respostas em ms, não segundos

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── src/main/java/.../
│   ├── config/
│   │   └── CacheConfig.java             ← Configuração de cache
│   ├── services/
│   │   └── PostService.java             ← @Cacheable, @CacheEvict
│   ├── controllers/
│   │   └── PostController.java
│   └── pom.xml                          ← spring-boot-starter-cache + Redis/Caffeine
└── application.properties                ← cache.type=redis
```

Tipos de cache em Spring Boot:
- **In-Memory (Caffeine)**: armazena em RAM local do servidor (rápido, mas servidor morre = perde cache)
- **Redis**: servidor separado que armazena (mais seguro, distribuído, mas network latency)
- **Memcached**: parecido com Redis, mais leve

**Fluxo com cache**:
1. Cliente → GET /posts/100
2. Controller checa: está no cache?
3. **Sim** → retorna do cache (rápido!)
4. **Não** → busca do banco, armazena em cache, depois retorna

## Exemplo prático comentado

**Configuração de cache** (usando Caffeine):
```java
package com.descomplica.frameblog.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching  // Ativa anotações @Cacheable, @CacheEvict
public class CacheConfig {

    // Usando Caffeine (in-memory)
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // Tempo de vida: 1 hora
                .expireAfterWrite(1, TimeUnit.HOURS)
                // Máximo de entradas: 1000
                .maximumSize(1000)
                // Remove automaticamente se não acessado por 30 min
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build());

        return cacheManager;
    }
}
```

**Service usando cache**:
```java
package com.descomplica.frameblog.services;

import com.descomplica.frameblog.models.Post;
import com.descomplica.frameblog.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostRepository repository;

    // @Cacheable: se já foi executado com os MESMOS parâmetros,
    // retorna do cache. Não executa a função.
    @Cacheable(value = "postsCache")
    public List<Post> buscarTodos() {
        System.out.println("Buscando todos do BANCO (executada uma vez)");
        return repository.findAll();
    }

    // value = nome do cache
    // key = identificador único (padrão: argumentos do método)
    @Cacheable(value = "postCache", key = "#id")
    public Post buscarPorId(Long id) {
        System.out.println("Buscando post " + id + " do banco");
        return repository.findById(id).orElse(null);
    }

    // Cache por autor: chave é o ID do autor
    @Cacheable(value = "postsPorAutorCache", key = "#autorId")
    public List<Post> buscarPorAutor(Long autorId) {
        System.out.println("Buscando posts do autor " + autorId + " do banco");
        return repository.findByAutorId(autorId);
    }

    // @CacheEvict: remove entrada DO cache quando post é criado
    // allEntries=true: limpa TODO o cache de "postsCache"
    @CacheEvict(value = "postsCache", allEntries = true)
    public Post criarPost(Post post) {
        System.out.println("Criando novo post...");
        Post salvo = repository.save(post);
        
        // Também remove cache de posts por autor
        // Se quiser remover cache de "postsPorAutorCache"
        
        return salvo;
    }

    // Remove cache específico quando post é deletado
    @CacheEvict(value = {"postCache", "postsCache"}, 
                allEntries = false, 
                key = "#id")
    public void deletarPost(Long id) {
        System.out.println("Deletando post " + id);
        repository.deleteById(id);
    }

    // Atualizar: remove cache antigo
    @CacheEvict(value = {"postCache", "postsCache"}, 
                allEntries = true)
    public Post atualizarPost(Long id, Post novosDados) {
        Post post = repository.findById(id).orElse(null);
        if (post != null) {
            post.setTitulo(novosDados.getTitulo());
            post.setConteudo(novosDados.getConteudo());
            return repository.save(post);
        }
        return null;
    }
}
```

**Cenário prático**: Buscar top posts (popular cache):
```java
@Service
public class PostService {

    // @Cacheable com condition: só cacheia se resultado não for vazio
    @Cacheable(value = "topPostsCache", 
               condition = "#result != null && #result.size() > 0")
    public List<Post> buscarTop10Posts() {
        System.out.println("Calculando TOP 10 do banco...");
        return repository.findAll().stream()
                .sorted((a, b) -> b.getVisualizacoes().compareTo(a.getVisualizacoes()))
                .limit(10)
                .toList();
    }
}
```

**Exemplo com Redis** (mais robusto para produção):
```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // TTL: 1 hora
                .entryTtl(Duration.ofHours(1))
                // Tipo de serialização
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.create(factory);
    }
}
```

## Conexão com Spring Boot

Spring Boot oferece cache via annotations:

| Anotação | Efeito |
|---|---|
| **@Cacheable** | Retorna do cache se disponível, senão executa e armazena |
| **@CacheEvict** | Remove entrada do cache |
| **@CachePut** | Sempre executa, mas atualiza cache |
| **@Caching** | Combina múltiplas operações de cache |
| **@EnableCaching** | Ativa interceptação de cache |

No pom.xml:
```xml
<!-- Cache genérico -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Caffeine (In-memory, recomendado para começar) -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- OU Redis (distribuído, produção) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

No application.properties:
```properties
# Tipo de cache
spring.cache.type=caffeine

# OU para Redis
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379

# TTL padrão (em ms)
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=600s
```

## Resumo em tópicos

- **Cache** = Armazenamento rápido (memória) de dados frequentes
- **@Cacheable** = "Se está no cache, retorna; senão busca e armazena"
- **@CacheEvict** = Remove entrada do cache (quando dados mudam)
- **Caffeine** = Cache local, rápido, em-memória
- **Redis** = Cache distribuído (múltiplos servidores compartilham)
- **TTL (Time To Live)** = Tempo de vida de entrada no cache (1h, 24h)
- **key** = Identificador único da entrada (geralmente args do método)

## Dúvidas frequentes (FAQ)

**P: Devo cachear tudo?**  
R: **Não!** Cache dados **imutáveis** ou que **mudam raramente** (posts, categorias). **Nunca** cacheia dados pessoais (perfil de usuário, carrinho de compras).

**P: E se os dados mudarem mas o cache não souber?**  
R: Use @CacheEvict quando salvar/deletar. Ou defina TTL baixo (5-30 min) para dados que mudam frequentemente.

**P: Caffeine vs Redis?**  
R: **Caffeine**: rápido, simples, mas só funciona em um servidor. **Redis**: distribuído (múltiplos servidores), mais robusto, mas com network latency. Use Caffeine para começar.

