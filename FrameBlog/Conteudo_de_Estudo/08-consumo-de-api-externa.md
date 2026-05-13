# 08 - Consumo de API Externa

## O que é

Consumir uma API externa significa: sua aplicação **chama outra API** para obter dados. Você não está criando um endpoint, está **ser um cliente** chamando outro servidor.

Exemplos:
- Seu blog pede a lista de usuários de outro servidor
- Seu blog pede dados de clima de API pública
- Seu blog consulta preços de produtos em servidor de pagamento

## Por que existe / Para que serve

Cenários do mundo real:
- **Integração entre sistemas**: seu blog integra com sistema de pagamento (Stripe, PayPal)
- **Dados de terceiros**: consumir API de previsão do tempo, taxas de câmbio
- **Microsserviços**: seu backend chama outro backend para dados específicos
- **Escalabilidade**: não reinventar a roda, reutilizar APIs existentes

Vantagens:
- Não precisa armazenar tudo localmente
- Acessa dados em tempo real
- Divide responsabilidades (seu blog foca em blog, outro servidor em pagamentos)

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── src/main/java/.../
│   ├── clients/                       ← NOVO! Clientes HTTP
│   │   ├── ViaCepClient.java          ← Consome API de CEP
│   │   ├── PaymentClient.java         ← Consome API de pagamento
│   │   └── TodoClient.java            ← Exemplo: consome JSONPlaceholder
│   ├── config/                        ← Configuração HTTP
│   │   └── HttpClientConfig.java      ← RestTemplate ou WebClient
│   ├── models/                        ← Dados externos mapeados
│   │   ├── CepData.java
│   │   └── TodoData.java
│   ├── controllers/                   ← Controllers chamam services
│   ├── services/                      ← Services usam clients
│   └── pom.xml                        ← spring-boot-starter-webflux ou RestTemplate
```

**Fluxo**:
1. Cliente → Seu endpoint (POST /posts com CEP do autor)
2. **Service** chama **Client**
3. **Client** faz HTTP GET na API externa
4. Recebe resposta JSON
5. Mapeia para classe Java (CepData)
6. Service processa dados juntos
7. Retorna resposta ao cliente

## Exemplo prático comentado

**Model para dados da API externa** (ViaCEP - API brasileira gratuita):
```java
package com.descomplica.frameblog.models;

// Mapeia resposta JSON da API ViaCEP
public class CepData {
    private String cep;
    private String logradouro;
    private String bairro;
    private String localidade;
    private String uf;

    // Getters e Setters
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    // ... etc
}
```

**Cliente HTTP** (usando RestTemplate):
```java
package com.descomplica.frameblog.clients;

import com.descomplica.frameblog.models.CepData;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Component
public class ViaCepClient {

    private RestTemplate restTemplate = new RestTemplate();

    // Publica API: buscar dados de um CEP
    public CepData buscarCep(String cep) {
        try {
            String url = "https://viacep.com.br/ws/" + cep + "/json/";
            
            // GET request: retorna JSON, mapeia para CepData
            CepData data = restTemplate.getForObject(url, CepData.class);
            
            return data;
        } catch (RestClientException e) {
            System.out.println("Erro ao buscar CEP: " + e.getMessage());
            return null;
        }
    }
}
```

**Cliente HTTP** (usando WebClient - moderno, não-bloqueante):
```java
package com.descomplica.frameblog.clients;

import com.descomplica.frameblog.models.CepData;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ViaCepClientReativo {

    private WebClient webClient = WebClient.create("https://viacep.com.br");

    // Retorna Mono: não-bloqueante
    public Mono<CepData> buscarCepReativo(String cep) {
        return webClient
                .get()
                .uri("/ws/{cep}/json/", cep)
                .retrieve()
                .bodyToMono(CepData.class)
                .onErrorResume(e -> {
                    System.out.println("Erro: " + e.getMessage());
                    return Mono.empty();
                });
    }
}
```

**Service usando o cliente**:
```java
package com.descomplica.frameblog.services;

