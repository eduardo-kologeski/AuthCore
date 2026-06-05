# AuthCore

AuthCore e uma API REST para cadastro de usuarios e login com autenticacao JWT. O projeto tambem inclui uma pagina web simples para testar cadastro, login, armazenamento do token e acesso a uma rota protegida.

## Funcionalidades

- Cadastro de usuarios
- Login com geracao de JWT
- Senhas criptografadas com BCrypt
- Endpoint protegido com Bearer token
- Configuracao stateless com Spring Security
- Validacao de entrada com Jakarta Bean Validation
- Respostas de erro padronizadas
- Banco H2 em memoria para desenvolvimento local
- Interface web simples para testes manuais

## Tecnologias

- Java 25
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Hibernate
- H2 Database
- JJWT 0.13.0
- Maven

## Estrutura Do Projeto

```text
src/main/java/com/example/jwtapi
|-- JwtApiApplication.java
|-- api
|   `-- ApiExceptionHandler.java
|-- auth
|   |-- AuthController.java
|   |-- AuthResponse.java
|   |-- AuthService.java
|   |-- LoginRequest.java
|   `-- RegisterRequest.java
|-- security
|   |-- JwtAuthenticationFilter.java
|   |-- JwtService.java
|   `-- SecurityConfig.java
`-- user
    |-- User.java
    |-- UserController.java
    |-- UserPrincipal.java
    |-- UserRepository.java
    `-- UserResponse.java
```

## Arquitetura

O projeto usa uma arquitetura em camadas:

```text
Pagina Web / Cliente API
        |
        v
Controller
        |
        v
Service
        |
        v
Repository
        |
        v
Banco de Dados
```

A seguranca fica entre a requisicao e os controllers protegidos. O filtro JWT intercepta as requisicoes, valida o token, carrega o usuario autenticado e registra a autenticacao no contexto do Spring Security.

## Principais Classes

| Classe | Responsabilidade |
| --- | --- |
| `JwtApiApplication` | Classe principal que inicia a aplicacao Spring Boot. |
| `AuthController` | Expoe os endpoints publicos de cadastro e login. |
| `AuthService` | Contem as regras de cadastro, login, criptografia de senha e geracao de token. |
| `RegisterRequest` | DTO com os dados recebidos no cadastro. |
| `LoginRequest` | DTO com os dados recebidos no login. |
| `AuthResponse` | DTO retornado apos cadastro ou login, incluindo o JWT. |
| `User` | Entidade JPA que representa a tabela de usuarios. |
| `UserRepository` | Interface de acesso ao banco com Spring Data JPA. |
| `UserController` | Expoe endpoints protegidos relacionados ao usuario autenticado. |
| `UserPrincipal` | Adapta a entidade `User` para o modelo usado pelo Spring Security. |
| `UserResponse` | DTO usado para retornar dados publicos do usuario. |
| `JwtService` | Gera, interpreta e valida tokens JWT. |
| `JwtAuthenticationFilter` | Le o header `Authorization`, valida o token e autentica a requisicao. |
| `SecurityConfig` | Define as regras de seguranca, rotas publicas e rotas protegidas. |
| `ApiExceptionHandler` | Padroniza respostas de erro da API. |

## Endpoints

### Cadastro

```http
POST /api/auth/register
Content-Type: application/json
```

Request:

```json
{
  "name": "Eduardo Kologeski",
  "email": "eduardo@example.com",
  "password": "123456"
}
```

Response:

```json
{
  "token": "jwt-token",
  "id": 1,
  "name": "Eduardo Kologeski",
  "email": "eduardo@example.com"
}
```

### Login

```http
POST /api/auth/login
Content-Type: application/json
```

Request:

```json
{
  "email": "eduardo@example.com",
  "password": "123456"
}
```

Response:

```json
{
  "token": "jwt-token",
  "id": 1,
  "name": "Eduardo Kologeski",
  "email": "eduardo@example.com"
}
```

### Usuario Autenticado

```http
GET /api/users/me
Authorization: Bearer jwt-token
```

Response:

```json
{
  "id": 1,
  "name": "Eduardo Kologeski",
  "email": "eduardo@example.com"
}
```

## Respostas De Erro

A API retorna erros em formato padronizado:

```json
{
  "timestamp": "2026-06-05T03:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciais invalidas."
}
```

Status comuns:

| Status | Significado |
| --- | --- |
| `400` | Dados invalidos na requisicao. |
| `401` | Credenciais invalidas ou token ausente/invalido. |
| `409` | E-mail ja cadastrado. |

## Requisitos

- Java 25 ou superior
- Maven 3.9 ou superior

## Como Executar Localmente

Clone o repositorio:

```bash
git clone https://github.com/eduardo-kologeski/AuthCore.git
cd AuthCore
```

Execute a aplicacao:

```bash
mvn spring-boot:run
```

A API ficara disponivel em:

```text
http://localhost:8080
```

A pagina web de teste fica em:

```text
http://localhost:8080/
```

## Build

Gere o arquivo JAR executavel:

```bash
mvn clean package
```

Execute o JAR:

```bash
java -jar target/authcore-0.0.1-SNAPSHOT.jar
```

## Testes

Execute a suite de testes:

```bash
mvn test
```

## Banco De Dados

O projeto usa H2 em memoria para desenvolvimento local.

Console H2:

```text
http://localhost:8080/h2-console
```

Configuracao padrao:

```text
JDBC URL: jdbc:h2:mem:jwt_api
Username: sa
Password:
```

## Configuracao

As configuracoes principais ficam em:

```text
src/main/resources/application.properties
```

Configuracoes atuais:

```properties
server.port=8080
spring.datasource.url=jdbc:h2:mem:jwt_api;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.jpa.hibernate.ddl-auto=update
app.jwt.expiration=2h
```

O segredo JWT esta configurado no `application.properties` apenas para desenvolvimento local. Em producao, use variaveis de ambiente ou um gerenciador de segredos.

## Fluxo De Autenticacao

```text
1. O usuario faz cadastro ou login.
2. A API valida os dados recebidos.
3. A senha e salva ou comparada usando BCrypt.
4. O AuthCore gera um JWT assinado.
5. O cliente envia o token no header Authorization.
6. O filtro JWT valida o token nas rotas protegidas.
7. O Spring Security libera o acesso ao endpoint autenticado.
```

## Exemplos Com curl

Cadastro:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Eduardo Kologeski","email":"eduardo@example.com","password":"123456"}'
```

Login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"eduardo@example.com","password":"123456"}'
```

Acessar rota protegida:

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Boas Praticas De Seguranca

- Senhas sao armazenadas com hash BCrypt.
- A API nao usa sessao no servidor.
- Rotas protegidas exigem JWT valido.
- O segredo JWT nao deve ser versionado em projetos de producao.
- Use HTTPS em ambientes de producao.
- O token expira em `2h`.

## Roadmap

- Adicionar refresh tokens
- Adicionar roles e permissoes
- Criar profile com PostgreSQL
- Adicionar testes de integracao dos endpoints de autenticacao
- Adicionar documentacao Swagger/OpenAPI
- Adicionar suporte a Docker

## Licenca

Este projeto ainda nao define uma licenca.
