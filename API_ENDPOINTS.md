# Endpoints da API Requiem

## Informações gerais

- **Base URL local:** `http://localhost:3001/api`
- O Requiem é um sistema inteligente de **auto acompanhamento**. A API armazena, serializa e compartilha configurações de acompanhamento (acordes, notas, BPM, tom, blocos de composição), sem dependência ou necessidade de exportação de arquivos MIDI.
- Os endpoints públicos da **Loja de Acompanhamentos** (`GET /store/presets`, `GET /store/presets/:id`, `POST /store/presets/:id/use`) não exigem autenticação.
- Os endpoints protegidos exigem o header:
  ```http
  Authorization: Bearer <token>
  ```
- Datas são retornadas no formato ISO-8601.

---

## Autenticação

### `POST /auth/register`

Cria uma conta e retorna um token JWT.

**Requisição**

```json
{
  "email": "usuario@example.com",
  "name": "Nome do Usuário",
  "password": "senha-segura"
}
```

**Resposta `201 Created`**

```json
{
  "token": "<jwt>",
  "user": {
    "id": "user-123",
    "email": "usuario@example.com",
    "name": "Nome do Usuário"
  }
}
```

### `POST /auth/login`

Autentica um usuário existente.

**Requisição**

```json
{
  "email": "usuario@example.com",
  "password": "senha-segura"
}
```

**Resposta `200 OK`**

```json
{
  "token": "<jwt>",
  "user": {
    "id": "user-123",
    "email": "usuario@example.com",
    "name": "Nome do Usuário"
  }
}
```

### `GET /auth/me`

Retorna os dados do usuário autenticado.

**Resposta `200 OK`**

```json
{
  "user": {
    "id": "user-123",
    "email": "usuario@example.com",
    "name": "Nome do Usuário",
    "createdAt": "2026-10-16T16:00:00Z"
  }
}
```

---

## Sessões de Auto Acompanhamento

### `POST /sessions`

Cria uma nova sessão de auto acompanhamento. O corpo é opcional; sem corpo, o título padrão é `Nova Composição`.

**Requisição**

```json
{
  "title": "Improviso de Terça"
}
```

**Resposta `201 Created`**

```json
{
  "session": {
    "id": "abc-123",
    "title": "Improviso de Terça",
    "createdAt": "2026-10-16T16:00:00Z",
    "updatedAt": "2026-10-16T16:00:00Z",
    "compositionData": null,
    "messages": []
  }
}
```

### `GET /sessions`

Lista as sessões do usuário autenticado, ordenadas pela atualização mais recente. Cada item inclui apenas a mensagem mais recente.

**Resposta `200 OK`**

```json
{
  "sessions": [
    {
      "id": "abc-123",
      "title": "Improviso de Terça",
      "createdAt": "2026-10-16T16:00:00Z",
      "updatedAt": "2026-10-16T16:30:00Z",
      "compositionData": [],
      "messages": [
        {
          "id": "msg-1",
          "role": "user",
          "content": "Vamos começar",
          "sessionId": "abc-123",
          "createdAt": "2026-10-16T16:30:00Z"
        }
      ]
    }
  ]
}
```

### `GET /sessions/:id`

Retorna uma sessão específica pertencente ao usuário autenticado com todas as suas mensagens e dados de composição serializados.

**Resposta `200 OK`**

```json
{
  "session": {
    "id": "abc-123",
    "title": "Improviso de Terça",
    "createdAt": "2026-10-16T16:00:00Z",
    "updatedAt": "2026-10-16T16:30:00Z",
    "compositionData": [
      {
        "id": "blk-1",
        "name": "Intro",
        "bpm": 120,
        "key": "C",
        "timeSignature": "4/4",
        "progression": [
          { "id": "p1", "chord": "Cmaj7", "durationBeats": 4 }
        ],
        "notes": []
      }
    ],
    "messages": []
  }
}
```

### `PATCH /sessions/:id`

Atualiza o título e/ou a configuração de blocos do auto acompanhamento (`compositionData`).

**Requisição**

