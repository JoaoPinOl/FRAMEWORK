# 14 - API Gateway

## O que é

**API Gateway** é um "rececionista centralizado" que fica na frente de todos os microsserviços. Em vez de clientes falarem diretamente com 10 serviços diferentes, falam com 1 gateway. O gateway conhece todos e roteia requisições.

Metáfora: Recepcionista de condomínio.
- **Sem gateway**: visitante bate em todo apartamento procurando
- **Com gateway**: visitante fala na portaria, porteiro encaminha

## Por que existe / Para que serve

Arquitetura sem API Gateway (monolítico ou microserviços simples):
```
Cliente ← → Serviço 1 (posts)
         ← → Serviço 2 (usuários)
         ← → Serviço 3 (pagamento)
         ← → Serviço 4 (notificações)
```

Problemnas:
- Cliente precisa conhecer URL de cada serviço
- Se serviço muda de host, cliente quebra
- Sem autenticação centralizada
- Sem rate limiting centralizado

Com API Gateway:
```
Cliente ← → API Gateway ← → Serviço 1
                        ← → Serviço 2
                        ← → Serviço 3
```

Vantagens:
- **Single entry point**: cliente só conhece gateway
- **Autenticação centralizada**: verifica JWT uma vez
- **Rate limiting**: controla requisições
- **Roteamento inteligente**: roteia para serviço correto
- **Transformação**: adapta request/response
- **Logging/Monitoring**: vê tudo que passa

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── API Gateway (Servidor separado)
│   ├── Routes: /posts → :8081
│   ├── Routes: /usuarios → :9001
│   └── Filters: autenticação, rate limit
│
├── Serviço Blog (:8081)
│   └── Só endpoints de posts
│
├── Serviço Usuários (:9001)
│   └── Só endpoints de usuários
│
└── Serviço Pagamento (:9101)
    └── Só endpoints de pagamento
```

Cliente chama `http://gateway:8080/posts/1` e gateway roteia para `http://blog:8081/posts/1`.

## Exemplo prático comentado

**Usar Spring Cloud Gateway** (API Gateway do Spring):

Criar projeto separado:
```
FrameBlogGateway/
├── pom.xml
├── src/main/java/.../
│   └── GatewayApplication.java
└── application.yml
```

```java
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

**Configuração de rotas** (application.yml):

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  
  cloud:
    gateway:
      routes:
        # Rota para serviço de Posts
        - id: posts-service
          uri: http://localhost:8081
          predicates:
            - Path=/posts/**
          filters:
            - StripPrefix=0  # Não remove prefixo

        # Rota para serviço de Usuários
        - id: usuarios-service
          uri: http://localhost:9001
          predicates:
            - Path=/usuarios/**
          filters:
            - StripPrefix=0

        # Rota para pagamento com autenticação
        - id: pagamento-service
          uri: http://localhost:9101
          predicates:
            - Path=/pagamento/**
          filters:
            - AuthFilter  # Filtro customizado
```

**Filtro customizado** (autenticação centralizada):

```java
package com.descomplica.frameblog.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.List;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private JwtTokenProvider jwtProvider;

    public AuthFilter(JwtTokenProvider jwtProvider) {
        super(Config.class);
        this.jwtProvider = jwtProvider;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 1. Extrai token do header
            String token = extractToken(exchange);

            if (token == null) {
                return handleUnauthorized(exchange);
            }

            // 2. Valida token
            if (!jwtProvider.validarToken(token)) {
                return handleUnauthorized(exchange);
            }

            // 3. Token válido, passa adiante
            String email = jwtProvider.extrairEmail(token);
            
            // 4. Adiciona email ao header para serviço usar
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.header("X-User-Email", email))
                    .build();

            return chain.filter(mutatedExchange);
        };
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(
            org.springframework.http.HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().writeWith(
            Mono.just(
                exchange.getResponse()
                    .bufferFactory()
                    .wrap("{\"erro\": \"Não autenticado\"}".getBytes())
            )
        );
    }

    public static class Config {
        // Configuração do filtro (se necessário)
    }
}
```

**Rate Limiting no Gateway**:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: posts-service
          uri: http://localhost:8081
          predicates:
            - Path=/posts/**
          filters:
            # RequestRateLimiter: limite 100 requisições por minuto
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
                key-resolver: "#{@userKeyResolver}"

# Redis para rate limiter
spring:
  redis:
    host: localhost
    port: 6379
```

```java
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        // Limita por usuário (extrai email do header que gateway adicionou)
        return exchange -> {
            String userEmail = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Email");
            return Mono.just(userEmail != null ? userEmail : "anonymous");
        };
    }
}
```

**Monitoramento no Gateway**:

```java
@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
```

## Conexão com Spring Boot

Spring oferece API Gateway via Spring Cloud Gateway:

| Conceito | Função |
|---|---|
| **Routes** | Define caminho → serviço |
| **Predicates** | Condição para matched (Path, Host, Header) |
| **Filters** | Modifica requisição/resposta |
| **GatewayFilter** | Filtro que intercepta requisição |
| **Rate Limiter** | Limita requisições por usuário/IP |

No pom.xml (do gateway, projeto separado):
```xml
<!-- Spring Cloud Gateway -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<!-- Redis (opcionalpara rate limiter) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Actuator (health check) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Resumo em tópicos

- **API Gateway** = Ponto de entrada único para todos os serviços
- **Roteamento** = Encaminha requisição ao serviço correto
- **Autenticação centralizada** = Verifica JWT uma vez (não em cada serviço)
- **Rate Limiting** = Controla quantas requisições por usuário
- **Spring Cloud Gateway** = Implementação Spring (recomendado)
- **Filtros** = Modificam requisição antes de rotear
- **Resilência** = Timeout, retry, fallback para serviços

## Dúvidas frequentes (FAQ)

**P: Devo autenticar no gateway ou em cada serviço?**  
R: **No gateway** (mais eficiente). Serviços confiam no header X-User-Email que gateway adiciona. Exceção: se serviço pode ser chamado diretamente, também autentica.

**P: API Gateway ou Nginx?**  
R: **Nginx**: simples, HTTP reverse proxy. **API Gateway**: complexo, suporta transformações, circuit breaker. Para microsserviços, use API Gateway. Para proxy simples, Nginx.

**P: E se o gateway fica down?**  
R: Não pode! Use alta disponibilidade (múltiplas instâncias + load balancer). Gateway é crítico, é ponto único de falha.

