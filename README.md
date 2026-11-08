# 💳 FinPay — Digital Wallet & Ledger Platform

<p align="center">
  <strong>Plataforma de carteira digital e gerenciamento de transações financeiras.</strong>
  <br>
  Desenvolvida com Java 21, Spring Boot e React, com foco em segurança, integridade financeira e consistência de dados.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.3-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black" alt="React 18">
  <img src="https://img.shields.io/badge/PostgreSQL-Database-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Docker-Containerized-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="MIT License">
</p>

---

## 📌 Sobre o Projeto

O **FinPay** é uma plataforma full stack de carteira digital desenvolvida para simular operações financeiras de uma aplicação bancária moderna.

O sistema permite o gerenciamento de carteiras, depósitos, transferências entre usuários e acompanhamento de movimentações financeiras por meio de um ledger transacional.

O projeto foi desenvolvido com atenção especial à **integridade dos dados financeiros**, utilizando mecanismos de controle de concorrência, bloqueios de registros no banco de dados, auditoria e regras de reversão de operações.

### 🎯 Principais diferenciais

- Arquitetura Full Stack com Java, Spring Boot e React.
- Autenticação e autorização utilizando JWT.
- Ledger para registro e rastreabilidade de operações financeiras.
- Controle de concorrência com bloqueios pessimistas e otimistas.
- Reversão de transações com validação de regras de negócio.
- Idempotência (`Idempotency-Key`) em depósitos, saques e transferências.
- Auditoria de operações e rastreamento de falhas.
- Versionamento do banco de dados com Flyway.
- Documentação da API com Swagger/OpenAPI.
- Testes de integração com PostgreSQL real (Testcontainers), incluindo cenários de concorrência, e CI com GitHub Actions.

---

## 🚀 Tecnologias

### Backend

| Tecnologia | Utilização |
|---|---|
| Java 21 | Linguagem principal |
| Spring Boot 3.2.3 | Framework backend |
| Spring Security | Segurança e autenticação |
| Spring Data JPA | Persistência de dados |
| PostgreSQL | Banco de dados relacional |
| Flyway | Versionamento e migrations |
| JWT | Autenticação baseada em tokens |
| Lombok | Redução de código boilerplate |
| Jakarta Validation | Validação de dados |
| OpenAPI / Swagger | Documentação da API |

### Frontend

| Tecnologia | Utilização |
|---|---|
| React 18 | Biblioteca de interface |
| Material UI | Componentes visuais |
| React Router | Navegação |
| Recharts | Gráficos e indicadores |
| Axios | Comunicação HTTP com a API |
| React Hook Form | Gerenciamento de formulários |
| Yup | Validação de formulários |
| React Toastify | Notificações |

### Infraestrutura e ferramentas

<p>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker">
  <img src="https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker Compose">
  <img src="https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven">
  <img src="https://img.shields.io/badge/Git-F05032?style=flat-square&logo=git&logoColor=white" alt="Git">
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black" alt="Swagger">
</p>

---

## ✨ Funcionalidades

### 💰 Gestão financeira

- Cadastro e gerenciamento de usuários.
- Criação e gerenciamento de carteiras digitais.
- Depósitos e saques em carteiras.
- Transferências entre usuários.
- Consulta de saldo.
- Histórico completo de transações.
- Reversão de operações financeiras.
- Registro de operações rejeitadas.

### 📊 Dashboard financeiro

- Visualização do saldo disponível.
- Indicadores de movimentação financeira.
- Gráficos de depósitos e transferências.
- Histórico de movimentações.
- Interface responsiva.

### 🔐 Segurança

- Autenticação utilizando JWT (expiração padrão de 2 horas, configurável).
- Logout com revogação do token no servidor (`jti`).
- A aplicação não inicia sem um `JWT_SECRET` válido (mínimo de 64 caracteres, exigido pelo HS512).
- Criptografia de senhas com BCrypt.
- Controle de acesso por perfil (ADMIN e USER).
- Configuração de CORS.
- Validação de dados com Jakarta Validation.
- Tratamento global de exceções.
- Proteção de operações financeiras concorrentes.

