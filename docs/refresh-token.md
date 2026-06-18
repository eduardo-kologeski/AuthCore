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
- Possui metadados de dispositivo/sessao.
- Registra ultimo uso em rotacao.
- Registra data de revogacao.
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
| `deviceName` | Nome do dispositivo ou origem informada pelo cliente. |
| `userAgent` | User-Agent da requisicao. |
| `ipAddress` | IP de origem da requisicao. |
| `createdAt` | Data de criacao. |
| `expiresAt` | Data de expiracao. |
| `lastUsedAt` | Data em que o token foi usado para rotacao. |
| `revokedAt` | Data em que o token foi revogado. |
| `revoked` | Indica se o token foi revogado. |

## Configuracao

```properties
app.refresh-token.expiration=604800000
app.refresh-token.cleanup.retention=2592000000
app.refresh-token.cleanup.interval=3600000
```

- `app.refresh-token.expiration`: 7 dias.
- `app.refresh-token.cleanup.retention`: 30 dias.
- `app.refresh-token.cleanup.interval`: 1 hora.

## Criacao

O refresh token e criado quando:

- O usuario faz login com `rememberMe=true`.

O valor original e retornado ao cliente apenas uma vez. O banco armazena somente o hash.

Quando informado, o campo `deviceName` do login e salvo junto com `User-Agent` e IP da requisicao. Esses dados permitem identificar sessoes diferentes do mesmo usuario e preparar revogacao por dispositivo em evolucoes futuras.

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
4. O token antigo recebe `lastUsedAt` e e revogado com `revokedAt`.
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
- Revoga o registro persistido e preenche `revokedAt`.
- Retorna `204 No Content`.

## Limpeza Agendada

A classe `RefreshTokenCleanupJob` executa periodicamente a remocao de refresh tokens expirados ou revogados antes do periodo de retencao configurado.

Fluxo:

```text
Job agendado
  -> calcula cutoff por retencao
  -> remove tokens expirados antes do cutoff
  -> remove tokens revogados antes do cutoff
  -> registra quantidade removida em log
```

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
- Metadados nao carregam segredo e servem apenas para rastreabilidade da sessao.

## Melhorias Planejadas

- Deteccao de reutilizacao de refresh token antigo.
- Revogacao de todos os tokens de um usuario.
- Endpoint administrativo ou do proprio usuario para listar e revogar sessoes por dispositivo.
- Armazenamento distribuido ou blacklist com Redis em ambientes multi-instancia.
