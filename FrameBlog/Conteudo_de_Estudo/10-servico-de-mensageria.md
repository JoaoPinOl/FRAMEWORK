# 10 - Serviço de Mensageria

## O que é

**Mensageria** é um sistema de **fila de mensagens**. Em vez de chamar diretamente (síncrono), você **coloca uma mensagem na fila** e outro processo a processa depois (assíncrono). Pense em uma caixa de correio: você escreve carta, coloca no correio, e o carteiro entrega depois.

Conceptualmente:
```
Síncrono (tradicional):      A → B (espera resposta)
Assíncrono (mensageria):     A → Fila → B (A não espera)
```

Sistemas populares: RabbitMQ, Apache Kafka, AWS SQS, Google Pub/Sub.

## Por que existe / Para que serve

Cenários do mundo real:
- **Email**: user cria post → fila → worker envia email de confirmação (não bloqueia)
- **Processamento pesado**: user faz busca complexa → entra na fila → servidor processa quando livre
- **Desacoplamento**: seu blog não precisa chamar API de pagamento diretamente, envia mensagem
- **Confiabilidade**: se servidor B cai, mensagens ficam na fila e são processadas depois

Benefícios:
- **Não bloqueia**: usuário recebe resposta rápido, trabalho pesado fica para trás
- **Escalabilidade**: múltiplos workers processando mensagens
- **Retry automático**: se falhar, tenta novamente
- **Auditoria**: registro de tudo que aconteceu

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── src/main/java/.../
│   ├── consumers/                      ← Processa mensagens
│   │   ├── EmailConsumer.java
│   │   └── NotificacaoConsumer.java
│   ├── producers/                      ← Envia mensagens (opcional)
│   │   ├── EmailProducer.java
│   ├── services/
│   │   └── PostService.java            ← Envia mensagem quando post criado
│   ├── config/
│   │   └── RabbitMqConfig.java         ← Configuração de filas
│   ├── models/
│   │   └── PostCriadoEvent.java        ← Eventos/mensagens
│   └── pom.xml                         ← spring-boot-starter-amqp (RabbitMQ)
```

**Fluxo**:
1. Cliente → POST /posts (criar post)
2. Service salva post no banco
3. Service **envia mensagem** "PostCriado" na fila
4. **Consumer** recebe mensagem
5. Consumer envia email, notificação, etc (sem bloquear usuário)

## Exemplo prático comentado

**Modelo de evento/mensagem**:
```java
package com.descomplica.frameblog.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PostCriadoEvent implements Serializable {
    private Long postId;
    private String titulo;
    private String nomeAutor;
    private String emailAutor;
    private LocalDateTime dataHora;

    public PostCriadoEvent(Long postId, String titulo, String nomeAutor, 
                           String emailAutor) {
        this.postId = postId;
        this.titulo = titulo;
        this.nomeAutor = nomeAutor;
        this.emailAutor = emailAutor;
        this.dataHora = LocalDateTime.now();
    }

    // Getters...
    public Long getPostId() { return postId; }
    public String getTitulo() { return titulo; }
    public String getNomeAutor() { return nomeAutor; }
}
```

**Configuração de fila** (RabbitMQ):
```java
package com.descomplica.frameblog.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // Nome da fila
    public static final String QUEUE_POST_CRIADO = "queue.post.criado";
    public static final String EXCHANGE_POSTS = "exchange.posts";
    public static final String ROUTING_KEY = "routing.post.criado";

    // Cria a fila (onde mensagens ficam armazenadas)
    @Bean
    public Queue postCriadoQueue() {
        return new Queue(QUEUE_POST_CRIADO, true);  // true = durável (persiste se broker cair)
    }

    // Cria o exchange (tipo: direct, topic, fanout)
    @Bean
    public DirectExchange postsExchange() {
        return new DirectExchange(EXCHANGE_POSTS, true, false);
    }

    // conecta fila ao exchange
    @Bean
    public Binding postsCriadoBinding(Queue postCriadoQueue, DirectExchange postsExchange) {
        return BindingBuilder.bind(postCriadoQueue)
                .to(postsExchange)
                .with(ROUTING_KEY);
    }
}
```

**Producer (envia mensagens)**:
```java
package com.descomplica.frameblog.services;

