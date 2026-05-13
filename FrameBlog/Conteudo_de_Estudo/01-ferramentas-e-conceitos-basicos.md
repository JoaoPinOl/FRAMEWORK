# 01 - Ferramentas e Conceitos Básicos

## O que é

Ferramentas e conceitos básicos são o alicerce para construir APIs com Spring Boot. Estamos falando de:
- **Spring Boot**: um framework que simplifica a criação de aplicações Java
- **Maven**: gerenciador de dependências e construtor de projetos
- **Git**: controle de versão do código
- **HTTP**: protocolo de comunicação da web (GET, POST, PUT, DELETE)
- **REST**: padrão arquitetural para APIs

Pense em ferramentas como a caixa de ferramentas de um pedreiro, e conceitos como as técnicas de construção. Você precisa conhecer ambas para começar a trabalhar.

## Por que existe / Para que serve

Antes do Spring Boot e das IDEs modernas, criar uma API Java era extremamente tedioso:
- Precisava configurar tudo manualmente (dependências, beans, servlets)
- Havia muito código repetitivo ("boilerplate")
- Os erros eram criptografados e difíceis de entender
- Cada projeto começava do zero

Spring Boot veio para **tirar a complexidade inicial e deixar você focar na lógica de negócio** do seu projeto. Maven cuida de baixar as dependências corretas automaticamente. Git garante que seu código não se perca e que múltiplas pessoas possam trabalhar juntas.

## Como funciona no projeto

```
Seu projeto FrameBlog/
├── pom.xml                          ← Maven: "receita" do projeto (dependências)
├── .gitignore                       ← Git: o que NÃO versionar
├── src/main/java/...               ← Código-fonte (onde tudo acontece)
├── src/main/resources/
│   └── application.properties       ← Configurações iniciais
└── target/                          ← Artefatos compilados (ignorado pelo Git)
```

- **pom.xml**: Define que versão do Spring Boot você está usando, quais bibliotecas precisa (exemplo: Spring Data JPA para banco de dados)
- **application.properties**: Configura porta (8080), nome da app, dados de conexão com banco
- **src/main/java**: Suas classes Java organizadas em pacotes por responsabilidade
- **Git + .gitignore**: Versionam o código, mas ignoram `target/` (gerado automaticamente)

## Exemplo prático comentado

`pom.xml` (trecho importante):
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.descomplica</groupId>
    <artifactId>frameblog</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>FrameBlog</name>
    
    <!-- Qual versão do Spring Boot usar -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.5</version>
    </parent>
    
    <!-- Dependências que o Maven vai baixar automaticamente -->
    <dependencies>
        <!-- Spring Web: para criar endpoints HTTP -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Data JPA: para fácil acesso ao banco -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
    </dependencies>
</project>
```

`application.properties`:
```properties
# Nome da aplicação (aparece nos logs)
spring.application.name=frameblog

# Porta que o servidor rodará (acessa em http://localhost:8080)
server.port=8080

# Banco de dados (MySQL, PostgreSQL, etc)
spring.datasource.url=jdbc:mysql://localhost:3306/frameblog
spring.datasource.username=root
spring.datasource.password=senha
```

`src/main/java/com/descomplica/frameblog/FrameBlogApplication.java`:
```java
package com.descomplica.frameblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Esta anotação mágica diz pro Spring Boot: "Configure tudo automaticamente!"
@SpringBootApplication
public class FrameBlogApplication {

    public static void main(String[] args) {
        // Inicia a aplicação: cria o servidor, conecta ao banco, etc
        SpringApplication.run(FrameBlogApplication.class, args);
    }
}
```

## Conexão com Spring Boot

Spring Boot é um "facilitador":

| Conceito | Sem Spring Boot | Com Spring Boot |
|----------|---|---|
| **Dependências** | Baixar manualmente, resolveconflitos | Maven cuida sozinho |
| **Servidor Web** | Configurar Tomcat/Jetty manualmente | Já vem embarcado |
| **Configurações** | Criardezenas de arquivos XML | Um `application.properties` ou `application.yml` |
| **Beans** | Declarar manualmente | `@SpringBootApplication` + anotações resolvem |

A anotação `@SpringBootApplication` na classe principal faz:
1. Escaneia automaticamente todas as classes com `@Component`, `@Service`, `@Controller`
2. Liga-as como "beans" (objetos gerenciados pelo Spring)
3. Injeta dependências automaticamente onde necessário
4. Configura segurança, exceções, logging

## Resumo em tópicos

- **Spring Boot** = Framework Java que tira a complexidade de montar uma aplicação
- **Maven** = Gestor de dependências (evita conflitos de versão e downloads manuais)
- **Git** = Controle de versão (histórico, colaboração, segurança do código)
- **pom.xml** = "Receita" do projeto: versão do Spring, bibliotecas necessárias
- **application.properties** = Local onde você personaliza a aplicação (porta, banco, timeouts)
- **@SpringBootApplication** = Anotação mágica que configura tudo automaticamente
- **target/** = Pasta gerada com código compilado (nunca editar, sempre regenerada)

## Dúvidas frequentes (FAQ)

**P: Por que existe a pasta `target/`? Posso deletá-la sem problema?**  
R: Sim! É gerada automaticamente quando você "compila" (constrói) o projeto. Maven regenera sozinha quando necessário. Coloque no `.gitignore` para não ocupar espaço no repositório.

**P: Qual é a diferença entre Maven e Gradle?**  
R: Ambos são "gerenciadores de projeto", mas Maven é mais estabelecido e usa XML (mais simples). Gradle é mais flexível mas com sintaxe mais complexa. Para este curso, Maven é suficiente.

**P: O que significa "SNAPSHOT" na versão (0.0.1-SNAPSHOT)?**  
R: Significa que é uma versão em desenvolvimento, não é "oficial" ainda. Quando liberar uma versão real, muda para 0.0.1 (sem SNAPSHOT). É como dizer "trabalho em progresso".