---

## 🧠 Diferenciais Técnicos

### 1. Ledger financeiro

Todas as operações financeiras são registradas no ledger, garantindo rastreabilidade das movimentações.

O sistema registra depósitos, transferências, reversões e tentativas de operações rejeitadas.

Os registros de operações malsucedidas recebem status `FAILED`, incluindo o motivo da rejeição, sem modificar os saldos das carteiras.

### 2. Controle de concorrência

O sistema utiliza mecanismos de controle de concorrência para proteger operações financeiras simultâneas.

- **Pessimistic Lock:** bloqueio de registros utilizando `SELECT FOR UPDATE`.
- **Optimistic Lock:** controle de alterações concorrentes.
- **Ordenação de bloqueios:** carteiras bloqueadas em ordem de ID para reduzir riscos de deadlock.
- **Transações atômicas:** operações financeiras executadas dentro de transações de banco de dados.

### 3. Reversão de transações

O FinPay implementa um mecanismo de reversão financeira com regras específicas de autorização.

| Perfil | Permissão |
|---|---|
| ADMIN | Pode reverter depósitos, transferências e saques concluídos |
| Remetente | Pode reverter suas transferências e saques dentro da janela permitida |
| Destinatário | Não pode reverter transferências recebidas |
| Usuário comum | Não pode reverter depósitos |

A reversão:

- Marca a transação original como `REVERSED`.
- Cria uma nova transação do tipo `REVERSAL`.
- Bloqueia as carteiras envolvidas antes de modificar os saldos.
- Verifica se o valor recebido ainda está disponível.
- Impede reversões duplicadas.

Por padrão, usuários comuns possuem uma janela de reversão de 30 minutos, configurável por variável de ambiente.

Rejeições por regras de negócio retornam `409 Conflict`, enquanto operações sem autorização retornam `403 Forbidden`.

### 4. Auditoria e integridade

- Registro de operações financeiras.
- Histórico de operações rejeitadas.
- Rastreabilidade das reversões.
- Validação de saldo antes das movimentações.
- Integridade dos registros por meio de transações de banco de dados.

### 5. Resumo financeiro

O endpoint de saldo calcula os indicadores diretamente no banco de dados, considerando somente transações com status `COMPLETED`.

Indicadores disponíveis:

- `totalDeposits`
- `totalWithdrawals`
- `totalTransfersOut`
- `totalTransfersIn`
- `totalTransactions`

Os valores de transferências enviadas e recebidas são apresentados separadamente.

### 6. Idempotência

`POST /transactions/deposit`, `/withdraw` e `/transfer` aceitam o header opcional `Idempotency-Key` (até 100 caracteres).

| Situação | Resultado |
|---|---|
| Primeira requisição com a chave | Processada normalmente; o resultado é guardado |
| Repetição com a mesma chave e o mesmo corpo | Devolve o resultado original, **sem movimentar saldo de novo** |
| Mesma chave com corpo diferente | `422 Unprocessable Entity` |
| Requisições simultâneas com a mesma chave | Apenas uma movimenta o saldo; as demais recebem o mesmo resultado |
| Operação rejeitada por regra de negócio | Não é guardada; o cliente pode tentar de novo com a mesma chave |

Como funciona: a chave é gravada **na mesma transação de banco** que move o dinheiro (tabela `idempotency_keys`, única por usuário + chave). Se duas requisições disputam a mesma chave, a restrição de unicidade desfaz a segunda e ela devolve o resultado da primeira. As chaves são mantidas por 24h (`IDEMPOTENCY_RETENTION_HOURS`). O frontend já envia a chave nas transferências.

---

## 🏗️ Arquitetura do Projeto

```text
finpay/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/finpay/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   ├── service/
│   │   │   │   ├── security/
│   │   │   │   └── exception/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/
│   │   └── test/
│   │       ├── java/com/finpay/        # testes unitários e de integração
│   │       └── resources/application-test.yml
│   └── pom.xml
│
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   ├── context/
│   │   ├── hooks/
│   │   ├── services/
│   │   └── utils/
│   └── package.json
│
├── .github/workflows/ci.yml
├── .env.example
└── docker-compose.yml
```

