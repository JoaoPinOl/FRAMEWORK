# 11 - Balanceamento de Carga

## O que é

**Balanceamento de carga** é distribuir requisições entre múltiplos servidores. Em vez de um servidor processar tudo, você tem 3-10 servidores idênticos, e um **balanceador** (load balancer) distribui: requisição 1 → servidor A, requisição 2 → servidor B, requisição 3 → servidor C.

Metáfora: Em vez de uma caixa de supermercado processando 100 clientes, você tem 5 caixas, e um gerente dirige cada cliente para a caixa menos ocupada.

## Por que existe / Para que serve

Sem balanceamento de carga:
```
100 usuários em horário pico
→ 1 servidor recebe 100 requisições
→ Servidor fica lento/cai
→ Erro 500, site fora do ar
```

Com balanceamento de carga:
```
100 usuários em horário pico
→ 5 servidores, cada um recebe ~20 requisições
→ Cada servidor responde rápido
→ Servidor cai? Os outros continuam
```

Benefícios:
- **Escalabilidade**: suporta mais usuários simultaneamente
- **Alta disponibilidade**: se um cai, os outros cobrem
- **Performance**: divide carga equitativamente
- **Manutenção**: pode reiniciar servidor sem desligar site

## Como funciona no projeto

```
                  ┌─ Servidor 1 (porta 8081)
Cliente ← → Load Balancer (Nginx/HAProxy)
                  ├─ Servidor 2 (porta 8082)
                  ├─ Servidor 3 (porta 8083)
                  └─ Servidor 4 (porta 8084)
```

Em desenvolvimento, você roda **uma ou duas instâncias** Spring Boot na mesma máquina (portas diferentes). Em produção, você roda em **máquinas diferentes**.

**Exemplo local**:
- Servidor 1: http://localhost:8081
- Servidor 2: http://localhost:8082
- Servidor 3: http://localhost:8083
- Load Balancer: http://localhost:80 (ou :8080)

Requisição bate em :80, load balancer escolhe qual servidor responde.

## Exemplo prático comentado

**Arquitetura com Docker Compose** (3 instâncias Spring Boot + Nginx load balancer):

Arquivo: `docker-compose.yml`
```yaml
version: '3.8'
services:
  # Instância 1 do Spring Boot
  app-server-1:
    build: .
    environment:
      - SERVER_PORT=8081
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/frameblog
    ports:
      - "8081:8081"
    depends_on:
      - mysql

  # Instância 2 do Spring Boot
  app-server-2:
    build: .
    environment:
      - SERVER_PORT=8082
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/frameblog
    ports:
      - "8082:8082"
    depends_on:
      - mysql

  # Instância 3 do Spring Boot
  app-server-3:
    build: .
    environment:
      - SERVER_PORT=8083
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/frameblog
    ports:
      - "8083:8083"
    depends_on:
      - mysql

  # Load Balancer (Nginx)
  nginx:
    image: nginx:latest
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - app-server-1
      - app-server-2
      - app-server-3

  # Banco de dados compartilhado
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: frameblog
    ports:
      - "3306:3306"
```

**Configuração Nginx** (load balancer):

Arquivo: `nginx.conf`
```nginx
upstream frameblog_backend {
    # Algoritmo: round-robin (distribui igualmente)
    server app-server-1:8081;
    server app-server-2:8082;
    server app-server-3:8083;
}

server {
    listen 80;
    server_name localhost;

    location / {
        # Encaminha para um servidor da lista
        proxy_pass http://frameblog_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

**Configuração Spring Boot para cluster**:

No `application.properties` (cada instância):
```properties
# Porta (diferente para cada instância via env var)
server.port=${SERVER_PORT:8081}

# Nome da instância (para debug/logs)
spring.application.name=frameblog

# Sessão distribuída (importante para múltiplos servidores!)
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always

# Banco de dados
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/frameblog}
spring.datasource.username=root
spring.datasource.password=root

# Logging: mostra qual servidor respondeu
logging.level.com.descomplica.frameblog=DEBUG
```

**Health Check** (Nginx monitora servidores):

Adicione endpoint no controller:
```java
@RestController
@RequestMapping("/actuator")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
```

Nginx testa periodicamente:
```nginx
upstream frameblog_backend {
    server app-server-1:8081;
    server app-server-2:8082;
    server app-server-3:8083;
    
    # Health check
    check interval=3000 rise=2 fall=5 timeout=1000 type=http;
    check_http_send "GET /actuator/health HTTP/1.0\r\n\r\n";
    check_http_expect_alive http_2xx;
}
```

**Estratégias de distribuição**:

```nginx
# 1. Round-robin (padrão, iguala distribuição)
upstream backend {
    server server1:8081;
    server server2:8082;
}

# 2. Least connections (menos requisições ativas)
upstream backend {
    least_conn;
    server server1:8081;
    server server2:8082;
}

# 3. IP Hash (mesma origem sempre vai pro mesmo servidor)
upstream backend {
    ip_hash;
    server server1:8081;
    server server2:8082;
}

# 4. Weighted (alguns servidores mais poderosos)
upstream backend {
    server server1:8081 weight=3;
    server server2:8082 weight=1;
}
```

## Conexão com Spring Boot

Spring Boot **naturalmente suporta** múltiplas instâncias:

| Conceito | Necessário? |
|---|---|
| **Múltiplas instâncias** | Sim, rode em portas diferentes |
| **Session compartilhada** | Sim, use spring-session-jdbc |
| **Cache compartilhado** | Recomendado, use Redis |
| **Banco de dados** | Compartilhado (todas apontam para mesmo DB) |

No pom.xml:
```xml
<!-- Spring Session (distribute sessões entre servidores) -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-jdbc</artifactId>
</dependency>

<!-- Redis (cache distribuído) -->
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

- **Load Balancer** = Distribui requisições entre múltiplos servidores
- **Round-robin** = Algoritmo padrão (distribui igualmente)
- **Health Check** = Verifica se servidor está vivo
- **Session compartilhada** = spring-session-jdbc (todas instâncias vêem mesma sessão)
- **Cache distribuído** = Redis (todos servidores compartilham cache)
- **Sem ponto único de falha** = Se um servidor cai, outros continuam
- **Docker/Kubernetes** = Orquestração de containers (escalagem automática)

## Dúvidas frequentes (FAQ)

**P: Quantos servidores preciso?**  
R: Comece com 2. Se não aguenta pico, adicione mais. Regra: 20-30% de margem (se máximo é 10k req/s, rode com 7-8k).

**P: Como sincronizar estado entre servidores?**  
R: Use **banco de dados** para dados persistentes, **Redis** para cache/sessão, **RabbitMQ** para eventos. Nunca dependa de "arquivo local".

**P: Nginx vs HAProxy?**  
R: **Nginx**: simples, usado em 70% dos casos. **HAProxy**: mais robusto, maior controle. Para começar, use Nginx.

