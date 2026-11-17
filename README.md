# Requiem API 🎵

> Backend inteligente para o sistema de **auto acompanhamento musical** do Requiem.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migration-red.svg)](https://flywaydb.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📌 Sobre o Projeto

O **Requiem** é uma aplicação voltada para músicos, compositores e improvisadores, atuando como um sistema de **auto acompanhamento inteligente** em tempo real.

A **Requiem API** gerencia o ecossistema de dados da plataforma:
- **Autenticação & Segurança**: Registro, login e validação de tokens JWT com criptografia BCrypt.
- **Sessões de Acompanhamento**: Persistência de arranjos e blocos musicais (`compositionData` com acordes, notas detectadas, BPM, tom e compasso) estruturados em JSONB.
- **Exportação & Serialização**: Exportação de configurações de acompanhamento para backups ou compartilhamento.
- **Preset Store (Loja Comunitária)**: Catálogo público para descoberta, busca, compartilhamento e importação de configurações de acompanhamento criadas pela comunidade.
- **Assistente Harmônico**: Histórico de mensagens contextuais integradas à sessão.

> [!NOTE]
> O Requiem foca integralmente no fluxo de auto acompanhamento dinâmico. A API não realiza exportação ou conversão de arquivos MIDI brutos, trabalhando diretamente com a serialização e desserialização das configurações musicais.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Java 21 (LTS)
- **Framework:** Spring Boot 3.3.5
  - *Spring Web*: Criação de endpoints RESTful
  - *Spring Data JPA*: Abstração de persistência com Hibernate
  - *Spring Security*: Controle de autenticação e filtros de segurança stateless
  - *Spring Validation*: Validação declarativa de DTOs com Bean Validation
- **Banco de Dados:** PostgreSQL 16 com suporte a tipos `JSONB`
- **Controle de Versão do Banco:** Flyway Migrations
- **Autenticação:** JSON Web Token (JJWT 0.12.6)
- **Produtividade:** Project Lombok
- **Contêineres:** Docker & Docker Compose
- **Testes:** JUnit 5, Mockito, AssertJ, Spring Test / MockMvc

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- **Java 21** instalado ([SDKMAN!](https://sdkman.io/) recomendado)
- **Docker** e **Docker Compose**
- **Git**

### 1. Clonar o Repositório
```bash
git clone https://github.com/kaiquefreire05/requem-api.git
cd requem-api
```

### 2. Subir o Banco de Dados com Docker
Utilize o `compose.yaml` incluído para iniciar uma instância do PostgreSQL:
```bash
docker compose up -d
```
O banco iniciará na porta `5432` com usuário `postgres` e banco `requiem_db`.

### 3. Configurar Variáveis de Ambiente (Opcional)
Você pode criar um arquivo `.env` na raiz do projeto (ou configurar as variáveis no ambiente):

```properties
PORT=3001
DATABASE_URL=jdbc:postgresql://localhost:5432/requiem_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=sua-chave-secreta-jwt-com-no-minimo-256-bits-de-seguranca
FRONTEND_URL=http://localhost:5173
```

### 4. Executar a Aplicação
Execute através do wrapper Maven:
```bash
./mvnw spring-boot:run
```

A API estará disponível em:
👉 `http://localhost:3001/api`

---

## 🧪 Executando os Testes

O projeto conta com uma suíte de testes unitários e de integração abrangendo controllers, services, repositories e regras de segurança:

```bash
# Rodar todos os testes
./mvnw test

# Limpar e rodar com relatório completo
./mvnw clean test
```

---

## 📂 Estrutura de Diretórios

```
requiem-api/
├── .mvn/wrapper/               # Binários do Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/requiem/api/
│   │   │   ├── config/         # Configurações de Segurança e CORS
│   │   │   ├── controller/     # Controladores REST (Auth, Sessions, Store, Health)
│   │   │   ├── dto/            # Objetos de Transferência de Dados (auth, session, store)
│   │   │   ├── entity/         # Entidades JPA (User, ChatSession, Message, StorePreset)
│   │   │   ├── exception/      # Handler global de exceções
│   │   │   ├── repository/     # Interfaces Spring Data JPA
│   │   │   ├── security/       # Serviço JWT e filtros de autenticação
│   │   │   └── service/        # Regras de negócio e lógica de aplicação
│   │   └── resources/
│   │       ├── db/migration/   # Scripts versionados de migração Flyway (V1, V2, V3)
│   │       └── application.properties
│   └── test/                   # Bateria completa de testes automatizados
├── API_ENDPOINTS.md            # Documentação técnica detalhada com payloads
├── compose.yaml                # Subida de contêineres Docker
├── Dockerfile                  # Imagem de produção
├── pom.xml                     # Configurações de dependências e plugins Maven
└── README.md                   # Apresentação do projeto
```

---

## 🌐 Resumo dos Endpoints

| Método | Endpoint | Acesso | Descrição |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Público | Cadastra um novo usuário e retorna token JWT |
| `POST` | `/api/auth/login` | Público | Autentica usuário existente |
| `GET` | `/api/auth/me` | Protegido | Retorna o perfil do usuário logado |
| `GET` | `/api/health` | Público | Verificação de status do servidor |
| `POST` | `/api/sessions` | Protegido | Cria uma nova sessão de auto acompanhamento |
| `GET` | `/api/sessions` | Protegido | Lista as sessões do usuário autenticado |
| `GET` | `/api/sessions/:id` | Protegido | Retorna uma sessão completa com blocos e mensagens |
| `PATCH` | `/api/sessions/:id` | Protegido | Salva blocos do acompanhamento (`compositionData`) ou renomeia |
| `DELETE`| `/api/sessions/:id` | Protegido | Remove a sessão |
| `GET` | `/api/sessions/:id/export` | Protegido | Exporta e serializa a configuração da sessão em JSON |
| `POST` | `/api/sessions/:id/export-to-store` | Protegido | Exporta a sessão atual diretamente para a Loja Pública |
| `POST` | `/api/sessions/:id/messages` | Protegido | Registra mensagens trocadas na sessão |
| `GET` | `/api/store/presets` | Público | Lista configurações públicas (busca, gênero e ordenação) |
| `GET` | `/api/store/presets/:id` | Público | Obtém detalhes e blocos serializados do preset |
| `POST` | `/api/store/presets/:id/use` | Público / Protegido | Registra download e opcionalmente cria sessão pronta (`?createSession=true`) |
| `POST` | `/api/store/presets` | Protegido | Publica uma configuração na loja |
| `GET` | `/api/store/presets/my` | Protegido | Lista os presets publicados pelo usuário logado |
| `PATCH` | `/api/store/presets/:id` | Protegido | Atualiza dados do preset publicado |
| `DELETE`| `/api/store/presets/:id` | Protegido | Remove o preset da loja |

> Para conferir os exemplos completos de payload, estruturas JSON e comandos cURL, consulte a [Documentação de Endpoints](API_ENDPOINTS.md).

---

## 📄 Licença

Este projeto é distribuído sob a licença [MIT](LICENSE).
