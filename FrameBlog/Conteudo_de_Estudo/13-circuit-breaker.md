# 13 - Circuit Breaker

## O que é

**Circuit Breaker** é um padrão que **protege sua aplicação de falhas em cascata**. Funciona como um disjuntor elétrico: quando algo está muito quebrado, você fecha o "circuito" e evita continuarfazendo requisições.

Metáfora: Você tenta chamar um amigo, mas telefone está sempre sem sinal. Em vez de ficar tentando (e gastando bateria), desiste por um tempo. Depois tenta de novo.

Estados do Circuit Breaker:
- **CLOSED** (normal): chamadas passam normalmente
- **OPEN** (falha): para de chamar, retorna erro rápido
- **HALF_OPEN** (recuperação): tenta 1-2 chamadas para ver se voltou

## Por que existe / Para que serve

Cenário real:
```
API de Pagamento está offline
├─ Seu código tenta chamar
├─ Timeout (espera 10 segundos)
└─ Falha, tenta de novo
   └─ Timeout de novo (total: 20s)
      └─ E de novo (30s)
         └─ Threads ficam bloqueadas
            └─ Sua aplicação fica lenta/cai!
```

Com Circuit Breaker:
```
API de Pagamento está offline
├─ Primeira falha: tenta
├─ Segunda falha: tenta
├─ OPEN: para de tentar, retorna erro RÁPIDO (<100ms)
└─ Economia: não desgasta recurso esperando serviço morto
   Depois de X tempo, tenta 1 vez (HALF_OPEN)
   Se recuperou, volta a CLOSED
```

Benefícios:
- **Fail-fast**: identifica falhas rápido
- **Economia de recursos**: não bloqueia threads
- **Cascata evitada**: falha isolada, não propaga
- **Observabilidade**: sabe quando está falhando

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── src/main/java/.../
│   ├── clients/
│   │   ├── PagamentoClient.java       ← @CircuitBreaker
│   │   └── UsuarioClient.java         ← fallback
│   ├── config/
│   │   └── ResilienceConfig.java      ← Configuração
│   └── pom.xml                        ← spring-cloud-circuitbreaker
```

**Fluxo**:
1. Chama API externa
2. Falha? Incrementa contador
3. Muitas falhas? **OPEN** (pára tentativas)
4. Retorna erro rápido ou fallback
5. Depois de X tempo, tenta 1 vez (HALF_OPEN)

## Exemplo prático comentado

**Usando Resilience4j** (recomendado):

```java
package com.descomplica.frameblog.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.CompletableFuture;

@Component
public class PagamentoClient {

    private RestTemplate restTemplate = new RestTemplate();

    // name = identificador do circuit breaker
    // fallbackMethod = método a chamar se falhar
    @CircuitBreaker(name = "pagamentoService", fallbackMethod = "fallbackPagamento")
    @Retry(name = "pagamentoService")
    @TimeLimiter(name = "pagamentoService")
    public PagamentoResponse procesarPagamento(PagamentoRequest request) {
        // Se API de pagamento está down, vai falhar rápido (não espera)
        String url = "http://api-pagamento.com/processar";
        
        try {
            return restTemplate.postForObject(url, request, PagamentoResponse.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Erro ao processar pagamento: " + e.getMessage());
        }
    }

    // Fallback: o que fazer se circuito está OPEN
    public PagamentoResponse fallbackPagamento(PagamentoRequest request, Exception e) {
        System.out.println("CIRCUIT ABERTO! Fallback acionado: " + e.getMessage());
        
        // Opções:
        // 1. Retornar sucesso temporário (depois tenta manualmente)
        PagamentoResponse resposta = new PagamentoResponse();
        resposta.setStatus("PENDENTE");
        resposta.setMensagem("Sistema de pagamento indisponível. Tentaremos novamente em breve.");
        return resposta;
        
        // 2. Lançar exceção customizada (recomendado)
        // throw new PagamentoIndisponível("Sistema de pagamento down");
    }
}
```

**Configuração do Circuit Breaker** (application.yml):

```yaml
resilience4j:
  circuitbreaker:
    instances:
      pagamentoService:
        # Registra após 5 falhas
        failureRateThreshold: 50  # 50% de falhas
        slowCallRateThreshold: 100
        slowCallDurationThreshold: 2000  # 2 segundos é "lento"
        
