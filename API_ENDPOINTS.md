# Documentação da API Requiem - Auto Acompanhamento & Preset Store

## Visão Geral

A **Requiem API** é o backend responsável por gerenciar a inteligência de **auto acompanhamento musical** do projeto Requiem.

O sistema foca 100% no acompanhamento dinâmico: armazenamento de sessões, persistência de progressões harmônicas, notas e parâmetros rítmicos, serialização de configurações e compartilhamento público através de uma **Loja Comunitária de Presets** (*Preset Store*).

> [!NOTE]
> O Requiem opera como um auto acompanhamento em tempo real. A API não realiza exportação ou conversão de arquivos MIDI brutos, concentrando-se na estrutura serializada das configurações de acompanhamento (`compositionData`).

---

## Informações Gerais & Configuração

- **Base URL local:** `http://localhost:3001/api`
- **Formato das Datas:** Padrão ISO-8601 UTC (ex: `2026-11-17T13:20:00Z`).
- **Formato de Dados:** `application/json; charset=utf-8`.
- **Autenticação:** JWT (*JSON Web Token*) emitido no registro ou login.
- **Cabeçalho para rotas protegidas:**
  ```http
  Authorization: Bearer <seu_token_jwt>
  ```
- **Rotas Públicas (sem token obrigatório):**
  - `POST /auth/register`
  - `POST /auth/login`
  - `GET /health`
  - `GET /store/presets` *(listagem pública da loja)*
  - `GET /store/presets/:id` *(detalhes e configuração do preset)*
  - `POST /store/presets/:id/use` *(registro de download/uso)*

---

## Estrutura da Configuração de Auto Acompanhamento (`compositionData`)

O auto acompanhamento do Requiem organiza uma sessão em **blocos musicais estruturados** (`SerializedBlock`). Cada bloco contém a progressão harmônica de acordes, as notas detectadas (pitch) e as definições métricas (tempo, tom e compasso).

### Modelo de um Bloco Musical (`SerializedBlock`)

```json
{
  "id": "block-1731840000000",
  "name": "Chorus Principal",
  "bpm": 120,
  "key": "C",
  "timeSignature": "4/4",
  "progression": [
    {
      "id": "chord-seg-1",
      "chord": "Dm7",
      "durationBeats": 4
    },
    {
      "id": "chord-seg-2",
      "chord": "G7",
      "durationBeats": 4
    },
    {
      "id": "chord-seg-3",
      "chord": "Cmaj7",
      "durationBeats": 8
    }
  ],
  "notes": [
    {
      "pitch": 62,
      "startTime": 0.0,
      "endTime": 1.0,
      "amplitude": 0.85
    },
    {
      "pitch": 65,
      "startTime": 1.0,
      "endTime": 2.0,
      "amplitude": 0.78
    }
  ]
}
```

#### Dicionário de Campos do Bloco:

| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `id` | `String` | Identificador único da seção ou bloco musical. |
| `name` | `String` | Nome descritivo da seção (ex: `"Intro"`, `"Verso A"`, `"Chorus"`, `"Solo"`). |
| `bpm` | `Integer` | Andamento rítmico em batidas por minuto (ex: `80`, `120`, `140`). |
| `key` | `String` | Tonalidade de referência do acompanhamento (ex: `"C"`, `"Am"`, `"F#m"`, `"Eb"`). |
| `timeSignature` | `String` | Fórmula de compasso musical (ex: `"4/4"`, `"3/4"`, `"6/8"`). |
| `progression` | `Array` | Lista ordenada de segmentos harmônicos gerados pelo motor de harmonia. |
| `progression[].id` | `String` | Identificador único do segmento de acorde. |
| `progression[].chord` | `String` | Cifra harmônica do acorde (ex: `"Cmaj7"`, `"Am9"`, `"G7b9"`, `"Em7b5"`). |
| `progression[].durationBeats` | `Number` | Duração do acorde em tempos/batidas métricas (ex: `4`, `2`, `1.5`). |
| `notes` | `Array` | Lista de notas melódicas gravadas/detectadas para guia do acompanhamento. |
| `notes[].pitch` | `Number` | Número MIDI da nota fundamental (ex: `60` para Dó Central C4). |
| `notes[].startTime` | `Number` | Ponto inicial da nota em segundos a partir do início do bloco. |
| `notes[].endTime` | `Number` | Ponto final da nota em segundos. |
| `notes[].amplitude` | `Number` | Intensidade dinâmica relativa detectada (normalizada entre `0.0` e `1.0`). |

