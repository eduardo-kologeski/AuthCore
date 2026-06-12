# Roles E Permissoes

Este documento descreve o estado atual e a evolucao planejada de roles no AuthCore.

## Estado Atual

Roles e permissoes ainda nao estao implementados no codigo.

Atualmente:

- A entidade `User` nao possui campo de role.
- `UserPrincipal#getAuthorities()` retorna uma lista vazia.
- `SecurityConfig` diferencia apenas rotas publicas e rotas autenticadas.
- Nao existem endpoints protegidos por permissao especifica.

## Modelo Atual De Autorizacao

O modelo atual e binario:

| Tipo de rota | Regra |
| --- | --- |
| Publica | Pode ser acessada sem token. |
| Protegida | Exige access token JWT valido. |

Rotas publicas atuais:

- `/`
- `/index.html`
- `/styles.css`
- `/app.js`
- `/api/auth/**`
- `/h2-console/**`

Demais rotas exigem autenticacao.

## Evolucao Planejada

Uma evolucao natural do AuthCore e adicionar roles como:

- `USER`
- `ADMIN`

Possivel extensao da entidade `User`:

```java
@Enumerated(EnumType.STRING)
private Role role;
```

Possivel enum:

```java
public enum Role {
    USER,
    ADMIN
}
```

Possivel ajuste em `UserPrincipal`:

```java
return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
```

## Autorizacao Por Endpoint

Com roles implementadas, endpoints poderiam usar regras como:

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

Ou anotacoes:

```java
@PreAuthorize("hasRole('ADMIN')")
```

## Cuidados Recomendados

- Definir roles no backend, nunca confiar em valor enviado pelo cliente.
- Persistir roles em banco de dados.
- Incluir roles no access token apenas se necessario.
- Validar alteracoes de roles em operacoes administrativas.
- Considerar escopos/permissoes granulares se a aplicacao crescer.

## Status

| Item | Estado |
| --- | --- |
| Entidade `Role` | Planejado |
| Campo role em `User` | Planejado |
| Authorities no `UserPrincipal` | Planejado |
| Endpoints por perfil | Planejado |
| Testes de autorizacao | Planejado |