---

## ⚙️ Executando o Projeto

### Pré-requisitos

- Docker e Docker Compose
- Java 21 e Maven 3.9+ (para rodar o backend localmente)
- Node.js 18+ (para rodar o frontend localmente)
- PostgreSQL 15+ (para execução local)

### 🐳 Executando com Docker

**1. Clone o repositório**

```bash
git clone https://github.com/adanwilliamdev/finpay.git
```

**2. Acesse o diretório**

```bash
cd finpay
```

**3. Configure as variáveis de ambiente**

O backend **não inicia sem `JWT_SECRET`** e o Compose exige `POSTGRES_PASSWORD`. Nenhum segredo fica versionado:

```bash
cp .env.example .env
# edite o .env: defina POSTGRES_PASSWORD e gere o JWT_SECRET com
openssl rand -hex 32
```

**4. Inicie os containers**

```bash
docker compose up -d --build
```

**5. Acesse a aplicação**

| Serviço | Endereço |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |

### 💻 Executando localmente

#### Backend

Configure o banco de dados PostgreSQL:

```sql
CREATE DATABASE finpay;

CREATE USER finpay_user
WITH PASSWORD 'finpay_password';

GRANT ALL PRIVILEGES
ON DATABASE finpay
TO finpay_user;
```

Execute o backend com o profile `dev` (já traz um `JWT_SECRET` de desenvolvimento, usuários demo e logs detalhados):

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Sem o profile `dev`, defina as variáveis antes de subir:

```bash
export JWT_SECRET=$(openssl rand -hex 32)
mvn spring-boot:run
```

> Para adicionar o Maven Wrapper ao projeto, rode `mvn wrapper:wrapper` dentro de `backend/` e versione os arquivos gerados.

#### Frontend

Em outro terminal:

```bash
cd frontend
npm install
npm start
```

---

## ⚙️ Variáveis de Ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `JWT_SECRET` | **Sim** | — | Chave de assinatura JWT (mínimo 64 caracteres) |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Não | `localhost` / `finpay_user` / `finpay_password` | Conexão com o PostgreSQL |
| `JWT_EXPIRATION_MS` | Não | `7200000` (2h) | Validade do token |
| `REVERSAL_USER_WINDOW_MINUTES` | Não | `30` | Janela para o remetente reverter uma operação |
| `IDEMPOTENCY_RETENTION_HOURS` | Não | `24` | Por quanto tempo uma `Idempotency-Key` é lembrada |
| `CORS_ALLOWED_ORIGINS` | Não | `http://localhost:3000,http://localhost:3001` | Origens permitidas, separadas por vírgula |
| `SEED_DEMO_USERS` | Não | `false` | Cria as contas de demonstração |

---

## 🔑 Credenciais de Demonstração

O projeto disponibiliza dois perfis para testes:

| Perfil | E-mail | Senha |
|---|---|---|
| ADMIN | `admin@finpay.com` | `Admin@123` |
| USER | `user@finpay.com` | `User@123` |

> ⚠️ Credenciais destinadas exclusivamente a ambientes de desenvolvimento e demonstração.

### Configuração do Seeder

As contas de demonstração são criadas pelo `DemoDataSeeder`, e não por migrations.

A inicialização depende da configuração `SEED_DEMO_USERS=true` (`finpay.seed.demo-users`).

| Ambiente | Comportamento |
|---|---|
| Docker Compose | Ativado |
| Profile `dev` | Ativado |
| Outros ambientes | Desativado por padrão |

O seeder é idempotente: usuários existentes não têm suas informações alteradas.

**Saldos iniciais:**
- USER: R$ 1.000,00, registrados como depósito no ledger.
- ADMIN: R$ 0,00.

Nunca habilite o seeder de demonstração em produção.

---

## 📡 API Endpoints

