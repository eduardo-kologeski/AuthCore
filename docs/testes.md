# Testes

Este documento descreve a estrategia de testes atual do AuthCore.

## Ferramentas

O projeto usa:

- JUnit
- Spring Boot Test
- MockMvc
- H2 em memoria
- Spring Security Test

## Testes Existentes

### JwtApiApplicationTests

Classe:

- `src/test/java/com/example/jwtapi/JwtApiApplicationTests.java`

Objetivo:

- Verificar se o contexto Spring Boot carrega corretamente.

### AuthIntegrationTests

Classe:

- `src/test/java/com/example/jwtapi/auth/AuthIntegrationTests.java`

Objetivo:

- Validar os principais fluxos de autenticacao usando endpoints HTTP simulados com MockMvc.

### RefreshTokenPersistenceTests

Classe:

- `src/test/java/com/example/jwtapi/security/RefreshTokenPersistenceTests.java`

Objetivo:

- Validar a persistencia segura e o ciclo de vida dos refresh tokens diretamente nas camadas de service e repository.

## Cenarios Cobertos

| Cenario | Cobertura |
| --- | --- |
| Login com `rememberMe=false` | Verifica retorno apenas de access token e ausencia de refresh token persistido. |
| Login com `rememberMe=true` | Verifica retorno de access token, refresh token e `expiresIn`. |
| Refresh valido | Verifica geracao de novo access token e novo refresh token. |
| Refresh expirado | Verifica retorno `401`. |
| Refresh revogado | Verifica retorno `401`. |
| Logout | Verifica revogacao do refresh token. |
| Rotacao de refresh token | Verifica que o refresh token antigo nao pode ser reutilizado. |
| Hash de refresh token | Verifica que o valor original nao e salvo no banco. |
| Metadados de refresh token | Verifica `deviceName`, `userAgent` e `ipAddress`. |
| Datas de uso e revogacao | Verifica `lastUsedAt` e `revokedAt`. |
| Limpeza de tokens antigos | Verifica remocao de tokens expirados e revogados antes do cutoff. |
| Access token valido | Verifica acesso ao endpoint `/api/users/me`. |
| Usuario publico com role USER | Verifica role padrao no cadastro. |
| Bootstrap de ADMIN | Verifica criacao segura do primeiro administrador. |
| ADMIN em endpoints administrativos | Verifica listagem e alteracao de role. |
| USER bloqueado em endpoints admin | Verifica retorno `403`. |

## Como Executar

```bash
mvn test
```

## Banco Durante Os Testes

Os testes usam H2 em memoria e limpam os dados antes de cada cenario:

```text
refreshTokenRepository.deleteAll()
userRepository.deleteAll()
```

## Boas Praticas Aplicadas

- Testes exercitam os endpoints reais.
- O contexto Spring e carregado.
- As regras de seguranca participam dos testes.
- Refresh tokens sao validados no banco.
- Refresh tokens sao persistidos apenas como hash.
- A limpeza agendada possui regra testavel na camada de service/repository.
- O comportamento de rotacao e revogacao e testado.

## Melhorias Planejadas

- Testes de validacao de payload invalido.
- Testes de e-mail duplicado.
- Testes de senha invalida.
- Testes de token JWT expirado.
- Testes de controller com contratos JSON mais completos.
- Testes com Testcontainers para validar PostgreSQL real em pipeline.