        # Número de requisições para análise
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        
        # CLOSED → OPEN: após 5 chamadas falharem
        waitDurationInOpenState: 30000  # 30 segundos, depois tenta HALF_OPEN
        
        # HALF_OPEN: tenta esses quantos
        permittedNumberOfCallsInHalfOpenState: 2
        
        # Se sucesso em HALF_OPEN, volta a CLOSED
        automaticTransitionFromOpenToHalfOpenEnabled: true
        
        # Registra eventos
        recordExceptions:
          - java.net.SocketTimeoutException
          - java.util.concurrent.TimeoutException
          - java.io.IOException

  retry:
    instances:
      pagamentoService:
        maxAttempts: 3  # Tenta 3 vezes antes de falhar
        waitDuration: 1000  # 1 segundo entre tentativas

  timelimiter:
    instances:
      pagamentoService:
        timeoutDuration: 5000  # 5 segundos máximo
        cancelRunningFuture: true
```

**Monitorando o Circuit Breaker**:

```java
package com.descomplica.frameblog.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class CircuitBreakerMonitor {

    private CircuitBreakerRegistry registry;

    public CircuitBreakerMonitor(CircuitBreakerRegistry registry) {
        this.registry = registry;
        monitorarCircuitBreakers();
    }

    private void monitorarCircuitBreakers() {
        registry.getAllCircuitBreakers().forEach(cb -> {
            cb.getEventPublisher()
                .onStateTransition(event -> {
                    System.out.println("CircuitBreaker '" + cb.getName() + 
                        "' transicionou de " + event.getStateTransition().getFromState() + 
                        " para " + event.getStateTransition().getToState());
                    // Enviar métrica/alert
                });
        });
    }

    public String statusCircuitBreakers() {
        StringBuilder status = new StringBuilder();
        registry.getAllCircuitBreakers().forEach(cb -> {
            status.append(cb.getName())
                .append(": ")
                .append(cb.getState())
                .append("\n");
        });
        return status.toString();
    }
}
```

**Endpoint para ver status**:

```java
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CircuitBreakerMonitor monitor;

    @GetMapping("/circuit-breakers")
    public ResponseEntity<String> statusCircuitBreakers() {
        return ResponseEntity.ok(monitor.statusCircuitBreakers());
    }
}
```

## Conexão com Spring Boot

Spring Cloud oferece circuit breaker via:

| Implementação | Função |
|---|---|
| **Resilience4j** | Moderna, Java 8+ (recomendado) |
| **Hystrix** | Legado Netflix (ainda funciona) |
| **Cloud Circuit Breaker** | Abstração Spring (suporta múltiplas) |

No pom.xml:
```xml
<!-- Spring Cloud Circuit Breaker (abstração) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>

<!-- Actuator (expõe métricas de circuit breaker) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer (métricas) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
```

## Resumo em tópicos

- **Circuit Breaker** = Prot ege contra falhas em cascata
- **CLOSED** = Normal, chamadas passam
- **OPEN** = Falha detectada, para chamadas
- **HALF_OPEN** = Testa recuperação
- **Fallback** = Alternativa quando circuit aberto
- **Resilience4j** = Implementação moderna, recomendada
- **Retry** = Tenta novamente (combinado com circuit breaker)

## Dúvidas frequentes (FAQ)

**P: Quando abrir o circuito? Por quantas falhas?**  
R: Depende. Se 50% de falhas em 10 chamadas, abre. Se 100% em 5 chamadas, abre mais rápido. Ajuste conforme experiência.

**P: Qual é a diferença entre Retry e Circuit Breaker?**  
R: **Retry**: tenta novamente logo (se falha ocasional). **Circuit Breaker**: abre se muitas falhas (se serviço está realmente down). Use ambos juntos.

**P: E se o fallback também falhar?**  
R: Não deve! Fallback é local, rápido, não depende de serviço externo. Se fallback falha, é bug no seu código.

