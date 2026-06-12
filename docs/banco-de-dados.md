# Banco De Dados

Este documento descreve o modelo de dados atual do AuthCore.

## Banco Atual

O projeto usa H2 em memoria para desenvolvimento local.

Configuracao:

```properties
spring.datasource.url=jdbc:h2:mem:jwt_api;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
```

Console H2:

```text
http://localhost:8080/h2-console
```

## Entidades

### User

Representa usuarios cadastrados.

Classe:

- `com.example.jwtapi.user.User`

Campos:

| Campo | Tipo | Observacao |
| --- | --- | --- |
| `id` | `Long` | Chave primaria gerada automaticamente. |
| `name` | `String` | Nome do usuario. |
| `email` | `String` | E-mail unico. |
| `password` | `String` | Hash BCrypt da senha. |
| `role` | `Role` | Role do usuario: `USER` ou `ADMIN`. |

Tabela:

```text
users
```

### RefreshToken

Representa refresh tokens persistidos como hash.

Classe:

- `com.example.jwtapi.security.RefreshToken`

Campos:

| Campo | Tipo | Observacao |
| --- | --- | --- |
| `id` | `Long` | Chave primaria gerada automaticamente. |
| `user` | `User` | Relacionamento muitos-para-um com usuario. |
| `tokenHash` | `String` | Hash SHA-256 unico do refresh token. |
| `createdAt` | `Instant` | Data de criacao. |
| `expiresAt` | `Instant` | Data de expiracao. |
| `revoked` | `boolean` | Indica se o token foi revogado. |

Tabela:

```text
refresh_tokens
```

## Relacionamentos

Um usuario pode possuir varios refresh tokens.

```text
users 1 ---- N refresh_tokens
```

## Repositories

### UserRepository

Responsavel por buscar e persistir usuarios.

Metodos especificos:

- `findByEmail(String email)`
- `existsByEmail(String email)`

### RefreshTokenRepository

Responsavel por buscar e persistir refresh tokens.

Metodo especifico:

- `findByTokenHash(String tokenHash)`

## Observacoes De Seguranca

- Senhas sao armazenadas com BCrypt.
- Refresh tokens sao armazenados apenas como hash.
- O token original nunca e persistido.
- O e-mail do usuario possui restricao de unicidade.
- O hash do refresh token possui restricao de unicidade.

## Evolucao Planejada

- Profile com PostgreSQL.
- Migrations com Flyway ou Liquibase.
- Indices explicitos para `email` e `tokenHash`.
- Metadados de refresh token por dispositivo.
- Politica de limpeza de refresh tokens expirados.
