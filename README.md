# 💳 FinPay - Digital Wallet & Ledger Platform

Plataforma de **carteira digital e gerenciamento de transações financeiras**, desenvolvida com **Java + Spring Boot** no backend e **React** no frontend.

O projeto utiliza autenticação JWT, PostgreSQL, Flyway, controle de concorrência, auditoria e documentação OpenAPI.

---

## 🚀 Tecnologias

### Backend

| Tecnologia        | Utilização               |
| ----------------- | ------------------------ |
| Java 21           | Linguagem principal      |
| Spring Boot 3.2.3 | Framework                |
| Spring Security   | Segurança e autenticação |
| Spring Data JPA   | Persistência             |
| PostgreSQL        | Banco de dados           |
| Flyway            | Migrations               |
| JWT               | Autenticação             |
| Lombok            | Redução de boilerplate   |
| OpenAPI / Swagger | Documentação da API      |

### Frontend

| Tecnologia      | Utilização          |
| --------------- | ------------------- |
| React 18        | Interface           |
| Material UI     | Componentes         |
| React Router    | Navegação           |
| Recharts        | Gráficos            |
| Axios           | Comunicação com API |
| React Hook Form | Formulários         |
| Yup             | Validação           |
| React Toastify  | Notificações        |

---

## 📦 Pré-requisitos

* Docker
* Docker Compose
* Java 21
* Node.js 18+
* PostgreSQL 15+ *(apenas para execução local)*

---

## 🐳 Executando com Docker

### 1. Clone o repositório

```bash
git clone https://github.com/adanwilliamdev/finpay.git
cd finpay
```

### 2. Execute os containers

```bash
docker-compose up -d
```

### 3. Acesse a aplicação

| Serviço     | URL                                         |
| ----------- | ------------------------------------------- |
| Frontend    | `http://localhost:3000`                     |
| Backend API | `http://localhost:8080/api`                 |
| Swagger UI  | `http://localhost:8080/api/swagger-ui.html` |

---

## 💻 Desenvolvimento Local

### Backend

Configure o PostgreSQL:

```sql
CREATE DATABASE finpay;

CREATE USER finpay_user
WITH PASSWORD 'finpay_password';

GRANT ALL PRIVILEGES
ON DATABASE finpay
TO finpay_user;
```

Execute o backend:

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

---

## 🔐 Credenciais de Demonstração

| Perfil | E-mail             | Senha       |
| ------ | ------------------ | ----------- |
| Admin  | `admin@finpay.com` | `Admin@123` |
| User   | `user@finpay.com`  | `User@123`  |

> ⚠️ Essas credenciais são destinadas apenas ao ambiente de desenvolvimento/demonstração.

As contas são criadas na inicialização por um *seeder* (`DemoDataSeeder`), **não** por migration, para que nenhum
ambiente receba um admin com senha pública por acidente. O seeder só roda quando `SEED_DEMO_USERS=true`
(`finpay.seed.demo-users`):

* `docker compose up` → ativado (definido no `docker-compose.yml`);
* profile `dev` (`--spring.profiles.active=dev`) → ativado;
* qualquer outro caso → **desativado** (padrão). Nunca habilite em produção.

É idempotente: contas já existentes não são alteradas. O usuário `user@finpay.com` começa com R$ 1.000,00
(registrado como um depósito no ledger) e a carteira do admin começa zerada.

---

## 📁 Estrutura do Projeto

```text
finpay/
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/finpay/
│   │       │   ├── config/
│   │       │   ├── controller/
│   │       │   ├── dto/
│   │       │   ├── entity/
│   │       │   ├── repository/
│   │       │   ├── service/
│   │       │   ├── security/
│   │       │   └── exception/
│   │       │
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/migration/
│   │
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── context/
│   │   ├── hooks/
│   │   ├── services/
│   │   └── utils/
│   └── public/
│
└── docker-compose.yml
```

---

## 🔄 Funcionalidades

### Backend

* ✅ Registro e autenticação de usuários com JWT
* ✅ Gerenciamento de carteiras digitais
* ✅ Transferências entre contas
* ✅ Histórico de transações
* ✅ Controle de concorrência
* ✅ Bloqueios pessimistas e otimistas
* ✅ Migrations com Flyway
* ✅ Auditoria de operações
* ✅ Tratamento global de exceções
* ✅ Documentação com Swagger/OpenAPI

### Frontend