---

## 1. Autenticação

### 1.1 Registrar Novo Usuário

Cria uma conta de usuário e retorna o token JWT imediato.

- **Método & Rota:** `POST /auth/register`
- **Acesso:** Público

#### Exemplo de Requisição

```http
POST /api/auth/register HTTP/1.1
Host: localhost:3001
Content-Type: application/json

{
  "name": "Tom Jobim",
  "email": "tom.jobim@ipanema.mus.br",
  "password": "bossa-nova-secreta"
}
```

#### Exemplo de Resposta `201 Created`

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMmU0NTY3OC1lODliLTEyZDMtYTQ1Ni00MjY2MTQxNzQwMDAiLCJlbWFpbCI6InRvbS5qb2JpbUBpcGFuZW1hLm11cy5iciIsImlhdCI6MTczMTg0MDAwMCwiZXhwIjoxNzMyNDQ0ODAwfQ...",
  "user": {
    "id": "12e45678-e89b-12d3-a456-426614174000",
    "name": "Tom Jobim",
    "email": "tom.jobim@ipanema.mus.br"
  }
}
```

---

### 1.2 Login de Usuário

Autentica com email e senha previamente cadastrados.

- **Método & Rota:** `POST /auth/login`
- **Acesso:** Público

#### Exemplo de Requisição

```http
POST /api/auth/login HTTP/1.1
Host: localhost:3001
Content-Type: application/json

