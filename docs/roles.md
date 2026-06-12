# Roles E Permissoes

Este documento descreve o controle de acesso baseado em roles implementado no AuthCore.

## Objetivo

O RBAC do AuthCore permite diferenciar usuarios comuns e administradores, protegendo endpoints administrativos com Spring Security.

## Roles Disponiveis

| Role | Descricao |
| --- | --- |
| `USER` | Usuario comum criado pelo cadastro publico. |
| `ADMIN` | Usuario administrador com acesso aos endpoints `/api/admin/**`. |

## Modelo De Dominio

A role e representada pelo enum:

```java
public enum Role {
    USER,
    ADMIN
}
```

A entidade `User` possui o campo:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private Role role = Role.USER;
```

Todo usuario criado pelo cadastro publico recebe `USER` por padrao.

## Authorities

`UserPrincipal#getAuthorities()` converte a role persistida para authority do Spring Security:

```java
ROLE_USER
ROLE_ADMIN
```

Isso permite usar regras como:

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

## JWT

O access token JWT inclui a claim:

```json
{
  "role": "ADMIN"
}
```

Mesmo com a role no token, a aplicacao carrega o usuario pelo e-mail do token e monta as authorities a partir do usuario persistido.

## Regras De Acesso

| Rota | Regra |
| --- | --- |
| `/api/auth/**` | Publica |
| `/api/admin/bootstrap` | Publica com `bootstrapToken` |
| `/api/admin/**` | Exige `ADMIN` |
| Demais rotas | Exigem usuario autenticado |

## Bootstrap Do Primeiro Administrador

Endpoint:

```http
POST /api/admin/bootstrap
```

O endpoint cria o primeiro usuario `ADMIN` somente se:

- O `bootstrapToken` enviado for valido.
- Ainda nao existir nenhum usuario com role `ADMIN`.
- O e-mail informado ainda nao estiver cadastrado.

Configuracao:

```properties
app.admin.bootstrap-token=change-me-dev-bootstrap-token
```

Em producao, esse valor deve ser configurado por variavel de ambiente ou gerenciador de segredos.

## Gestao Administrativa

Endpoints atuais:

```http
GET /api/admin/users
PATCH /api/admin/users/{userId}/role
```

Apenas usuarios com role `ADMIN` podem acessar esses endpoints.

Exemplo de alteracao de role:

```json
{
  "role": "ADMIN"
}
```

## Comportamento De Erros

| Cenario | Status |
| --- | --- |
| Usuario sem token acessa rota protegida | `401` |
| Usuario `USER` acessa rota administrativa | `403` |
| Bootstrap token invalido | `401` |
| Segundo bootstrap de ADMIN | `409` |

## Testes

A suite `RbacIntegrationTests` cobre:

- Usuario publico criado como `USER`.
- Bootstrap do primeiro `ADMIN`.
- Rejeicao de bootstrap com token invalido.
- Bloqueio de segundo bootstrap.
- `USER` sem acesso a endpoints admin.
- `ADMIN` listando usuarios.
- `ADMIN` alterando role de usuario.
- Login de admin retornando access token valido.

## Melhorias Planejadas

- Permissoes granulares alem de roles.
- Auditoria de alteracao de role.
- Protecao para evitar rebaixamento acidental do ultimo administrador.
- Configuracao de bootstrap token via variavel de ambiente em profiles de producao.
