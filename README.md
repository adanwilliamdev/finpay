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
| MapStruct         | Mapeamento DTO           |
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
git clone https://github.com/yourusername/finpay.git
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
