# Banco De Dados

Este documento descreve o modelo de dados atual do AuthCore.

## Banco Padrao

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

## Profile PostgreSQL

O projeto possui o profile `postgres` para ambiente mais proximo de producao.

Arquivo:

- `src/main/resources/application-postgres.properties`

Configuracao principal:

```properties
spring.datasource.url=${POSTGRES_URL:jdbc:postgresql://localhost:5432/authcore}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=${POSTGRES_USER:authcore}
spring.datasource.password=${POSTGRES_PASSWORD:authcore}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.h2.console.enabled=false

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

Com esse profile, o Hibernate nao cria nem altera tabelas automaticamente. Ele apenas valida se o schema existente esta compativel com as entidades. A criacao e evolucao do schema ficam sob responsabilidade do Flyway.

Execucao com PostgreSQL:

```powershell
$env:POSTGRES_URL="jdbc:postgresql://localhost:5432/authcore"
$env:POSTGRES_USER="authcore"
$env:POSTGRES_PASSWORD="authcore"
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Migrations

As migrations ficam em:

```text
src/main/resources/db/migration
```

Migration inicial:

- `V1__create_authcore_schema.sql`

Ela cria:

- tabela `users`
- tabela `refresh_tokens`
- foreign key de refresh token para usuario
- indice unico para e-mail
- indice unico para `token_hash`
- indices auxiliares para usuario, expiracao e revogacao

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
| `deviceName` | `String` | Nome do dispositivo ou origem da sessao. |
| `userAgent` | `String` | User-Agent informado pelo cliente HTTP. |
| `ipAddress` | `String` | IP de origem, considerando `X-Forwarded-For` quando presente. |
| `createdAt` | `Instant` | Data de criacao. |
| `expiresAt` | `Instant` | Data de expiracao. |
| `lastUsedAt` | `Instant` | Ultima utilizacao do token em rotacao. |
| `revokedAt` | `Instant` | Data de revogacao. |
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
- `deleteExpiredOrRevokedBefore(Instant cutoff)`

## Indices E Constraints

| Tabela | Indice/constraint | Objetivo |
| --- | --- | --- |
| `users` | `ux_users_email` | Garante e-mail unico. |
| `refresh_tokens` | `ux_refresh_tokens_token_hash` | Garante unicidade do hash do refresh token. |
| `refresh_tokens` | `ix_refresh_tokens_user_id` | Otimiza consultas por usuario. |
| `refresh_tokens` | `ix_refresh_tokens_expires_at` | Apoia limpeza e verificacoes por expiracao. |
| `refresh_tokens` | `ix_refresh_tokens_revoked_at` | Apoia limpeza de tokens revogados antigos. |

## Politica De Limpeza

Refresh tokens expirados ou revogados antigos podem ser removidos automaticamente por um job agendado.

Properties:

```properties
app.refresh-token.cleanup.retention=2592000000
app.refresh-token.cleanup.interval=3600000
```

- `retention`: periodo de retencao antes da remocao definitiva. O valor padrao representa 30 dias.
- `interval`: intervalo entre execucoes do job. O valor padrao representa 1 hora.

## Observacoes De Seguranca

- Senhas sao armazenadas com BCrypt.
- Refresh tokens sao armazenados apenas como hash.
- O token original nunca e persistido.
- O e-mail do usuario possui restricao de unicidade.
- O hash do refresh token possui restricao de unicidade.