import com.descomplica.frameblog.config.RabbitMqConfig;
import com.descomplica.frameblog.models.Post;
import com.descomplica.frameblog.models.PostCriadoEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void criarPost(Post post) {
        // 1. Salva post no banco
        Post salvo = repository.save(post);

        // 2. Cria evento
        PostCriadoEvent evento = new PostCriadoEvent(
            salvo.getId(),
            salvo.getTitulo(),
            salvo.getAutor().getNome(),
            salvo.getAutor().getEmail()
        );

        // 3. Envia para fila (não-bloqueante)
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.EXCHANGE_POSTS,
            RabbitMqConfig.ROUTING_KEY,
            evento
        );

        // 4. Retorna logo pro cliente (não espera consumer processar)
    }
}
```

**Consumer (processa mensagens)**:
```java
package com.descomplica.frameblog.consumers;

import com.descomplica.frameblog.config.RabbitMqConfig;
import com.descomplica.frameblog.models.PostCriadoEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer {

    @Autowired
    private JavaMailSender mailSender;

    // Ouve fila: quando mensagem chega, executa
    @RabbitListener(queues = RabbitMqConfig.QUEUE_POST_CRIADO)
    public void enviarEmailPostCriado(PostCriadoEvent evento) {
        try {
            System.out.println("Processando evento: Post criado " + evento.getPostId());

            // Monta email
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(evento.getEmailAutor());
            email.setSubject("Novo post criado!");
            email.setText("Seu post '" + evento.getTitulo() + "' foi publicado com sucesso!");

            // Envia (pode demorar, não bloqueia usuário)
            mailSender.send(email);

            System.out.println("Email enviado para " + evento.getEmailAutor());
        } catch (Exception e) {
            System.out.println("Erro ao enviar email: " + e.getMessage());
            // RabbitMQ recoloca a mensagem na fila para retry
            throw new RuntimeException("Erro ao processar evento", e);
        }
    }
}
```

**Outro consumer** (notificações):
```java
package com.descomplica.frameblog.consumers;

import com.descomplica.frameblog.config.RabbitMqConfig;
import com.descomplica.frameblog.models.PostCriadoEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificacaoConsumer {

    @RabbitListener(queues = RabbitMqConfig.QUEUE_POST_CRIADO)
    public void enviarNotificacaoPush(PostCriadoEvent evento) {
        System.out.println("Enviando notificação push para seguidores do " + evento.getNomeAutor());
        // Lógica de notificação push (Firebase, etc)
    }
}
```

**Controller**:
```java
@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService service;

    @PostMapping
    public ResponseEntity<Post> criar(@RequestBody CriarPostRequest request) {
        // Service envia mensagem, não bloqueia
        Post post = service.criarPost(request);
        return ResponseEntity.status(201).body(post);
    }
}
```

## Conexão com Spring Boot

Spring Boot integra mensageria via Spring AMQP:

| Conceito | Função |
|---|---|
| **Queue** | Fila onde mensagens ficam |
| **Exchange** | Roteador (decide para qual fila mandar) |
| **RabbitTemplate** | Envia mensagens |
| **@RabbitListener** | Escuta fila e processa |
| **Direct/Topic/Fanout** | Tipos de exchange (roteamento) |

No pom.xml:
```xml
<!-- RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Kafka (alternativa) -->
<!-- <dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency> -->
```

No application.properties:
```properties
# Conexão RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Retry automático
spring.rabbitmq.listener.retry.enabled=true
spring.rabbitmq.listener.retry.max-attempts=3
spring.rabbitmq.listener.retry.initial-interval=1000
```

## Resumo em tópicos

- **Mensageria** = Sistema de filas (assíncrono, não-bloqueante)
- **Producer** = Envia mensagem na fila (seu app)
- **Consumer** = Processa mensagem (worker)
- **Queue** = Fila (armazena mensagens)
- **Exchange** = Roteador (dirija mensagens às filas certas)
- **RabbitMQ** = Sistema de mensageria popular, confiável
- **@RabbitListener** = Ouve fila e executa quando mensagem chega

## Dúvidas frequentes (FAQ)

**P: Quando usar mensageria?**  
R: Use para operações **assíncronas** (email, SMS, processamento pesado). **Não use** para operações que precisam de resposta imediata (transferência bancária precisa confirmar logo).

**P: RabbitMQ ou Kafka?**  
R: **RabbitMQ**: simples, bom para fila tradicional. **Kafka**: distribuído, stream de eventos, escalável. Para começar, use RabbitMQ.

**P: E se consumer ficar offline?**  
R: Mensagens ficam na fila (durável). Quando consumer volta, processa todas. Por isso é confiável.