```json
{
  "title": "Novo nome da sessão",
  "compositionData": [
    {
      "id": "blk-1",
      "name": "Parte A",
      "bpm": 120,
      "key": "Am",
      "timeSignature": "4/4",
      "progression": [
        { "id": "p1", "chord": "Am7", "durationBeats": 4 },
        { "id": "p2", "chord": "Dm7", "durationBeats": 4 }
      ],
      "notes": []
    }
  ]
}
```

**Resposta `200 OK`**

```json
{
  "success": true
}
```

### `GET /sessions/:id/export`

Exporta e serializa a configuração atual da sessão em formato JSON para cópia, download ou integração externa.

**Resposta `200 OK`**

```json
{
  "export": {
    "sessionId": "abc-123",
    "title": "Improviso de Terça",
    "exportedAt": "2026-11-17T10:00:00Z",
    "configurationData": [
      {
        "id": "blk-1",
        "name": "Parte A",
        "bpm": 120,
        "key": "Am",
        "timeSignature": "4/4",
        "progression": [
          { "id": "p1", "chord": "Am7", "durationBeats": 4 }
        ],
        "notes": []
      }
    ]
  }
}
```

### `POST /sessions/:id/export-to-store`

Exporta e serializa a configuração da sessão diretamente para a **Loja Pública** de configurações do Requiem.

**Requisição**

```json
{
  "title": "Bossa Nova Lounge",
  "description": "Acompanhamento relaxante em Am a 110bpm",
  "genre": "Bossa Nova",
  "tags": "bossa, piano, lounge",
  "bpm": 110,
  "tonality": "Am",
  "timeSignature": "4/4"
}
```

**Resposta `201 Created`**

```json
{
  "preset": {
    "id": "preset-123",
    "title": "Bossa Nova Lounge",
    "description": "Acompanhamento relaxante em Am a 110bpm",
    "genre": "Bossa Nova",
    "tags": "bossa, piano, lounge",
    "bpm": 110,
    "tonality": "Am",
    "timeSignature": "4/4",
    "configurationData": [...],
    "downloadsCount": 0,
    "likesCount": 0,
    "authorId": "user-123",
    "authorName": "Nome do Usuário",
    "createdAt": "2026-11-17T10:00:00Z",
    "updatedAt": "2026-11-17T10:00:00Z"
  }
}
```

### `DELETE /sessions/:id`

Exclui a sessão do usuário autenticado e suas mensagens.

**Resposta `200 OK`**

```json
{
  "success": true
}
```

---

## Loja Pública de Configurações (Presets Store)

A loja é o espaço público onde usuários podem publicar, navegar, buscar e carregar configurações de auto acompanhamento feitas pela comunidade.

### `GET /store/presets` *(Público)*

Lista as configurações disponíveis na loja pública. Suporta pesquisa textual, filtro por gênero e ordenação.

**Parâmetros de Query (opcionais):**
- `search`: termo de busca (busca por título, descrição, tags ou autor).
- `genre`: filtro por gênero musical (ex: `Jazz`, `Bossa Nova`, `Lo-Fi`, `Pop`, `Rock`).
- `sortBy`: critério de ordenação (`recent` [padrão] ou `popular`).

**Exemplo:** `GET /store/presets?search=jazz&sortBy=popular`

**Resposta `200 OK`**

```json
{
  "presets": [
    {
      "id": "preset-123",
      "title": "Jazz Club Trio",
      "description": "Progressão II-V-I rica para improviso",
      "genre": "Jazz",
      "tags": "jazz, ii-v-i, swing",
      "bpm": 130,
      "tonality": "Dm",
      "timeSignature": "4/4",
      "downloadsCount": 42,
      "likesCount": 15,
      "authorId": "user-123",
      "authorName": "Miles",
      "createdAt": "2026-11-17T09:00:00Z",
      "updatedAt": "2026-11-17T09:00:00Z"
    }
  ]
}
```

### `GET /store/presets/:id` *(Público)*

Obtém os detalhes completos de uma configuração da loja, incluindo a estrutura serializada de auto acompanhamento (`configurationData`).

**Resposta `200 OK`**