{
  "email": "tom.jobim@ipanema.mus.br",
  "password": "bossa-nova-secreta"
}
```

#### Exemplo de Resposta `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "12e45678-e89b-12d3-a456-426614174000",
    "name": "Tom Jobim",
    "email": "tom.jobim@ipanema.mus.br"
  }
}
```

---

### 1.3 Obter Dados do Usuário Autenticado

Retorna o perfil do usuário proprietário do token JWT.

- **Método & Rota:** `GET /auth/me`
- **Acesso:** Protegido (`Authorization: Bearer <token>`)

#### Exemplo de Resposta `200 OK`

```json
{
  "user": {
    "id": "12e45678-e89b-12d3-a456-426614174000",
    "name": "Tom Jobim",
    "email": "tom.jobim@ipanema.mus.br",
    "createdAt": "2026-11-17T10:15:30Z"
  }
}
```

---

## 2. Sessões de Auto Acompanhamento

### 2.1 Criar Nova Sessão

Inicia uma sessão de auto acompanhamento vazia para o usuário autenticado.

- **Método & Rota:** `POST /sessions`
- **Acesso:** Protegido
- **Corpo:** Opcional. Se omitido, o título padrão é `"Nova Composição"`.

#### Exemplo de Requisição

```json
{
  "title": "Ensaio Bossa Nova em Ré Maior"
}
```

#### Exemplo de Resposta `201 Created`

```json
{
  "session": {
    "id": "a9b8c7d6-1111-2222-3333-444455556666",
    "title": "Ensaio Bossa Nova em Ré Maior",
    "createdAt": "2026-11-17T10:20:00Z",
    "updatedAt": "2026-11-17T10:20:00Z",
    "compositionData": null,
    "messages": []
  }
}
```

---

### 2.2 Listar Sessões do Usuário

Retorna todas as sessões do usuário, ordenadas pela mais recentemente modificada. Traz a última mensagem trocada com o assistente inteligente se houver.

- **Método & Rota:** `GET /sessions`
- **Acesso:** Protegido

#### Exemplo de Resposta `200 OK`

```json
{
  "sessions": [
    {
      "id": "a9b8c7d6-1111-2222-3333-444455556666",
      "title": "Ensaio Bossa Nova em Ré Maior",
      "createdAt": "2026-11-17T10:20:00Z",
      "updatedAt": "2026-11-17T10:25:30Z",
      "compositionData": [
        {
          "id": "blk-1",
          "name": "Tema A",
          "bpm": 124,
          "key": "D",
          "timeSignature": "4/4",
          "progression": [
            { "id": "p1", "chord": "Dmaj7", "durationBeats": 4 },
            { "id": "p2", "chord": "Em7", "durationBeats": 4 }
          ],
          "notes": []
        }
      ],
      "messages": [
        {
          "id": "msg-1",
          "role": "assistant",
          "content": "Sugiro um Em7 seguido de A7(b9) para a preparação do refrão.",
          "sessionId": "a9b8c7d6-1111-2222-3333-444455556666",
          "createdAt": "2026-11-17T10:25:30Z"
        }
      ]
    }
  ]
}
```

---

### 2.3 Obter Sessão por ID

Carrega a sessão completa, incluindo todo o histórico de mensagens e os blocos de acompanhamento.

- **Método & Rota:** `GET /sessions/:id`
- **Acesso:** Protegido

#### Exemplo de Resposta `200 OK`

```json
{
  "session": {
    "id": "a9b8c7d6-1111-2222-3333-444455556666",
    "title": "Ensaio Bossa Nova em Ré Maior",
    "createdAt": "2026-11-17T10:20:00Z",
    "updatedAt": "2026-11-17T10:25:30Z",
    "compositionData": [
      {
        "id": "blk-1",
        "name": "Tema A",
        "bpm": 124,
        "key": "D",
        "timeSignature": "4/4",
        "progression": [
          { "id": "p1", "chord": "Dmaj7", "durationBeats": 4 },
          { "id": "p2", "chord": "Em7", "durationBeats": 4 }
        ],
        "notes": []
      }
    ],
    "messages": [
      {
        "id": "msg-1",
        "role": "user",
        "content": "Qual acorde posso usar para conectar ao Dmaj7?",
        "sessionId": "a9b8c7d6-1111-2222-3333-444455556666",
        "createdAt": "2026-11-17T10:24:00Z"
      },
      {
        "id": "msg-2",
        "role": "assistant",
        "content": "Sugiro um Em7 seguido de A7(b9) para a preparação do refrão.",
        "sessionId": "a9b8c7d6-1111-2222-3333-444455556666",
        "createdAt": "2026-11-17T10:25:30Z"
      }
    ]
  }
}
```

---

### 2.4 Salvar Alterações na Sessão (`PATCH /sessions/:id`)

Permite salvar ou atualizar o título e a lista completa de blocos do acompanhamento (`compositionData`).

- **Método & Rota:** `PATCH /sessions/:id`
- **Acesso:** Protegido

#### Exemplo de Requisição Real

```json
{
  "title": "Garota de Ipanema - Ensaio 2",
  "compositionData": [
    {
      "id": "blk-intro",
      "name": "Intro Instrumental",
      "bpm": 128,
      "key": "F",
      "timeSignature": "4/4",
      "progression": [
        { "id": "c1", "chord": "Fmaj7", "durationBeats": 4 },
        { "id": "c2", "chord": "G7", "durationBeats": 4 }
      ],
      "notes": [
        { "pitch": 65, "startTime": 0.0, "endTime": 1.5, "amplitude": 0.9 },
        { "pitch": 67, "startTime": 1.5, "endTime": 3.0, "amplitude": 0.85 }
      ]
    },
    {
      "id": "blk-verse",
      "name": "Verso A",
      "bpm": 128,
      "key": "F",
      "timeSignature": "4/4",
      "progression": [
        { "id": "c3", "chord": "Fmaj7", "durationBeats": 4 },
        { "id": "c4", "chord": "G7", "durationBeats": 4 },
        { "id": "c5", "chord": "Gm7", "durationBeats": 4 },
        { "id": "c6", "chord": "Gb7", "durationBeats": 4 }
      ],
      "notes": []
    }
  ]
}
```

#### Exemplo de Resposta `200 OK`

```json
{
  "success": true
}
```

---

### 2.5 Exportar Configuração da Sessão (`GET /sessions/:id/export`)

Gera um payload serializado com os blocos da sessão e carimbo de data/hora de exportação, ideal para backups, compartilhamento direto ou download.

- **Método & Rota:** `GET /sessions/:id/export`
- **Acesso:** Protegido

#### Exemplo de Resposta `200 OK`

```json
{
  "export": {
    "sessionId": "a9b8c7d6-1111-2222-3333-444455556666",
    "title": "Garota de Ipanema - Ensaio 2",
    "exportedAt": "2026-11-17T10:28:00Z",
    "configurationData": [
      {
        "id": "blk-intro",
        "name": "Intro Instrumental",
        "bpm": 128,
        "key": "F",
        "timeSignature": "4/4",
        "progression": [
          { "id": "c1", "chord": "Fmaj7", "durationBeats": 4 },
          { "id": "c2", "chord": "G7", "durationBeats": 4 }
        ],
        "notes": []
      }
    ]
  }
}
```

---

### 2.6 Exportar Sessão para a Loja Pública (`POST /sessions/:id/export-to-store`)

Atalho prático que pega a configuração atual da sessão do usuário e a publica instantaneamente como um preset na Loja Pública do Requiem.

- **Método & Rota:** `POST /sessions/:id/export-to-store`
- **Acesso:** Protegido

#### Exemplo de Requisição Real

```json
{
  "title": "Bossa Nova Clássica em Fá",
  "description": "Harmonia tradicional de bossa nova com cadência II-V e acordes alterados. Excelente para violão e piano.",
  "genre": "Bossa Nova",
  "tags": "bossa, violao, swing, mpb, jazz",
  "bpm": 128,
  "tonality": "F",
  "timeSignature": "4/4"
}
```

#### Exemplo de Resposta `201 Created`

```json
{
  "preset": {
    "id": "preset-8888-9999-aaaa-bbbbccccdddd",
    "title": "Bossa Nova Clássica em Fá",
    "description": "Harmonia tradicional de bossa nova com cadência II-V e acordes alterados. Excelente para violão e piano.",
    "genre": "Bossa Nova",
    "tags": "bossa, violao, swing, mpb, jazz",
    "bpm": 128,
    "tonality": "F",
    "timeSignature": "4/4",
    "configurationData": [
      {
        "id": "blk-intro",
        "name": "Intro Instrumental",
        "bpm": 128,
        "key": "F",
        "timeSignature": "4/4",
        "progression": [
          { "id": "c1", "chord": "Fmaj7", "durationBeats": 4 },
          { "id": "c2", "chord": "G7", "durationBeats": 4 }
        ],
        "notes": []
      }
    ],
    "downloadsCount": 0,
    "likesCount": 0,
    "authorId": "12e45678-e89b-12d3-a456-426614174000",
    "authorName": "Tom Jobim",
    "createdAt": "2026-11-17T10:30:00Z",
    "updatedAt": "2026-11-17T10:30:00Z"
  }
}
```

---

### 2.7 Excluir Sessão (`DELETE /sessions/:id`)

Remove a sessão e todas as mensagens associadas.

- **Método & Rota:** `DELETE /sessions/:id`
- **Acesso:** Protegido

#### Exemplo de Resposta `200 OK`

```json
{
  "success": true
}
```

---

## 3. Loja Comunitária de Presets (`/store/presets`)

A **Preset Store** é o catálogo público onde qualquer músico pode encontrar configurações prontas de auto acompanhamento postadas pela comunidade e importá-las para tocar junto no Requiem.

### 3.1 Listar Presets Públicos

Lista presets disponíveis com suporte a busca textual, filtro de gênero musical e ordenação.

- **Método & Rota:** `GET /store/presets`
- **Acesso:** Público (não exige token)
- **Parâmetros de Consulta (*Query Parameters*):**

| Parâmetro | Tipo | Padrão | Descrição |
| :--- | :--- | :--- | :--- |
| `search` | `String` | *(opcional)* | Termo pesquisado no título, descrição, tags ou nome do autor. |
| `genre` | `String` | *(opcional)* | Filtro exato por gênero musical (ex: `"Jazz"`, `"Bossa Nova"`, `"Lo-Fi"`, `"Pop"`). |
| `sortBy` | `String` | `"recent"` | Critério de ordenação: `"recent"` (mais recentes) ou `"popular"` (mais baixados). |

#### Exemplo de Chamada:
`GET /api/store/presets?search=jazz&genre=Jazz&sortBy=popular`

#### Exemplo de Resposta `200 OK`

```json
{
  "presets": [
    {
      "id": "preset-jazz-001",
      "title": "Jazz Club Trio Swing",
      "description": "Progressão fluida com baixo caminhante (walking bass) implícito em Dm.",
      "genre": "Jazz",
      "tags": "jazz, ii-v-i, swing, trio",
      "bpm": 136,
      "tonality": "Dm",
      "timeSignature": "4/4",
      "downloadsCount": 89,
      "likesCount": 34,
      "authorId": "user-miles-99",
      "authorName": "Miles Davis",
      "createdAt": "2026-11-15T18:00:00Z",
      "updatedAt": "2026-11-15T18:00:00Z"
    },
    {
      "id": "preset-jazz-002",
      "title": "Autumn Ballad 90bpm",
      "description": "Acompanhamento lento para baladas e improvisos com palhetada suave.",
      "genre": "Jazz",
      "tags": "ballad, slow, piano, chords",
      "bpm": 90,
      "tonality": "G",
      "timeSignature": "4/4",
      "downloadsCount": 45,
      "likesCount": 18,
      "authorId": "user-bill-77",
      "authorName": "Bill Evans",
      "createdAt": "2026-11-16T12:00:00Z",
      "updatedAt": "2026-11-16T12:00:00Z"
    }
  ]
}
```

---

### 3.2 Obter Detalhes do Preset (`GET /store/presets/:id`)

Recupera os detalhes completos do preset incluindo toda a estrutura de blocos (`configurationData`).

- **Método & Rota:** `GET /store/presets/:id`
- **Acesso:** Público

#### Exemplo de Resposta `200 OK`

```json
{
  "preset": {
    "id": "preset-jazz-001",
    "title": "Jazz Club Trio Swing",
    "description": "Progressão fluida com baixo caminhante implícito em Dm.",
    "genre": "Jazz",
    "tags": "jazz, ii-v-i, swing, trio",
    "bpm": 136,
    "tonality": "Dm",
    "timeSignature": "4/4",
    "configurationData": [
      {
        "id": "blk-swing-a",
        "name": "Seção A - II-V-I",
        "bpm": 136,
        "key": "Dm",
        "timeSignature": "4/4",
        "progression": [
          { "id": "p1", "chord": "Em7b5", "durationBeats": 2 },
          { "id": "p2", "chord": "A7b9", "durationBeats": 2 },
          { "id": "p3", "chord": "Dm7", "durationBeats": 4 }
        ],
        "notes": []
      }
    ],
    "downloadsCount": 89,
    "likesCount": 34,
    "authorId": "user-miles-99",
    "authorName": "Miles Davis",
    "createdAt": "2026-11-15T18:00:00Z",
    "updatedAt": "2026-11-15T18:00:00Z"
  }
}
```

---

### 3.3 Usar/Carregar Preset (`POST /store/presets/:id/use`)

Registra o uso do preset, incrementando seu contador de downloads.

- **Método & Rota:** `POST /store/presets/:id/use`
- **Acesso:** Público (ou Autenticado se `createSession=true`)
- **Parâmetro de Consulta:** `createSession=true|false` *(padrão: false)*
  - Quando `createSession=true`: exige autenticação (`Bearer <token>`) e cria uma **nova sessão pronta** na conta do usuário já preenchida com a configuração do preset para que o usuário comece a tocar imediatamente.

#### Exemplo de Chamada com criação de sessão:
`POST /api/store/presets/preset-jazz-001/use?createSession=true`

#### Exemplo de Resposta `200 OK`

```json
{
  "preset": {
    "id": "preset-jazz-001",
    "title": "Jazz Club Trio Swing",
    "downloadsCount": 90,
    "likesCount": 34,
    "configurationData": [...]
  },
  "createdSession": {
    "id": "sess-criada-automaticamente-123",
    "title": "Jazz Club Trio Swing",
    "createdAt": "2026-11-17T10:32:00Z",
    "updatedAt": "2026-11-17T10:32:00Z",
    "compositionData": [
      {
        "id": "blk-swing-a",
        "name": "Seção A - II-V-I",
        "bpm": 136,
        "key": "Dm",
        "timeSignature": "4/4",
        "progression": [
          { "id": "p1", "chord": "Em7b5", "durationBeats": 2 },
          { "id": "p2", "chord": "A7b9", "durationBeats": 2 },
          { "id": "p3", "chord": "Dm7", "durationBeats": 4 }
        ],
        "notes": []
      }
    ],
    "messages": []
  }
}
```

---

### 3.4 Publicar Configuração na Loja (`POST /store/presets`)

Permite a um usuário autenticado disponibilizar uma configuração na loja pública. Pode ser criada fornecendo os blocos serializados diretamente ou fornecendo um `sessionId`.

- **Método & Rota:** `POST /store/presets`
- **Acesso:** Protegido

#### Exemplo 1: Publicação com dados diretos de acompanhamento

```json
{
  "title": "Lo-Fi Beats Chilled Night",
  "description": "Progressão atmosférica em Cmaj9 e Fmaj7 com 75 BPM.",
  "genre": "Lo-Fi",
  "tags": "lofi, chill, piano, relax",
  "bpm": 75,
  "tonality": "C",
  "timeSignature": "4/4",
  "configurationData": [
    {
      "id": "blk-lofi-1",
      "name": "Loop Principal",
      "bpm": 75,
      "key": "C",
      "timeSignature": "4/4",
      "progression": [
        { "id": "c1", "chord": "Cmaj9", "durationBeats": 4 },
        { "id": "c2", "chord": "Fmaj7", "durationBeats": 4 }
      ],
      "notes": []
    }
  ]
}
```

#### Exemplo 2: Publicação vinculando a uma sessão existente

```json
{
  "sessionId": "a9b8c7d6-1111-2222-3333-444455556666",
  "title": "Meu Arranjo da Sessão",
  "description": "Compartilhando os acordes refinados da minha sessão",
  "genre": "Pop"
}
```

#### Exemplo de Resposta `201 Created`

```json
{
  "preset": {
    "id": "preset-lofi-777",
    "title": "Lo-Fi Beats Chilled Night",
    "description": "Progressão atmosférica em Cmaj9 e Fmaj7 com 75 BPM.",
    "genre": "Lo-Fi",
    "tags": "lofi, chill, piano, relax",
    "bpm": 75,
    "tonality": "C",
    "timeSignature": "4/4",
    "configurationData": [...],
    "downloadsCount": 0,
    "likesCount": 0,
    "authorId": "12e45678-e89b-12d3-a456-426614174000",
    "authorName": "Tom Jobim",
    "createdAt": "2026-11-17T10:35:00Z",
    "updatedAt": "2026-11-17T10:35:00Z"
  }
}
```

---

### 3.5 Listar Meus Presets na Loja (`GET /store/presets/my`)

Retorna a lista de todas as configurações postadas na loja pelo usuário autenticado.

- **Método & Rota:** `GET /store/presets/my`
- **Acesso:** Protegido

#### Exemplo de Resposta `200 OK`

```json
{
  "presets": [
    {
      "id": "preset-lofi-777",
      "title": "Lo-Fi Beats Chilled Night",
      "genre": "Lo-Fi",
      "downloadsCount": 14,
      "likesCount": 5,
      "authorId": "12e45678-e89b-12d3-a456-426614174000",
      "authorName": "Tom Jobim",
      "createdAt": "2026-11-17T10:35:00Z",
      "updatedAt": "2026-11-17T10:35:00Z"
    }
  ]
}
```

---

### 3.6 Atualizar Preset (`PATCH /store/presets/:id`)

Permite ao autor alterar título, descrição, tags, gênero musical ou blocos de configuração do preset.

- **Método & Rota:** `PATCH /store/presets/:id`
- **Acesso:** Protegido (somente o autor pode alterar)

#### Exemplo de Requisição

```json
{
  "title": "Lo-Fi Beats Chilled Night (Edição Especial)",
  "description": "Atualizado com novas variações de acordes nos tempos pares.",
  "bpm": 78
}
```

#### Exemplo de Resposta `200 OK`

```json
{
  "preset": {
    "id": "preset-lofi-777",
    "title": "Lo-Fi Beats Chilled Night (Edição Especial)",
    "description": "Atualizado com novas variações de acordes nos tempos pares.",
    "genre": "Lo-Fi",
    "bpm": 78,
    "updatedAt": "2026-11-17T10:38:00Z"
  }
}
```

---

### 3.7 Remover Preset (`DELETE /store/presets/:id`)

Remove a publicação da loja. Apenas o usuário que publicou pode remover.

- **Método & Rota:** `DELETE /store/presets/:id`
- **Acesso:** Protegido

#### Exemplo de Resposta `200 OK`

```json
{
  "success": true
}
```

---

## 4. Mensagens do Assistente na Sessão

### 4.1 Enviar Mensagem

Adiciona uma mensagem de texto à sessão (pergunta do usuário ou resposta do assistente inteligente).

- **Método & Rota:** `POST /sessions/:id/messages`
- **Acesso:** Protegido

#### Exemplo de Requisição

```json
{
  "role": "user",
  "content": "Quais acordes combinam com um solo pentatônico em Dó maior?"
}
```

#### Exemplo de Resposta `201 Created`

```json
{
  "message": {
    "id": "msg-8888-9999",
    "role": "user",
    "content": "Quais acordes combinam com um solo pentatônico em Dó maior?",
    "sessionId": "a9b8c7d6-1111-2222-3333-444455556666",
    "createdAt": "2026-11-17T10:40:00Z"
  }
}
```

---

## 5. Resumo de Respostas e Códigos de Status HTTP

| Código | Significado | Exemplo de Ocorrência |
| :--- | :--- | :--- |
| `200 OK` | Sucesso na requisição | Consulta de sessões, presets ou atualização com PATCH. |
| `201 Created` | Recurso criado com sucesso | Cadastro de usuário, criação de sessão ou preset publicado. |
| `400 Bad Request` | Parâmetros inválidos | Título ou blocos de configuração ausentes em requisições obrigatórias. |
| `401 Unauthorized` | Autenticação necessária ou inválida | Token JWT expirado, ausente ou solicitação de criar sessão sem estar logado. |
| `404 Not Found` | Recurso inexistente | Sessão ou preset não encontrado no banco ou não pertencente ao usuário. |
| `409 Conflict` | Conflito de unicidade | Tentativa de cadastro com email já existente. |
| `500 Server Error` | Erro interno do servidor | Falha de banco de dados ou erro inesperado de processamento. |

---

## 6. Exemplos com cURL para Teste Rápido

### Registro & Login
```bash
# Registrar
curl -X POST http://localhost:3001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Miles Davis","email":"miles@jazz.com","password":"kind-of-blue"}'

# Login
curl -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"miles@jazz.com","password":"kind-of-blue"}'
```

### Explorar Presets Públicos
```bash
# Listar todos os presets ordenados por popularidade
curl http://localhost:3001/api/store/presets?sortBy=popular

# Buscar presets de Jazz
curl "http://localhost:3001/api/store/presets?search=jazz&genre=Jazz"
```

### Criar Sessão & Exportar para a Loja
```bash
# Criar sessão
curl -X POST http://localhost:3001/api/sessions \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Minha Sessão de Estudo"}'

# Salvar blocos de acompanhamento
curl -X PATCH http://localhost:3001/api/sessions/<SESSION_ID> \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"compositionData":[{"id":"b1","name":"A","bpm":120,"key":"C","timeSignature":"4/4","progression":[{"id":"p1","chord":"Cmaj7","durationBeats":4}],"notes":[]}]}'

# Exportar direto para a Loja Pública
curl -X POST http://localhost:3001/api/sessions/<SESSION_ID>/export-to-store \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Preset Dó Maior Suave","genre":"Pop","description":"Acompanhamento limpo em Dó"}'
```
