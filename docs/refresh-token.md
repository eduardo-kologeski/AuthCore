# Refresh Token

Este documento descreve a implementacao atual de refresh token no AuthCore.

## Objetivo

Permitir que o usuario mantenha a sessao ativa por mais tempo quando selecionar `rememberMe=true`, sem aumentar a duracao do access token JWT.

## Implementacao Atual

O refresh token:

- Nao e JWT.
- E gerado com `SecureRandom`.
- Usa 64 bytes aleatorios.
- E codificado em Base64 URL-safe sem padding.
- E salvo no banco apenas como hash SHA-256.
- Possui data de criacao.
- Possui data de expiracao.
- Pode ser revogado.
- E rotacionado obrigatoriamente a cada uso.

## Entidade

Entidade atual:

- `RefreshToken`

Campos:

| Campo | Descricao |
| --- | --- |
| `id` | Identificador do registro. |
| `user` | Usuario dono do refresh token. |
| `tokenHash` | Hash SHA-256 do token original. |
| `createdAt` | Data de criacao. |
| `expiresAt` | Data de expiracao. |
| `revoked` | Indica se o token foi revogado. |

## Configuracao

```properties
app.refresh-token.expiration=604800000
```

Isso representa 7 dias.

## Criacao

O refresh token e criado quando:

- O usuario faz login com `rememberMe=true`.

O valor original e retornado ao cliente apenas uma vez. O banco armazena somente o hash.

## Refresh

Endpoint:

```http
POST /api/auth/refresh
```

Request:

```json
{
  "refreshToken": "..."
}
```

Response:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 900
}
```

## Rotacao Obrigatoria

Quando um refresh token valido e usado:

1. O token recebido e hasheado.
2. A API busca o hash no banco.
3. A API verifica se o token existe, nao esta expirado e nao esta revogado.
4. O token antigo e revogado.
5. Um novo refresh token e gerado.
6. Um novo access token e gerado.
7. Os novos tokens sao retornados.

O refresh token anterior nao pode ser reutilizado.

## Logout

Endpoint:

```http
POST /api/auth/logout
```

Request:

```json
{
  "refreshToken": "..."
}
```

Comportamento:

- Valida o refresh token.
- Revoga o registro persistido.
- Retorna `204 No Content`.

## Erros

Retorna `401` quando o refresh token:

- Nao existe.
- Esta expirado.
- Foi revogado.
- E invalido.

## Consideracoes De Seguranca

- O valor original do refresh token nunca e salvo.
- A rotacao reduz o risco de reutilizacao indevida.
- O access token continua curto.
- A autenticacao permanece stateless para access tokens.

## Melhorias Planejadas

- Refresh token por dispositivo.
- Deteccao de reutilizacao de refresh token antigo.
- Revogacao de todos os tokens de um usuario.
- Registro de metadados como IP, user agent e device id.
- Armazenamento distribuido ou blacklist com Redis em ambientes multi-instancia.
