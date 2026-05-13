# 12 - Service Registry e Service Discovery

## O que é

**Service Registry** é um "catálogo de todos os serviços" rodando em uma infraestrutura. **Service Discovery** é o processo de achar um serviço nesse catálogo automaticamente.

Pense num cenário com microsserviços:
- Serviço de posts (porta 8081, 8082, 8083)
- Serviço de usuários (porta 9001, 9002)
- Serviço de notificações (porta 9101)

Em vez de hardcode "chame usuários em http://localhost:9001", você diz "preciso do serviço de usuários" e o sistema acha automaticamente.

## Por que existe / Para que serve

Sem Service Discovery:
```
// No código: hardcode
String urlUsuarios = "http://localhost:9001";
// E se serviço de usuários mudou pra 9003? Código quebra!
```

Com Service Discovery:
```
// No código: nome do serviço
String urlUsuarios = discoveryClient.getInstances("usuarios").get(0).getUri();
// Sistema acha automaticamente, mesmo se IP/porta mudar
```

Benefícios:
- **Resiliência**: se uma instância cai, outro pega
- **Elasticidade**: novo servidor? Registry atualizao automaticamente
- **Load balancing**: cliente ignora detalhes, pede serviço
- **Manutenção**: sem modificar código

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── src/main/java/.../
│   ├── clients/
│   │   ├── UsuarioClient.java         ← Usa DiscoveryClient
│   │   └── NotificacaoClient.java
│   ├── config/
│   │   └── DiscoveryConfig.java       ← Configuração Eureka
│   └── services/
│       └── PostService.java           ← Cria clients
├── pom.xml                            ← spring-cloud-starter-eureka-client
└── application.properties             ← eureka.client.service-url.defaultZone
```

Arquitetura completa:
```
┌─────────────────────────────────────┐
│      Eureka Server (Registry)        │
│  Catálogo: usuarios:9001, posts:8081│
└─────────────────────────────────────┘

     ↑ registra             ↑ registra
     │                      │
┌────┴─────┐          ┌─────┴────┐
│ Usuarios  │          │   Posts  │
│ (:9001)   │◄────────◄│ (:8081)  │
└───────────┘          └──────────┘
```

## Exemplo prático comentado

**Configurar Eureka Server** (servidor de registro, separa lmente):

Se usar Eureka, rode um servidor:
```xml
<!-- pom.xml do servidor Eureka -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```properties
# application.properties (Eureka Server)
server.port=8761
spring.application.name=eureka-server

# Não registrar a si mesmo
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false

# Dashboard em http://localhost:8761
```

**Cliente (seu Blog) registrando no Eureka**:

```properties
# application.properties
spring.application.name=frameblog
server.port=8081

# Eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.hostname=localhost
```

```java
@SpringBootApplication
@EnableDiscoveryClient  // Registra em Eureka
public class FrameBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(FrameBlogApplication.class, args);
    }
}
```

**Usar Service Discovery para chamar outro serviço**:

```java
package com.descomplica.frameblog.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Component
public class UsuarioClient {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private RestTemplate restTemplate;

    public Usuario buscarUsuario(Long id) {
        // 1. Descobre instância do serviço "usuarios"
        List<ServiceInstance> instances = discoveryClient.getInstances("usuarios");
        
        if (instances.isEmpty()) {
            throw new RuntimeException("Serviço 'usuarios' não encontrado");
        }

        // 2. Pega uma instância (primeira, ou com load balancer)
        ServiceInstance instance = instances.get(0);
        
        // 3. Constrói URL dinamicamente
        String url = instance.getUri() + "/usuarios/" + id;
        
        // 4. Chama serviço
        return restTemplate.getForObject(url, Usuario.class);
    }
}
```

**Usando Feign Client** (mais elegante, com load balancing):

```java
package com.descomplica.frameblog.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// "usuarios" deve estar registrado no Eureka
@FeignClient(name = "usuarios")
public interface UsuarioFeignClient {
    
    @GetMapping("/usuarios/{id}")
    Usuario buscarUsuario(@PathVariable Long id);
}
```

No Service:
```java
@Service
public class PostService {

    @Autowired
    private UsuarioFeignClient usuarioClient;

    public void validarAutor(Long autorId) {
        // Feign automaticamente:
        // 1. Descobre instância de "usuarios"
        // 2. Faz load balancing
        // 3. Chama endpoint
        Usuario autor = usuarioClient.buscarUsuario(autorId);
        System.out.println("Autor encontrado: " + autor.getNome());
    }
}
```

**Tratamento de falha com Hystrix (Circuit Breaker)**:

```java
@FeignClient(name = "usuarios", fallback = UsuarioFallback.class)
public interface UsuarioFeignClient {
    @GetMapping("/usuarios/{id}")
    Usuario buscarUsuario(@PathVariable Long id);
}

// Fallback: o que faz se serviço de usuários cair
@Component
public class UsuarioFallback implements UsuarioFeignClient {
    @Override
    public Usuario buscarUsuario(Long id) {
        // Retorna usuário vazio ou cached
        Usuario fallback = new Usuario();
        fallback.setId(id);
        fallback.setNome("Usuário indisponível");
        return fallback;
    }
}
```

## Conexão com Spring Boot

Spring Cloud oferece Service Discovery:

| Tecnologia | Função |
|---|---|
| **Eureka (Netflix)** | Service Registry built-in |
| **Consul (Hashicorp)** | Alternativa mais potente |
| **Feign** | Cliente HTTP declarativo (integra com discovery) |
| **Ribbon** | Load balancer do lado do cliente |
| **Hystrix** | Circuit breaker (tolera falhas) |

No pom.xml:
```xml
<!-- Spring Cloud Starter Parent (necessário) -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Discovery Client (Eureka) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netfl ix-eureka-client</artifactId>
</dependency>

<!-- Feign (cliente HTTP) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Hystrix (circuit breaker) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>
```

No application.properties:
```properties
spring.application.name=frameblog
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

# Feign timeout
feign.client.config.default.connectTimeout=5000
feign.client.config.default.readTimeout=5000

# Hystrix
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds=5000
```

## Resumo em tópicos

- **Service Registry** = Catálogo central de todos os serviços
- **Service Discovery** = Achar serviço automaticamente no registry
- **Eureka** = Service Registry do Spring Cloud
- **Feign** = Cliente HTTP declarativo (integra com discovery)
- **Load Balancing** = Cliente automáticamente distribui entre instâncias
- **Resilience** = Se serviço cai, outro pega; código não quebra
- **Fallback** = Comportamento alternativo se serviço indisponível

## Dúvidas frequentes (FAQ)

**P: Preciso de Eureka para microsserviços?**  
R: **Sim, se** muitos serviços com instâncias dinâmicas. **Não, se** são 2-3 serviços estáticos (pode hardcode URL). Para começar com microsserviços, use Eureka.

**P: Qual é a diferença entre Eureka, Consul e Kubernetes?**  
R: **Eureka**: simples, em-aplicação. **Consul**: externo, mais features. **Kubernetes**: orquestração completa, descobre serviços automaticamente. Hoje em dia, Kubernetes é o padrão.

**P: E se não quiser Eureka e usar Kubernetes?**  
R: Kubernetes descobre serviços via DNS (exemplo: `usuarios.default.svc.cluster.local`). Sem Eureka, sem Feign, tudo nativo.

