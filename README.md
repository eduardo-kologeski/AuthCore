# AuthCore

AuthCore e uma API REST de autenticacao reutilizavel baseada em Java, Spring Boot, Spring Security, JWT, refresh token e banco de dados relacional.

O projeto oferece cadastro de usuarios, login, emissao de access token JWT, refresh token opcional com `rememberMe`, rotacao de refresh token, logout com revogacao e uma pagina web simples para testes manuais.

## Sumario

- [Visao geral](#visao-geral)
- [Tecnologias](#tecnologias)
- [Funcionalidades atuais](#funcionalidades-atuais)
- [Estrutura da documentacao](#estrutura-da-documentacao)
- [Como executar](#como-executar)
- [Endpoints principais](#endpoints-principais)
- [Testes](#testes)
- [Estado das funcionalidades](#estado-das-funcionalidades)

## Visao Geral

AuthCore segue uma arquitetura em camadas:

```text
Controller -> Service -> Repository -> Banco de dados
```

A camada de seguranca usa Spring Security em modo stateless. O access token JWT autentica chamadas para endpoints protegidos, enquanto o refresh token permite renovar a sessao quando o login e feito com `rememberMe=true`.

## Tecnologias

- Java 25
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Hibernate
- H2 Database
- PostgreSQL
- Flyway
- JJWT 0.13.0
- Maven
- JUnit e MockMvc

## Funcionalidades Atuais

- Cadastro de usuarios
- Login com access token JWT
- Login com `rememberMe`
- Refresh token aleatorio, nao JWT
- Armazenamento de refresh token apenas como hash
- Rotacao obrigatoria de refresh token
- Logout com revogacao de refresh token
- Roles `USER` e `ADMIN`
- Protecao de endpoints administrativos por role
- Bootstrap seguro do primeiro administrador
- Gestao administrativa de usuarios
- Profile PostgreSQL com credenciais por variaveis de ambiente
- Migrations versionadas com Flyway
- Metadados de refresh token por dispositivo/sessao
- Limpeza agendada de refresh tokens expirados ou revogados antigos
- Endpoint protegido para dados do usuario autenticado
- Tratamento padronizado de erros
- Testes de integracao para fluxos de autenticacao

## Estrutura Da Documentacao

- [Arquitetura](docs/arquitetura.md)
- [Autenticacao](docs/autenticacao.md)
- [Refresh Token](docs/refresh-token.md)
- [Roles](docs/roles.md)
- [Banco de Dados](docs/banco-de-dados.md)
- [Testes](docs/testes.md)
- Diagramas:
  - [Arquitetura em camadas](docs/diagramas/arquitetura.png)
  - [Fluxo de login](docs/diagramas/fluxo-login.png)

## Como Executar Com H2

Clone o repositorio:

```bash
git clone https://github.com/eduardo-kologeski/AuthCore.git
cd AuthCore
```

Execute a aplicacao:

```bash
mvn spring-boot:run
```

A aplicacao ficara disponivel em:

```text
http://localhost:8080
```

A pagina web local fica em:

```text
http://localhost:8080/
```

O profile padrao usa H2 em memoria, indicado para desenvolvimento local e testes rapidos.

## Como Executar Com PostgreSQL

Crie um banco PostgreSQL para o projeto e informe as credenciais por variaveis de ambiente.

PowerShell:

```powershell
$env:POSTGRES_URL="jdbc:postgresql://localhost:5432/authcore"
$env:POSTGRES_USER="authcore"
$env:POSTGRES_PASSWORD="authcore"
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Bash:

```bash
POSTGRES_URL=jdbc:postgresql://localhost:5432/authcore \
POSTGRES_USER=authcore \
POSTGRES_PASSWORD=authcore \
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

No profile `postgres`, o Hibernate valida o schema e o Flyway cria/atualiza as tabelas por migrations versionadas em `src/main/resources/db/migration`.

## Endpoints Principais

| Metodo | Endpoint | Descricao | Autenticacao |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Cadastra usuario e retorna access token | Publico |
| `POST` | `/api/auth/login` | Autentica usuario | Publico |
| `POST` | `/api/auth/refresh` | Rotaciona refresh token e emite novo access token | Publico com refresh token |
| `POST` | `/api/auth/logout` | Revoga refresh token | Publico com refresh token |
| `GET` | `/api/users/me` | Retorna usuario autenticado | Bearer access token |
| `POST` | `/api/admin/bootstrap` | Cria o primeiro administrador | Bootstrap token |
| `GET` | `/api/admin/users` | Lista usuarios | ADMIN |
| `PATCH` | `/api/admin/users/{userId}/role` | Altera role de usuario | ADMIN |

## Testes

Execute:

```bash
mvn test
```

A suite atual cobre login com e sem `rememberMe`, refresh valido, refresh expirado, refresh revogado, logout, rotacao de refresh token, persistencia segura do hash, metadados de sessao, limpeza de tokens antigos, autenticacao de endpoint protegido com access token, bootstrap de administrador e autorizacao por roles.

## Estado Das Funcionalidades

| Funcionalidade | Estado |
| --- | --- |
| Cadastro de usuarios | Implementado |
| Login com JWT | Implementado |
| Refresh token | Implementado |
| Rotacao de refresh token | Implementado |
| Logout com revogacao | Implementado |
| Roles e permissoes | Implementado |
| Bootstrap de administrador | Implementado |
| Gestao administrativa de usuarios | Implementado |
| Banco relacional externo PostgreSQL | Implementado |
| Migrations Flyway | Implementado |
| Limpeza agendada de refresh tokens | Implementado |
| Swagger/OpenAPI | Planejado / em evolucao |
| Docker | Planejado / em evolucao |

## Observacao Sobre Diagramas

Os arquivos PNG em `docs/diagramas` representam os fluxos principais do projeto. Os diagramas tambem estao descritos em Mermaid dentro dos documentos tecnicos para permitir futuras exportacoes e manutencao simples.