```json
{
  "preset": {
    "id": "preset-123",
    "title": "Jazz Club Trio",
    "description": "Progressão II-V-I rica para improviso",
    "genre": "Jazz",
    "tags": "jazz, ii-v-i, swing",
    "bpm": 130,
    "tonality": "Dm",
    "timeSignature": "4/4",
    "configurationData": [
      {
        "id": "blk-1",
        "name": "Chorus A",
        "bpm": 130,
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
    "downloadsCount": 42,
    "likesCount": 15,
    "authorId": "user-123",
    "authorName": "Miles",
    "createdAt": "2026-11-17T09:00:00Z",
    "updatedAt": "2026-11-17T09:00:00Z"
  }
}
```

### `POST /store/presets/:id/use` *(Público / Autenticado)*

Registra o uso/download da configuração, incrementando o contador da loja.
- Se o usuário estiver autenticado e o parâmetro `createSession=true` for passado, uma nova sessão é criada automaticamente na conta do usuário com os blocos deste auto acompanhamento.

**Parâmetros de Query (opcionais):**
- `createSession`: `true` ou `false` (padrão `false`).

**Resposta `200 OK`**

```json
{
  "preset": {
    "id": "preset-123",
    "title": "Jazz Club Trio",
    "downloadsCount": 43,
    "likesCount": 15,
    "configurationData": [...]
  },
  "createdSession": {
    "id": "sess-new-456",
    "title": "Jazz Club Trio",
    "compositionData": [...]
  }
}
```

### `POST /store/presets` *(Protegido)*

Publica uma configuração diretamente na loja pública. Pode receber os dados de configuração serializados ou o ID de uma sessão existente do usuário.

**Requisição direta**

```json
{
  "title": "Bossa Nova Moderna",
  "description": "Acompanhamento leve de piano e cordas",
  "genre": "Bossa Nova",
  "tags": "bossa, piano, cordas",
  "bpm": 124,
  "tonality": "C",
  "timeSignature": "4/4",
  "configurationData": [
    {
      "id": "blk-1",
      "name": "Intro",
      "bpm": 124,
      "key": "C",
      "timeSignature": "4/4",
      "progression": [
        { "id": "p1", "chord": "Cmaj9", "durationBeats": 4 }
      ],
      "notes": []
    }
  ]
}
```

**Requisição a partir de uma sessão (`sessionId`)**

```json
{
  "sessionId": "abc-123",
  "description": "Publicando meu arranjo da sessão",
  "genre": "Pop",
  "tags": "pop, upbeat"
}
```

**Resposta `201 Created`**

Retorna o objeto `preset` detalhado criado.

### `GET /store/presets/my` *(Protegido)*

Lista todas as configurações da loja publicadas pelo usuário autenticado.

**Resposta `200 OK`**

```json
{
  "presets": [
    {
      "id": "preset-123",
      "title": "Bossa Nova Moderna",
      "genre": "Bossa Nova",
      "downloadsCount": 12,
      "authorId": "user-123",
      "authorName": "Nome do Usuário"
    }
  ]
}
```

### `PATCH /store/presets/:id` *(Protegido)*

Atualiza informações de uma configuração da loja pertencente ao usuário autenticado.

**Requisição**

```json
{
  "title": "Bossa Nova Moderna Rev 2",
  "description": "Atualizado com novos acordes",
  "bpm": 126
}
```

**Resposta `200 OK`**

```json
{
  "preset": { ... }
}
```

### `DELETE /store/presets/:id` *(Protegido)*

Remove uma publicação da loja pertencente ao usuário autenticado.

**Resposta `200 OK`**

```json
{
  "success": true
}
```

---

## Mensagens de Sessão

### `POST /sessions/:id/messages`

Adiciona uma mensagem à sessão.

**Requisição**

```json
{
  "role": "user",
  "content": "Quero experimentar outra progressão"
}
```

**Resposta `201 Created`**

```json
{
  "message": {
    "id": "msg-1",
    "role": "user",
    "content": "Quero experimentar outra progressão",
    "sessionId": "abc-123",
    "createdAt": "2026-10-16T16:30:00Z"
  }
}
```

---

## Erros comuns

- `400 Bad Request`: dados obrigatórios ausentes ou configuração de acompanhamento vazia.
- `401 Unauthorized`: token JWT ausente, inválido ou expirado em rota protegida.
- `404 Not Found`: recurso não encontrado ou sem permissão de acesso.
- `409 Conflict`: email já cadastrado.