### 🔐 Authentication

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/auth/register` | Registrar usuário |
| `POST` | `/api/auth/login` | Realizar login |
| `POST` | `/api/auth/logout` | Realizar logout (revoga o token) |

### 💳 Wallets

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/wallets` | Listar carteiras (ADMIN) |
| `GET` | `/api/wallets/{id}` | Consultar carteira |
| `GET` | `/api/wallets/user/{userId}` | Consultar carteira por usuário |
| `POST` | `/api/wallets/{id}/block` | Bloquear carteira (ADMIN) |
| `POST` | `/api/wallets/{id}/activate` | Ativar carteira (ADMIN) |

### 💸 Transactions

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/transactions/deposit` | Realizar depósito (aceita `Idempotency-Key`) |
| `POST` | `/api/transactions/withdraw` | Realizar saque (aceita `Idempotency-Key`) |
| `POST` | `/api/transactions/transfer` | Realizar transferência (aceita `Idempotency-Key`) |
| `GET` | `/api/transactions/wallet/{walletId}` | Listar transações |
| `GET` | `/api/transactions/wallet/{walletId}/balance` | Consultar saldo e indicadores |
| `POST` | `/api/transactions/{id}/reverse` | Reverter transação |

---

## 🧪 Testes

### Backend

```bash
cd backend
mvn test      # ou: mvn verify
```

Os **testes de integração usam PostgreSQL real via Testcontainers**, então precisam de Docker em execução. Sem Docker eles são ignorados (os unitários continuam rodando). Um mesmo container é reaproveitado por todas as classes de teste.

| Teste | O que garante |
|---|---|
| `TransactionConcurrencyTest` | Transferências opostas simultâneas conservam o dinheiro e não geram deadlock; disputa por saldo nunca deixa a carteira negativa e registra as rejeições como `FAILED`; reversões simultâneas da mesma transação: só uma vence; retentativa com a mesma `Idempotency-Key` move o dinheiro uma vez só (inclusive em requisições simultâneas); regras de autorização de reversão, saque e transferência |
| `TransactionApiTest` | Fluxo HTTP completo com a cadeia de segurança real: registro, JWT, logout revogando o token, `Idempotency-Key`, códigos 400/401/403/422 |
| `WalletTest` | Regras de crédito/débito da entidade e ausência de recursão em `equals`/`hashCode` |
| `JwtTokenProviderTest` / `TokenBlacklistTest` | Segredo fraco impede a inicialização, `jti` único por token, token adulterado/expirado rejeitado, revogação |
| `IdempotencyServiceTest` | Normalização da chave e hash do corpo da requisição |

### Frontend

```bash
cd frontend
npm test -- --watchAll=false
```

### CI

O workflow `.github/workflows/ci.yml` roda os testes do backend (com Docker/Testcontainers), os testes do frontend e o build do frontend a cada push e pull request.

---

## 🗺️ Melhorias Futuras

- [x] Idempotência de requisições financeiras.
- [x] Testes de concorrência.
- [x] Pipeline de CI (GitHub Actions).
- [ ] Rate limiting (principalmente em `/auth/login`).
- [ ] Refresh token (hoje o token dura 2h e é revogado apenas no logout).
- [ ] Lista de tokens revogados em Redis (hoje é em memória: vale por instância e se perde ao reiniciar).
- [ ] Deploy automatizado (CD).
- [ ] Integração com serviços externos de pagamento.
- [ ] Notificações de movimentações financeiras.
- [ ] Testes de carga.

---

## 🤝 Contribuindo

Contribuições são bem-vindas!

1. Faça um fork do projeto.
2. Crie uma branch para sua funcionalidade:

```bash
git checkout -b feature/AmazingFeature
```

3. Faça suas alterações e commit:

```bash
git commit -m "Add AmazingFeature"
```

4. Envie para o repositório:

```bash
git push origin feature/AmazingFeature
```

5. Abra um Pull Request.

---

## 📄 Licença

Este projeto está licenciado sob a **MIT License**.

---

<p align="center">
  Desenvolvido por <strong>Adan William</strong>
  <br>
  <a href="https://github.com/adanwilliamdev">GitHub</a>
  &nbsp;•&nbsp;
  <a href="https://adanwilliamdev.github.io/">Portfólio</a>
</p>
