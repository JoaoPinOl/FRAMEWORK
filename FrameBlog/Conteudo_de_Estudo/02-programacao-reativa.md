# 02 - Programação Reativa

## O que é

Programação reativa é um paradigma que trata seu código como **fluxos de dados que respondem a mudanças**, em vez de executar passo a passo como tradicionalmente fazemos.

Imagine um escritório antigo:
- **Forma tradicional (imperativa)**: "Ana, me traga o arquivo então, espero; depois você faz uma cópia; depois me entrega"
- **Forma reativa**: "Quando o cliente ligar, busque automaticamente o arquivo, copie, e entregue"

Em programação reativa, você não espera bloqueado. Em vez disso, você **reage aos eventos** (dados chegando, erro ocorrendo, conexão encerrada).

## Por que existe / Para que serve

Aplicações web modernas precisam:
- **Lidar com múltiplas requisições simultâneas** sem travar
- **Usar menos memória** (não alocar thread por requisição)
- **Responder rápido** mesmo com operações lentas (banco de dados, APIs externas)

Metáfora: Um garçom de restaurante:
- **Tradicional (bloqueante)**: Atende um cliente, fica esperando o cozinheiro terminar o prato dele, só depois atende o próximo
- **Reativo**: Anota o pedido, segue para o próximo cliente, quando o prato fica pronto, ele entrega

Reatividade economiza threads e permite que **uma única thread** atenda centenas de requisições.

## Como funciona no projeto

Em um projeto reativo com Spring Boot:

```
Seu projeto FrameBlog/
├── pom.xml                     ← spring-boot-starter-webflux (não é web!)
├── src/main/java/.../
│   ├── controllers/             ← Retornam Mono<T> ou Flux<T> em vez de T
│   ├── services/                ← Processam dados reativamente
│   ├── repository/              ← Acessam banco de forma não-bloqueante
│   └── clients/                 ← Chamam APIs externas sem bloquear
└── application.properties       ← Configuração do servidor reativo
```

- **Mono<T>**: Um "stream" que emite 0 ou 1 valor (como um Optional)
- **Flux<T>**: Um "stream" que emite 0, 1 ou vários valores (como uma List)
- Operações são **lazy** (não executam até alguém "se inscrever")

## Exemplo prático comentado

Comparação: **Antes (Springs Web tradicional)**

```java
@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService service;

    @GetMapping
    public List<Post> buscarTodos() {
        // BLOQUEANTE: espera o banco terminar
        List<Post> posts = service.buscarTodos();
        return posts;
    }
}
```

**Depois (Spring WebFlux reativo)**

```java
@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService service;

    @GetMapping
    public Flux<Post> buscarTodos() {
        // NÃO-BLOQUEANTE: retorna logo, dados chegam quando prontos
        return service.buscarTodosReativo()
                // Transforma cada post antes de enviar
                .map(post -> {
                    post.setDataConsulta(LocalDateTime.now());
                    return post;
                })
                // Se ocorrer erro, retorna lista vazia
                .onErrorResume(e -> {
                    System.out.println("Erro ao buscar: " + e.getMessage());
                    return Flux.empty();
                });
    }
}
```

Service reativo:

```java
@Service
public class PostService {

    @Autowired
    private PostRepository repository;

    // Retorna um Flux: múltiplos posts vindindo "aos poucos"
    public Flux<Post> buscarTodosReativo() {
        return repository.findAllReative()
                // Filtra: só posts com mais de 10 comentários
                .filter(post -> post.getComentarios().size() > 10)
                // Processa em lotes de 5
                .buffer(5)
                // Faz algo com cada lote
                .flatMap(lote -> processarLote(lote));
    }

    private Mono<Post> processarLote(List<Post> lote) {
        // Faz algo complexo, mas não bloqueia
        return Mono.fromCallable(() -> {
            lote.forEach(post -> post.incrementarVisualizacoes());
            return salvarTodos(lote);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
```

Base de dados (usando R2DBC - driver reativo):

```java
@Repository
public interface PostRepository extends ReactiveCrudRepository<Post, Long> {
    // Retorna Flux, não List
    Flux<Post> findByAutor(String autor);
    
    // Retorna Mono, não um único Post
    Mono<Post> findById(Long id);
}
```

## Conexão com Spring Boot

Spring Boot oferece **WebFlux** para reatividade:

| Aspecto | Spring Web | Spring WebFlux |
|---|---|---|
| **Starter** | spring-boot-starter-web | spring-boot-starter-webflux |
| **Servidor** | Tomcat (thread por requisição) | Netty (event loop) |
| **Retorno Controller** | `List<T>`, `T`, `String` | `Mono<T>`, `Flux<T>` |
| **BD** | JDBC (bloqueante) | R2DBC (não-bloqueante) |
| **Melhor para** | Apps tradicionais | APIs de alta concorrência |

No `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Driver reativo para MySQL -->
<dependency>
    <groupId>dev.miku</groupId>
    <artifactId>r2dbc-mysql</artifactId>
</dependency>
```

No `application.properties`:
```properties
# Configuração reativa do banco (R2DBC, não JDBC)
spring.r2dbc.url=r2dbc:mysql://localhost:3306/frameblog
spring.r2dbc.username=root
spring.r2dbc.password=senha

# Número de handlers (threads virtuais) processando requisições
server.netty.threads.io=100
```

## Resumo em tópicos

- **Reatividade** = Código que responde a eventos em vez de bloquear esperando
- **Mono<T>** = Stream que emite 0 ou 1 valor (tipo empacotado para não-bloqueante)
- **Flux<T>** = Stream que emite 0, muitos ou infinitos valores
- **WebFlux** = Framework Spring para APIs reativas (substitui Spring Web)
- **Netty** = Servidor web reativo (mais eficiente em concorrência que Tomcat)
- **R2DBC** = Driver de banco de dados não-bloqueante (tipo "JDBC reativo")
- **Lazy evaluation** = Código reativo só executa quando alguém "se inscreve" (.subscribe() ou requisição HTTP)

## Dúvidas frequentes (FAQ)

**P: Reatividade sempre é melhor que tradicional?**  
R: Não. Reatividade é ótima para APIs com milhares de requisições simultâneas. Para aplicações internas ou com poucas requisições, a complexidade extra não compensa. Use reatividade quando realmente precisar.

**P: Qual é a diferença entre Mono e Flux?**  
R: **Mono** = 0 ou 1 resultado (tipo `Optional`). **Flux** = 0 a muitos resultados (tipo `Stream` ou `List`). Se seu banco retorna um único usuário, use Mono. Se busca vários, use Flux.

**P: Preciso usar R2DBC com MongoDB ou só com SQL?**  
R: R2DBC é para bancos SQL. MongoDB já funciona de forma natural reativa no Spring Data. Verifique a documentação do seu banco para driver reativo.