* ✅ Login e registro
* ✅ Dashboard financeiro
* ✅ Gráficos e indicadores
* ✅ Visualização da carteira
* ✅ Transferências com validação
* ✅ Histórico de transações
* ✅ Máscaras de moeda
* ✅ Interface responsiva

---

## 🛡️ Segurança

* 🔐 Autenticação baseada em JWT
* 🔑 Senhas protegidas com BCrypt
* 🌐 CORS configurado
* 🛡️ Validação de dados com Jakarta Validation
* 🔒 Controle de concorrência em operações financeiras
* 🚧 Rate limiting *(planejado)*

---

## 🧪 Testes

### Backend

```bash
cd backend
./mvnw test
```

### Frontend

```bash
cd frontend
npm test
```

---

## 📝 API Endpoints

### 🔑 Authentication

| Método | Endpoint             | Descrição         |
| ------ | -------------------- | ----------------- |
| `POST` | `/api/auth/register` | Registrar usuário |
| `POST` | `/api/auth/login`    | Realizar login    |
| `POST` | `/api/auth/logout`   | Realizar logout   |

### 💰 Wallets

| Método | Endpoint                     | Descrição                   |
| ------ | ---------------------------- | --------------------------- |
| `GET`  | `/api/wallets`               | Listar carteiras *(Admin)*  |
| `GET`  | `/api/wallets/{id}`          | Obter carteira              |
| `GET`  | `/api/wallets/user/{userId}` | Obter carteira por usuário  |
| `POST` | `/api/wallets/{id}/block`    | Bloquear carteira *(Admin)* |
| `POST` | `/api/wallets/{id}/activate` | Ativar carteira *(Admin)*   |

### 💸 Transactions

| Método | Endpoint                                      | Descrição              |
| ------ | --------------------------------------------- | ---------------------- |
| `POST` | `/api/transactions/deposit`                   | Realizar depósito      |
| `POST` | `/api/transactions/transfer`                  | Realizar transferência |
| `GET`  | `/api/transactions/wallet/{walletId}`         | Listar transações      |
| `GET`  | `/api/transactions/wallet/{walletId}/balance` | Consultar saldo        |
| `POST` | `/api/transactions/{id}/reverse`              | Reverter transação     |

---

### ↩️ Regras de reversão

`POST /api/transactions/{id}/reverse` devolve o dinheiro e marca a transação original como `REVERSED`,
gerando uma transação `REVERSAL` (origem = carteira debitada, destino = carteira creditada).

| Quem                                         | Pode reverter                                                                 |
| -------------------------------------------- | ----------------------------------------------------------------------------- |
| `ADMIN`                                      | Qualquer depósito, transferência ou saque `COMPLETED`                         |
| Remetente (dono da carteira de origem)       | Transferência/saque próprios, em até `finpay.reversal.user-window-minutes` (padrão 30 min, env `REVERSAL_USER_WINDOW_MINUTES`) |
| Destinatário de uma transferência            | ❌ Nunca (`403`)                                                              |
| Dono de um depósito                          | ❌ Depósitos só podem ser revertidos por admin (`403`)                        |

Rejeições de regra de negócio retornam `409 Conflict` com mensagem explícita: transação já revertida, uma
`REVERSAL` não pode ser revertida, janela expirada, carteira fechada, ou destinatário que já gastou o valor.
A transação e todas as carteiras envolvidas são bloqueadas (`SELECT … FOR UPDATE`, carteiras em ordem de ID)
antes de qualquer alteração, então reversões e transferências concorrentes não geram corrida nem deadlock.

### ❌ Tentativas falhas

Transferências e depósitos rejeitados por regra de negócio (saldo insuficiente, carteira inativa, moedas
diferentes) deixam um registro `FAILED` no ledger (e no audit log) com o motivo na descrição; nenhum saldo é
alterado. Esses registros só aparecem no extrato da carteira **de origem**. O status `PENDING` segue reservado
para fluxos assíncronos futuros.

### 📊 Resumo de saldo

`GET /api/transactions/wallet/{id}/balance` retorna `totalDeposits`, `totalWithdrawals`, `totalTransfersOut`
(enviadas), `totalTransfersIn` (recebidas) e `totalTransactions`, calculados com `SUM`/`COUNT` no banco
(somente transações `COMPLETED`). *Mudança:* o campo `totalTransfers` foi substituído por `totalTransfersOut`
e `totalTransfersIn`.

## 🤝 Contribuindo

1. Faça um fork do projeto.
2. Crie uma branch:

```bash
git checkout -b feature/AmazingFeature
```

3. Faça o commit:

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

Este projeto está sob a licença **MIT**.
