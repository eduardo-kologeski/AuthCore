# AuthCore

AuthCore e uma API REST para cadastro de usuarios, login com JWT e suporte a refresh tokens com opcao "Permanecer conectado". O projeto tambem inclui uma pagina web simples para testar cadastro, login, token de acesso, refresh token e logout.

## Funcionalidades

- Cadastro de usuarios
- Login com access token JWT
- Suporte a "Permanecer conectado" usando refresh token
- Rotacao obrigatoria de refresh token
- Logout com revogacao do refresh token
- Senhas criptografadas com BCrypt
- Refresh tokens armazenados apenas como hash no banco
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
|   |-- LogoutRequest.java
|   |-- RefreshTokenRequest.java
|   `-- RegisterRequest.java
|-- security
|   |-- JwtAuthenticationFilter.java
|   |-- JwtService.java
|   |-- RefreshToken.java
|   |-- RefreshTokenRepository.java
|   |-- RefreshTokenService.java
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

A seguranca fica entre a requisicao e os controllers protegidos. O filtro JWT intercepta as requisicoes, valida o access token, carrega o usuario autenticado e registra a autenticacao no contexto do Spring Security.

## Access Token + Refresh Token

O AuthCore usa dois tipos de token:

| Token | Tipo | Duracao | Armazenamento no servidor | Uso |
| --- | --- | --- | --- | --- |
| Access token | JWT | 15 minutos | Nao armazenado | Acessar rotas protegidas |
| Refresh token | Valor aleatorio seguro | 7 dias | Apenas hash SHA-256 | Gerar novo access token |

O refresh token nao e JWT. Ele e um valor aleatorio gerado com `SecureRandom`, enviado ao cliente apenas uma vez e salvo no banco somente como hash. Isso evita que o valor original fique persistido no servidor.

## Remember Me

O campo `rememberMe` controla se o usuario recebera refresh token:

- `rememberMe=false`: retorna apenas `accessToken`. Quando o JWT expirar, o usuario precisa fazer login novamente.
- `rememberMe=true`: retorna `accessToken`, `refreshToken` e `expiresIn`. Quando o access token expirar, o cliente pode usar o refresh token para renovar a sessao.

Fluxo:

```text
Login
  |
  v
Access Token
  |
  v
Expira
  |
  v
Refresh Token
  |
  v
Novo Access Token + Novo Refresh Token
```

## Rotacao De Refresh Token

Sempre que `/api/auth/refresh` e chamado com um refresh token valido:

1. Um novo access token e gerado.
2. Um novo refresh token e gerado.
3. O refresh token anterior e revogado.
4. O refresh token anterior nao pode ser reutilizado.

Essa rotacao reduz o impacto caso um refresh token antigo seja exposto.

## Principais Classes

| Classe | Responsabilidade |
| --- | --- |
| `JwtApiApplication` | Classe principal que inicia a aplicacao Spring Boot. |
| `AuthController` | Expoe os endpoints publicos de cadastro, login, refresh e logout. |
| `AuthService` | Contem as regras de cadastro, login, refresh, logout e emissao de respostas. |
| `RegisterRequest` | DTO com os dados recebidos no cadastro. |
| `LoginRequest` | DTO com email, senha e `rememberMe`. |
| `RefreshTokenRequest` | DTO usado para renovar tokens. |
| `LogoutRequest` | DTO usado para revogar refresh token no logout. |
| `AuthResponse` | DTO retornado apos login, cadastro ou refresh. |
| `User` | Entidade JPA que representa a tabela de usuarios. |
| `UserRepository` | Interface de acesso ao banco com Spring Data JPA. |
| `UserController` | Expoe endpoints protegidos relacionados ao usuario autenticado. |
| `UserPrincipal` | Adapta a entidade `User` para o modelo usado pelo Spring Security. |
| `UserResponse` | DTO usado para retornar dados publicos do usuario. |
| `JwtService` | Gera, interpreta e valida apenas access tokens JWT. |
| `RefreshToken` | Entidade JPA do refresh token persistido como hash. |
| `RefreshTokenRepository` | Interface de acesso aos refresh tokens. |
| `RefreshTokenService` | Gera, valida, revoga e rotaciona refresh tokens. |
| `JwtAuthenticationFilter` | Le o header `Authorization`, valida o JWT e autentica a requisicao. |
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
  "accessToken": "jwt-token"
}
```

### Login Sem Remember Me

```http
POST /api/auth/login
Content-Type: application/json
```

Request:

```json
{
  "email": "eduardo@example.com",
  "password": "123456",
  "rememberMe": false
}
```

Response:

```json
{
  "accessToken": "jwt-token"
}
```

### Login Com Remember Me

```http
POST /api/auth/login
Content-Type: application/json
```

Request:

```json
{
  "email": "eduardo@example.com",
  "password": "123456",
  "rememberMe": true
}
```

Response:

```json
{
  "accessToken": "jwt-token",
  "refreshToken": "refresh-token",
  "expiresIn": 900
}
```

### Refresh

```http
POST /api/auth/refresh
Content-Type: application/json
```

Request:

```json
{
  "refreshToken": "refresh-token"
}
```

Response:

```json
{
  "accessToken": "new-jwt-token",
  "refreshToken": "new-refresh-token",
  "expiresIn": 900
}
```

### Logout

```http
POST /api/auth/logout
Content-Type: application/json
```

Request:

```json
{
  "refreshToken": "refresh-token"
}
```

Response:

```text
204 No Content
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
  "timestamp": "2026-06-12T03:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Refresh token invalido."
}
```

Status comuns:

| Status | Significado |
| --- | --- |
| `400` | Dados invalidos na requisicao. |
| `401` | Credenciais invalidas ou token ausente, expirado, revogado ou inexistente. |
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
app.jwt.expiration=900000
app.refresh-token.expiration=604800000
```

O segredo JWT esta configurado no `application.properties` apenas para desenvolvimento local. Em producao, use variaveis de ambiente ou um gerenciador de segredos.

## Exemplos Com curl

Cadastro:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Eduardo Kologeski","email":"eduardo@example.com","password":"123456"}'
```

Login sem rememberMe:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"eduardo@example.com","password":"123456","rememberMe":false}'
```

Login com rememberMe:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"eduardo@example.com","password":"123456","rememberMe":true}'
```

Refresh:

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}'
```

Logout:

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}'
```

Acessar rota protegida:

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Boas Praticas De Seguranca

- Senhas sao armazenadas com hash BCrypt.
- A API nao usa sessao no servidor.
- Rotas protegidas exigem JWT valido.
- Refresh tokens sao aleatorios, longos e nao carregam dados sensiveis.
- Refresh tokens sao persistidos apenas como hash.
- Refresh tokens revogados ou expirados retornam `401`.
- Refresh tokens sao rotacionados obrigatoriamente.
- O segredo JWT nao deve ser versionado em projetos de producao.
- Use HTTPS em ambientes de producao.

## Roadmap

- Refresh token por dispositivo
- Revogacao distribuida com Redis
- Blacklist distribuida para access tokens em cenarios especificos
- Deteccao de reutilizacao de refresh token
- Limite de sessoes ativas por usuario
- Profile com PostgreSQL
- Swagger/OpenAPI
- Docker e Docker Compose

## Licenca

Este projeto ainda nao define uma licenca.
