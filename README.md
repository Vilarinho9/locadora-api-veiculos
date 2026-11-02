# Locadora de Veículos — API REST com Spring Boot

API REST para gestão de uma locadora de veículos: **veículos**, **clientes** e **locações**, com filtros, regras de negócio, tratamento padronizado de erros HTTP, documentação Swagger e telas web que consomem a API.

> Trabalho da disciplina de **Programação Backend**.

---

## Sumário

1. [Descrição e objetivo](#1-descrição-e-objetivo)
2. [Requisitos do trabalho × onde estão atendidos](#2-requisitos-do-trabalho--onde-estão-atendidos)
3. [Tecnologias](#3-tecnologias)
4. [Como executar](#4-como-executar)
5. [Configuração do banco de dados](#5-configuração-do-banco-de-dados)
6. [Modelo de dados](#6-modelo-de-dados)
7. [Regras de negócio](#7-regras-de-negócio)
8. [Endpoints](#8-endpoints)
9. [Exemplos de utilização dos principais endpoints](#9-exemplos-de-utilização-dos-principais-endpoints)
10. [Filtros, paginação e ordenação](#10-filtros-paginação-e-ordenação)
11. [Status HTTP e tratamento de exceções](#11-status-http-e-tratamento-de-exceções)
12. [Documentação Swagger](#12-documentação-swagger)
13. [Telas (front-end)](#13-telas-front-end)
14. [Arquitetura e estrutura de pastas](#14-arquitetura-e-estrutura-de-pastas)
15. [Testes](#15-testes)
16. [Dados de demonstração](#16-dados-de-demonstração)

---

## 1. Descrição e objetivo

O sistema gerencia o dia a dia de uma locadora de veículos. Ele permite cadastrar a frota (**veículos**) e os **clientes**, registrar **locações** e acompanhar a devolução, sempre aplicando as regras do negócio:

- uma locação liga um cliente a um veículo e só é aceita se o cliente tiver idade mínima, CNH válida e limite de locações disponível, e se o veículo estiver livre;
- o valor é calculado automaticamente, com **desconto progressivo** para períodos longos;
- na devolução, o sistema calcula **multa por atraso** ou **taxa de devolução antecipada**, atualiza a quilometragem e libera o veículo.

**Objetivo:** demonstrar uma API REST completa em Spring Boot, com CRUD, filtros, regras de negócio, tratamento correto de status HTTP, documentação com Swagger e um painel web conectado ao backend.

---

## 2. Requisitos do trabalho × onde estão atendidos

| Requisito | Como foi atendido |
|---|---|
| Spring Boot com no mínimo 2 tabelas relacionadas | 3 tabelas: `clientes`, `veiculos` e `locacoes` (a locação se relaciona com cliente e veículo via `@ManyToOne`) |
| CRUD completo para as tabelas | `Veiculo`, `Cliente` e `Locacao`: criar, listar, buscar por id, atualizar e excluir ([endpoints](#8-endpoints)) |
| Filtragem de dados | Filtros dinâmicos combináveis com **JPA Specifications** + paginação e ordenação ([seção 10](#10-filtros-paginação-e-ordenação)) |
| Regras de negócio mais complexas | Idade mínima, CNH, disponibilidade, limite de locações, desconto progressivo, multa por atraso, taxa de devolução antecipada, controle de concorrência ([seção 7](#7-regras-de-negócio)) |
| Status HTTP e exceções tratados | `GlobalExceptionHandler` com 400/404/405/409/422/500 em formato único de erro ([seção 11](#11-status-http-e-tratamento-de-exceções)) |
| Documentação com Swagger | springdoc-openapi: `/swagger-ui.html` ([seção 12](#12-documentação-swagger)) |
| Telas conectadas ao backend | Painel web (HTML/CSS/JS) servido pela própria aplicação em `/` ([seção 13](#13-telas-front-end)) |
| Testes | Testes unitários e de integração ([seção 15](#15-testes)) |
| README bem estruturado | Este arquivo |

---

## 3. Tecnologias

- **Java 17+** e **Spring Boot 3.3.5** (Web, Data JPA, Validation)
- **Hibernate / JPA** com **H2** em arquivo (padrão, sem instalar nada) — **PostgreSQL** opcional via perfil
- **springdoc-openapi 2.6.0** (Swagger UI)
- **JUnit 5 + MockMvc** para testes
- **Maven** para build e dependências
- Front-end em **HTML, CSS e JavaScript puro** (sem build, sem dependências)

---

## 4. Como executar

### Pré-requisitos

- **JDK 17 ou superior** (`java -version`)
- **Maven 3.9+** (`mvn -version`) — ou abra o projeto no IntelliJ / Eclipse / VS Code, que já trazem suporte ao Maven

### Passo a passo

```bash
# 1. clone o repositório e entre na pasta do projeto (a que contém o pom.xml)
git clone <URL deste repositório>
cd locadora-api

# 2. suba a aplicação
mvn spring-boot:run
```

Na primeira execução o banco é criado e populado com dados de demonstração automaticamente.

| O quê | Endereço |
|---|---|
| **Telas (painel web)** | http://localhost:8080 |
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **OpenAPI (JSON)** | http://localhost:8080/v3/api-docs |
| **Console do H2** | http://localhost:8080/h2-console — JDBC URL `jdbc:h2:file:./data/locadora`, usuário `sa`, senha em branco |

Para parar a aplicação, use `Ctrl+C` no terminal.

### Gerar o .jar e executar

```bash
mvn clean package
java -jar target/locadora-api-1.0.0.jar
```

### Problemas comuns

| Sintoma | Solução |
|---|---|
| `mvn` não é reconhecido | Instale o Maven (https://maven.apache.org/download.cgi), adicione a pasta `bin` ao `PATH` e abra um novo terminal. Ou abra o projeto em uma IDE. |
| `Port 8080 was already in use` | Outro programa usa a porta 8080. Rode em outra porta e use-a nos endereços acima: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`. No PowerShell, coloque o parâmetro entre aspas: `"-Dspring-boot.run.arguments=--server.port=8081"`. |
| Erro ao criar a pasta `data` (Windows) | Pastas protegidas, como *Documentos*, podem bloquear a escrita. Rode o projeto de outra pasta (ex.: `C:\projetos`) ou use banco em memória: `"-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:mem:locadora"`. |

---

## 5. Configuração do banco de dados

### H2 em arquivo (padrão)

Não exige instalação. As configurações estão em `src/main/resources/application.properties`:

| Propriedade | Valor |
|---|---|
| `spring.datasource.url` | `jdbc:h2:file:./data/locadora` (arquivo criado na pasta `data/`) |
| `spring.datasource.username` / `password` | `sa` / (vazia) |
| `spring.jpa.hibernate.ddl-auto` | `update` (as tabelas são criadas a partir das entidades) |
| `spring.h2.console.path` | `/h2-console` |
| `app.seed` | `true` (carrega os dados de demonstração quando o banco está vazio; use `false` para desativar) |

O banco persiste entre execuções. Para inspecioná-lo, acesse o console do H2 (endereço na seção anterior).

### Zerar o banco

Pare a aplicação e apague a pasta `data/` (criada ao lado do projeto). Na próxima execução ela é recriada com os dados de demonstração.

### PostgreSQL (opcional)

1. Crie um banco chamado `locadora`. Com Docker, por exemplo:
   ```bash
   docker run --name locadora-db -e POSTGRES_DB=locadora -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
   ```
2. Suba a aplicação com o perfil `postgres`:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=postgres
   ```

Configuração em `application-postgres.properties`. Valores padrão: `jdbc:postgresql://localhost:5432/locadora`, usuário `postgres`, senha `postgres`. Podem ser trocados pelas variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD`.

### Banco dos testes

Os testes usam H2 **em memória** (`src/test/resources/application.properties`), sem tocar nos dados da aplicação.

---

## 6. Modelo de dados

```mermaid
erDiagram
    CLIENTE ||--o{ LOCACAO : realiza
    VEICULO ||--o{ LOCACAO : "é alugado em"

    CLIENTE {
        bigint id PK
        varchar nome
        varchar cpf UK
        varchar email UK
        varchar telefone
        date data_nascimento
        varchar cnh_numero
        date cnh_validade
    }
    VEICULO {
        bigint id PK
        varchar placa UK
        varchar marca
        varchar modelo
        int ano
        varchar categoria
        decimal valor_diaria
        int quilometragem
        varchar status
        bigint versao
    }
    LOCACAO {
        bigint id PK
        bigint cliente_id FK
        bigint veiculo_id FK
        date data_retirada
        date data_prevista_devolucao
        date data_devolucao
        decimal valor_diaria
        decimal desconto
        decimal valor_previsto
        decimal multa
        decimal valor_final
        varchar status
        varchar observacoes
    }
```

- **Categorias de veículo:** `ECONOMICO`, `INTERMEDIARIO`, `SUV`, `LUXO`, `ESPORTIVO`
- **Status do veículo:** `DISPONIVEL`, `ALUGADO`, `MANUTENCAO`
- **Status da locação:** `ATIVA`, `FINALIZADA`, `CANCELADA`
- A locação guarda um *snapshot* da diária (`valor_diaria`): reajustes futuros no veículo não alteram locações já feitas.

---

## 7. Regras de negócio

### Cadastros

| Regra | Resposta em caso de violação |
|---|---|
| CPF válido (dígitos verificadores; aceita com ou sem máscara) | `400` |
| CPF e e-mail únicos por cliente | `409` |
| Cliente com no mínimo 18 anos | `422` |
| CNH com exatamente 11 dígitos | `400` |
| Placa válida (padrão antigo `ABC1234` ou Mercosul `ABC1D23`), normalizada em maiúsculas e sem hífen; placa única | `400` / `409` |
| `ALUGADO` não pode ser definido manualmente (só as locações mudam esse status) | `422` |
| Veículo alugado não pode ter o status alterado manualmente | `422` |
| Quilometragem do veículo nunca pode diminuir | `422` |
| Veículo alugado ou com histórico de locações não pode ser excluído | `409` |
| Cliente com locações registradas não pode ser excluído | `409` |

### Criar locação (`POST /api/locacoes`)

| # | Regra | Violação |
|---|---|---|
| 1 | Retirada não pode estar no passado; devolução prevista deve ser posterior à retirada; período máximo de **30 dias** | `422` |
| 2 | Cliente com no mínimo **21 anos** — e **25 anos** para veículos `LUXO` e `ESPORTIVO` | `422` |
| 3 | **CNH válida até a data prevista de devolução** | `422` |
| 4 | Veículo precisa estar `DISPONIVEL` (não pode estar alugado nem em manutenção) | `422` |
| 5 | Cliente pode ter no máximo **2 locações ativas** ao mesmo tempo | `422` |
| 6 | Duas requisições simultâneas pelo mesmo carro: a segunda falha por **bloqueio otimista** (`@Version`) | `409` |

Ao criar, o veículo passa para `ALUGADO` e o **valor previsto** é calculado com **desconto progressivo**:

| Duração | Desconto |
|---|---|
| até 6 dias | 0% |
| 7 a 14 dias | 10% |
| 15 dias ou mais | 15% |

### Devolução (`POST /api/locacoes/{id}/devolucao`)

Só locações `ATIVA`; a data de devolução não pode ser futura nem anterior à retirada; a quilometragem final não pode ser menor que a atual. O **valor final** depende do cenário:

| Cenário | Cálculo |
|---|---|
| **No prazo** | valor previsto |
| **Com atraso** | valor previsto + (diária × dias de atraso) + **multa de 20% da diária por dia de atraso** |
| **Antecipada** | diárias dos dias efetivamente usados (mínimo 1, com o desconto recalculado) + **taxa de 10% da diária por dia não utilizado** |

Exemplos com diária de R$ 100,00:
- 5 dias previstos (R$ 500,00), devolvido com 5 dias de atraso → 500 + 500 + 100 de multa = **R$ 1.100,00**
- 5 dias previstos, devolvido após 2 dias → 200 + 30 de taxa (3 dias não usados) = **R$ 230,00**

Após a devolução a locação vira `FINALIZADA`, a quilometragem do veículo é atualizada e ele volta a `DISPONIVEL`.

### Outras ações

- **Cancelar** (`PATCH /api/locacoes/{id}/cancelar`): só locação ativa que ainda não começou; libera o veículo.
- **Alterar** (`PUT /api/locacoes/{id}`): só locação ativa que ainda não começou; altera data prevista e observações, **revalida** CNH/prazo e **recalcula** o valor.
- **Excluir** (`DELETE /api/locacoes/{id}`): bloqueado (`409`) para locações ativas.

---

## 8. Endpoints

### Veículos — `/api/veiculos`

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| GET | `/api/veiculos` | Lista com filtros, paginação e ordenação | `200` |
| GET | `/api/veiculos/{id}` | Busca por id | `200` |
| POST | `/api/veiculos` | Cadastra | `201` + `Location` |
| PUT | `/api/veiculos/{id}` | Atualiza | `200` |
| DELETE | `/api/veiculos/{id}` | Exclui | `204` |

### Clientes — `/api/clientes`

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| GET | `/api/clientes` | Lista com filtros, paginação e ordenação | `200` |
| GET | `/api/clientes/{id}` | Busca por id | `200` |
| POST | `/api/clientes` | Cadastra | `201` + `Location` |
| PUT | `/api/clientes/{id}` | Atualiza | `200` |
| DELETE | `/api/clientes/{id}` | Exclui | `204` |

### Locações — `/api/locacoes`

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| GET | `/api/locacoes` | Lista com filtros, paginação e ordenação | `200` |
| GET | `/api/locacoes/{id}` | Busca por id | `200` |
| POST | `/api/locacoes` | Registra locação (aplica as regras) | `201` + `Location` |
| PUT | `/api/locacoes/{id}` | Altera data prevista e observações | `200` |
| POST | `/api/locacoes/{id}/devolucao` | Registra a devolução e calcula o valor final | `200` |
| PATCH | `/api/locacoes/{id}/cancelar` | Cancela a locação | `200` |
| DELETE | `/api/locacoes/{id}` | Exclui (não permitido se ativa) | `204` |

### Dashboard — `GET /api/dashboard`

Frota por status, total de clientes, locações ativas/atrasadas e receita das locações finalizadas.

---

## 9. Exemplos de utilização dos principais endpoints

Com a aplicação no ar em `http://localhost:8080`. As respostas abaixo foram resumidas; os `id` podem variar conforme os dados existentes.

> **Datas:** a retirada de uma locação não pode estar no passado. Ajuste as datas dos exemplos para hoje em diante.
>
> **Windows / PowerShell:** `curl` é um apelido de outro comando; use `curl.exe`, ou teste direto pelo [Swagger](#12-documentação-swagger) ou pelo arquivo [`docs/requests.http`](docs/requests.http), que reúne dezenas de requisições prontas com as respostas esperadas.

### Veículos

**Cadastrar** — `201 Created` com o header `Location: http://localhost:8080/api/veiculos/9`

```bash
curl -X POST http://localhost:8080/api/veiculos \
  -H "Content-Type: application/json" \
  -d '{"placa":"KWD1A23","marca":"Renault","modelo":"Kwid","ano":2024,"categoria":"ECONOMICO","valorDiaria":95.00,"quilometragem":1200}'
```

```json
{
  "id": 9, "placa": "KWD1A23", "marca": "Renault", "modelo": "Kwid", "ano": 2024,
  "categoria": "ECONOMICO", "valorDiaria": 95.00, "quilometragem": 1200, "status": "DISPONIVEL"
}
```

**Atualizar** — `200 OK` (o PUT recebe o objeto completo)

```bash
curl -X PUT http://localhost:8080/api/veiculos/9 \
  -H "Content-Type: application/json" \
  -d '{"placa":"KWD1A23","marca":"Renault","modelo":"Kwid","ano":2024,"categoria":"ECONOMICO","valorDiaria":99.90,"quilometragem":1200}'
```

**Buscar por id** — `200 OK` (ou `404` se não existir)

```bash
curl http://localhost:8080/api/veiculos/9
```

**Listar com filtros** — `200 OK`, resposta paginada

```bash
curl "http://localhost:8080/api/veiculos?categoria=SUV&status=DISPONIVEL&sort=valorDiaria,asc"
```

```json
{ "content": [
    { "id": 3, "placa": "QWE3F67", "modelo": "T-Cross", "valorDiaria": 220.00, "status": "DISPONIVEL" },
    { "id": 5, "placa": "MNO5H01", "modelo": "Compass", "valorDiaria": 300.00, "status": "DISPONIVEL" }
  ],
  "page": 0, "size": 10, "totalElements": 2, "totalPages": 1 }
```

**Excluir** — `204 No Content` (ou `409` se o veículo está alugado ou tem locações)

```bash
curl -X DELETE http://localhost:8080/api/veiculos/9
```

### Clientes

**Cadastrar** — `201 Created`

```bash
curl -X POST http://localhost:8080/api/clientes \
  -H "Content-Type: application/json" \
  -d '{"nome":"Joana Pereira","cpf":"168.995.350-09","email":"joana@email.com","telefone":"(86) 99999-0005","dataNascimento":"1992-05-20","cnhNumero":"45678901234","cnhValidade":"2031-06-30"}'
```

**CPF inválido** — `400 Bad Request`, com o campo que falhou

```bash
curl -X POST http://localhost:8080/api/clientes \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teste","cpf":"111.111.111-11","email":"teste@email.com","dataNascimento":"1990-01-01","cnhNumero":"12345678901","cnhValidade":"2030-01-01"}'
```

```json
{ "status": 400, "mensagem": "Dados inválidos: verifique os campos informados",
  "campos": [ { "campo": "cpf", "mensagem": "CPF inválido" } ] }
```

### Locações

**Registrar** — `201 Created`. Exemplo de 10 dias com diária de R$ 130,00: subtotal de R$ 1.300,00, desconto de 10% e valor previsto de R$ 1.170,00.

```bash
curl -X POST http://localhost:8080/api/locacoes \
  -H "Content-Type: application/json" \
  -d '{"clienteId":2,"veiculoId":2,"dataRetirada":"2026-11-05","dataPrevistaDevolucao":"2026-11-15","observacoes":"Cliente solicitou cadeirinha infantil"}'
```

```json
{
  "id": 2, "clienteId": 2, "clienteNome": "Carlos Souza",
  "veiculoId": 2, "veiculoDescricao": "RIO2E45 - Hyundai HB20",
  "dataRetirada": "2026-11-05", "dataPrevistaDevolucao": "2026-11-15", "dataDevolucao": null,
  "valorDiaria": 130.00, "desconto": 130.00, "valorPrevisto": 1170.00, "multa": null, "valorFinal": null,
  "status": "ATIVA", "atrasada": false, "observacoes": "Cliente solicitou cadeirinha infantil"
}
```

**Regra de negócio violada** — `422 Unprocessable Entity` (ex.: cliente com 23 anos alugando um veículo `LUXO`; resposta na [seção 11](#11-status-http-e-tratamento-de-exceções))

**Registrar a devolução** — `200 OK`. A data é opcional (se omitida, vale hoje); a resposta traz `multa` e `valorFinal` calculados e a locação passa a `FINALIZADA`.

```bash
curl -X POST http://localhost:8080/api/locacoes/1/devolucao \
  -H "Content-Type: application/json" \
  -d '{"quilometragemFinal":15450}'
```

**Cancelar** — `200 OK` (só locação ativa que ainda não começou)

```bash
curl -X PATCH http://localhost:8080/api/locacoes/2/cancelar
```

### Dashboard

```bash
curl http://localhost:8080/api/dashboard
```

```json
{ "totalVeiculos": 8, "veiculosDisponiveis": 6, "veiculosAlugados": 1, "veiculosManutencao": 1,
  "totalClientes": 4, "locacoesAtivas": 1, "locacoesAtrasadas": 0, "receitaTotal": 0 }
```

---

## 10. Filtros, paginação e ordenação

Todos os filtros são **opcionais e combináveis**. A resposta paginada segue o formato:

```json
{ "content": [ ... ], "page": 0, "size": 10, "totalElements": 8, "totalPages": 1 }
```

| Recurso | Parâmetros de filtro |
|---|---|
| Veículos | `placa`, `marca`, `modelo` (busca parcial), `categoria`, `status`, `valorMin`, `valorMax` (faixa de diária) |
| Clientes | `nome`, `cpf`, `email` (busca parcial, sem diferenciar maiúsculas) |
| Locações | `status`, `clienteId`, `veiculoId`, `retiradaDe`, `retiradaAte` (`yyyy-MM-dd`), `atrasadas=true` |

Paginação e ordenação: `page`, `size`, `sort=campo,asc|desc`.

```bash
# SUVs disponíveis com diária até R$ 250, da mais barata para a mais cara
curl "http://localhost:8080/api/veiculos?categoria=SUV&status=DISPONIVEL&valorMax=250&sort=valorDiaria,asc"

# clientes com "silva" no nome
curl "http://localhost:8080/api/clientes?nome=silva"

# locações ativas que já passaram da data de devolução
curl "http://localhost:8080/api/locacoes?status=ATIVA&atrasadas=true"
```

Valores inválidos (ex.: `status=VOANDO` ou `sort=campoInexistente`) retornam `400` com mensagem clara.

---

## 11. Status HTTP e tratamento de exceções

O `GlobalExceptionHandler` (`@RestControllerAdvice`) traduz qualquer exceção em um **formato único de erro**:

| Status | Quando |
|---|---|
| `200 OK` | Consulta/atualização/ação realizada |
| `201 Created` | Recurso criado (com header `Location`) |
| `204 No Content` | Exclusão realizada |
| `400 Bad Request` | Validação de campos (lista de campos com erro), JSON malformado, enum/parâmetro/ordenação inválidos |
| `404 Not Found` | Recurso (ou rota) inexistente |
| `405 / 415` | Método HTTP ou `Content-Type` não suportado |
| `409 Conflict` | CPF/e-mail/placa duplicados, exclusão com vínculos, locação ativa, concorrência |
| `422 Unprocessable Entity` | Dados válidos, mas **regra de negócio violada** |
| `500 Internal Server Error` | Erro inesperado (detalhe só no log, nunca vaza para o cliente) |

Exemplo de erro de validação (`400`):

```json
{
  "timestamp": "2026-10-28T16:50:12.331",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos: verifique os campos informados",
  "caminho": "/api/clientes",
  "campos": [
    { "campo": "cpf", "mensagem": "CPF inválido" }
  ]
}
```

Exemplo de regra de negócio (`422`):

```json
{
  "timestamp": "2026-10-28T16:51:40.902",
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Veículos da categoria LUXO exigem cliente com no mínimo 25 anos",
  "caminho": "/api/locacoes"
}
```

As exceções do domínio são `RecursoNaoEncontradoException` (404), `ConflitoException` (409) e `RegraNegocioException` (422).

---

## 12. Documentação Swagger

Com a aplicação no ar, acesse **http://localhost:8080/swagger-ui.html**.

- Endpoints agrupados por recurso (Veículos, Clientes, Locações, Dashboard)
- Cada operação descreve as regras aplicadas e os códigos de resposta possíveis
- Os DTOs trazem exemplos prontos: clique em **Try it out** e **Execute**
- O contrato OpenAPI em JSON fica em `/v3/api-docs` (pode ser importado no Postman/Insomnia)

Um arquivo com requisições prontas está em [`docs/requests.http`](docs/requests.http) (IntelliJ HTTP Client ou extensão REST Client do VS Code).

---

## 13. Telas (front-end)

O painel é servido pela própria aplicação em **http://localhost:8080** (arquivos em `src/main/resources/static`). Como front e API estão na mesma origem, não há configuração extra.

| Tela | O que faz |
|---|---|
| **Dashboard** | Indicadores da frota, clientes, locações ativas/atrasadas e receita |
| **Veículos** | Listagem com filtros (placa, marca, modelo, categoria, status, faixa de diária), paginação, cadastro, edição e exclusão |
| **Clientes** | Listagem com filtros (nome, CPF, e-mail), cadastro, edição e exclusão; CNH vencida destacada |
| **Locações** | Listagem com filtros (status, cliente, veículo, período, atrasadas), nova locação com **prévia do preço**, edição, devolução, cancelamento e exclusão |

As mensagens de erro da API (validações `400`, regras `422`, conflitos `409`) aparecem diretamente nos formulários, com o código HTTP.

---

## 14. Arquitetura e estrutura de pastas

Arquitetura em camadas: **Controller → Service → Repository → Banco**. As regras de negócio ficam nos *services*; os controllers só recebem/validam a requisição e devolvem o status HTTP correto.

```
locadora-api/
├── pom.xml
├── README.md
├── docs/
│   ├── requests.http            # requisições prontas para testar a API
│   └── ROTEIRO_VIDEO.md         # roteiro sugerido para o vídeo
└── src/
    ├── main/
    │   ├── java/br/com/locadora/
    │   │   ├── LocadoraApplication.java
    │   │   ├── config/          # Clock, CORS e metadados do OpenAPI
    │   │   ├── controller/      # endpoints REST + anotações Swagger
    │   │   ├── domain/          # entidades JPA e enums
    │   │   ├── dto/             # records de request/response (com validação)
    │   │   ├── exception/       # exceções do domínio + GlobalExceptionHandler
    │   │   ├── repository/      # Spring Data JPA + Specifications (filtros)
    │   │   ├── service/         # regras de negócio e CalculadoraLocacao
    │   │   ├── validation/      # @Cpf (Bean Validation)
    │   │   └── seed/            # dados de demonstração
    │   └── resources/
    │       ├── application.properties
    │       ├── application-postgres.properties
    │       └── static/          # telas: index.html, css/, js/
    └── test/                    # testes unitários e de integração
```

Decisões de projeto:

- **DTOs como `record`** com Bean Validation: a entidade nunca é exposta diretamente.
- **`CalculadoraLocacao`** isola as fórmulas de preço, sem dependência de Spring (fácil de testar).
- **`Clock` injetável**: as regras de data usam o fuso de Brasília e podem ser testadas sem depender do relógio do sistema.
- **Specifications** em vez de dezenas de métodos `findBy...`: filtros opcionais combináveis com uma única consulta.
- **`@EntityGraph`** na listagem de locações evita o problema de N+1 consultas.
- **`open-in-view` desligado**: o mapeamento para DTO acontece dentro da transação do service.
- **`@Version` em `Veiculo`**: bloqueio otimista contra locações simultâneas do mesmo veículo.

---

## 15. Testes

```bash
mvn test
```

| Classe | Tipo | O que cobre |
|---|---|---|
| `CpfUtilTest` | unitário | validação e normalização de CPF |
| `CalculadoraLocacaoTest` | unitário | desconto progressivo, multa de atraso, taxa de antecipação |
| `LocadoraApiIntegrationTest` | integração (MockMvc + H2 em memória) | validação (400), duplicidade (409), 404, filtros, criação de locação, veículo indisponível, idade mínima para categoria de luxo, CNH vencida, limite de 2 locações ativas, período máximo, devolução com atraso e antecipada, cancelamento e integridade referencial |

---

## 16. Dados de demonstração

Na primeira execução são carregados **8 veículos**, **4 clientes** e **1 locação ativa**, escolhidos para demonstrar as regras:

| Dado | Para demonstrar |
|---|---|
| **Ana Costa** (23 anos, id 3) × BMW 320i (id 6) / Camaro (id 7) (`LUXO`/`ESPORTIVO`) | `422` — idade mínima de 25 anos para categorias premium |
| **Pedro Lima** (id 4, CNH vencida) | `422` — CNH vencida antes da devolução |
| **Fiat Mobi** (id 8, em manutenção) | `422` — veículo indisponível |
| **Fiat Argo** (id 1, alugado por Maria da Silva) | `422` — veículo já alugado; `409` ao tentar excluir |
| **Maria da Silva** (id 1) | `409` ao tentar excluir (tem locação); devolver a locação mostra o cálculo da taxa antecipada |
| CPF inválido ao cadastrar cliente | `400` com o campo `cpf` na resposta |