import com.descomplica.frameblog.clients.ViaCepClient;
import com.descomplica.frameblog.models.CepData;
import com.descomplica.frameblog.models.Post;
import com.descomplica.frameblog.request.CriarPostComCepRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    @Autowired
    private ViaCepClient viaCepClient;

    // Criar post com validação de CEP do autor
    public Post criarPostComCep(CriarPostComCepRequest request) {
        // 1. Busca dados do CEP na API externa
        CepData cepData = viaCepClient.buscarCep(request.getCep());
        
        if (cepData == null) {
            throw new RuntimeException("CEP inválido: " + request.getCep());
        }

        // 2. Cria post enriquecido com dados do CEP
        Post post = new Post();
        post.setTitulo(request.getTitulo());
        post.setConteudo(request.getConteudo());
        // Adiciona localização do autor
        post.setUf(cepData.getUf());
        post.setCidade(cepData.getLocalidade());

        // 3. Salva no banco
        return repository.save(post);
    }
}
```

**Configuração do RestTemplate** (no SecurityConfig ou separate):
```java
package com.descomplica.frameblog.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class HttpClientConfig {

    // RestTemplate: cliente HTTP síncrono (bloqueante)
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .requestFactory(this::clientHttpRequestFactory)
                .build();
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 segundos
        factory.setReadTimeout(10000);    // 10 segundos
        return new BufferingClientHttpRequestFactory(factory);
    }

    // WebClient: cliente HTTP assíncrono (não-bloqueante)
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            System.out.println("Request: " + clientRequest.getMethod() + " " + clientRequest.getURL());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            System.out.println("Response: " + clientResponse.getStatusCode());
            return Mono.just(clientResponse);
        });
    }
}
```

**Controller consumindo o serviço**:
```java
@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService service;

    @PostMapping("/com-cep")
    public ResponseEntity<Post> criarComCep(@RequestBody CriarPostComCepRequest request) {
        // Service chama client que chama API externa
        Post post = service.criarPostComCep(request);
        return ResponseEntity.status(201).body(post);
    }
}
```

## Conexão com Spring Boot

Spring oferece dois clientes HTTP:

| Cliente | Tipo | Uso |
|---|---|---|
| **RestTemplate** | Síncrono (bloqueante) | APIs tradicionais, fácil de usar |
| **WebClient** | Assíncrono (não-bloqueante) | APIs reativas, alta concorrência |
| **Feign** | Declarativo (anotações) | Mais legível, menos código |

No pom.xml:
```xml
<!-- RestTemplate: já vem com spring-web -->
<!-- Mas se quiser WebClient: -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Feign (alternativa mais elegante) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>4.0.0</version>
</dependency>
```

No `application.properties`:
```properties
# Timeout para requisições externas
spring.mvc.async.request-timeout=10000

# Se usar Feign
feign.client.config.default.connectTimeout=5000
feign.client.config.default.readTimeout=10000
```

## Resumo em tópicos

- **API Externa** = Outro servidor que você chama para pegar/enviar dados
- **RestTemplate** = Cliente HTTP síncrono (simples, mas bloqueia thread)
- **WebClient** = Cliente HTTP assíncrono (moderno, não bloqueia)
- **Feign** = Cliente HTTP declarativo (anotações, mais elegante)
- **Timeout** = Sempre defina prazo (não espere infinito)
- **Tratamento de erro** = API externa pode estar down, sempre trate exceções
- **Mapping** = Converta JSON da API externa em classe Java (DTOs)

## Dúvidas frequentes (FAQ)

**P: RestTemplate vs WebClient: qual usar?**  
R: **RestTemplate** se API é simples e síncrono é ok. **WebClient** se precisa alta concorrência ou integrar com código reativo.

**P: E se a API externa está lenta?**  
R: Use **timeout** pequeno (5-10s) para falhar rápido. Considere **cache** ou **fallback** (retornar valor padrão se API falhar).

**P: Como tratar quando API externa retorna erro?**  
R: Capture `HttpClientErrorException`, `HttpServerErrorException` em try-catch. Retorne erro amigável ao cliente (não exponha problema da API externa).

