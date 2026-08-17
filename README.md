# FinPay - Digital Wallet & Ledger Platform

## 🚀 Tecnologias

### Backend
- Java 21
- Spring Boot 3.2.3
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT
- Lombok
- MapStruct
- OpenAPI/Swagger

### Frontend
- React 18
- Material-UI
- React Router
- Recharts
- Axios
- React Hook Form
- Yup
- React Toastify

## 📦 Pré-requisitos

- Docker & Docker Compose
- Java 21 (para desenvolvimento local)
- Node.js 18+ (para desenvolvimento local)
- PostgreSQL 15+ (para desenvolvimento local)

## 🐳 Executando com Docker

1. Clone o repositório:
```bash
git clone https://github.com/yourusername/finpay.git
cd finpay
Execute o Docker Compose:

bash
docker-compose up -d
Acesse:

Frontend: http://localhost:3000

Backend API: http://localhost:8080/api

Swagger UI: http://localhost:8080/api/swagger-ui.html

💻 Desenvolvimento Local
Backend
Configure o PostgreSQL:

sql
CREATE DATABASE finpay;
CREATE USER finpay_user WITH PASSWORD 'finpay_password';
GRANT ALL PRIVILEGES ON DATABASE finpay TO finpay_user;
Execute o projeto:

bash
cd backend
./mvnw spring-boot:run
Frontend
bash
cd frontend
npm install
npm start
🔐 Credenciais Padrão
Admin: admin@finpay.com / Admin@123

User: user@finpay.com / User@123

📁 Estrutura do Projeto
text
finpay/
├── backend/          # Spring Boot API
│   ├── src/main/java/com/finpay/
│   │   ├── config/   # Configurações
│   │   ├── controller/ # Controllers REST
│   │   ├── dto/      # Data Transfer Objects
│   │   ├── entity/   # Entidades JPA
│   │   ├── repository/ # Repositórios
│   │   ├── service/  # Services
│   │   ├── security/ # Security/JWT
│   │   └── exception/ # Exception Handling
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/ # Flyway scripts
├── frontend/         # React App
│   ├── src/
│   │   ├── components/ # React Components
│   │   ├── context/    # Context API
│   │   ├── hooks/      # Custom Hooks
│   │   ├── services/   # API Services
│   │   └── utils/      # Utilities
│   └── public/
└── docker-compose.yml
🔄 Funcionalidades
Backend
✅ Registro e autenticação de usuários (JWT)

✅ Gerenciamento de carteiras digitais

✅ Transferências entre contas

✅ Histórico de transações

✅ Bloqueios pessimistas/otimistas para concorrência

✅ Flyway migrations

✅ Auditoria completa

✅ Tratamento de exceções global

✅ Swagger/OpenAPI

Frontend
✅ Login e registro

✅ Dashboard com gráficos

✅ Visualização da carteira

✅ Transferências com validação

✅ Histórico de transações

✅ Máscaras de moeda

✅ Responsividade

🛡️ Segurança
JWT para autenticação

BCrypt para hash de senhas

CORS configurado

Rate limiting (a ser implementado)

Validação de dados com Jakarta Validation

🧪 Testes
bash
# Backend
cd backend
./mvnw test

# Frontend
cd frontend
npm test
📝 API Endpoints
Auth
POST /api/auth/register - Registrar usuário

POST /api/auth/login - Login

POST /api/auth/logout - Logout

Wallets
GET /api/wallets - Listar todas (Admin)

GET /api/wallets/{id} - Obter carteira

GET /api/wallets/user/{userId} - Obter por usuário

POST /api/wallets/{id}/block - Bloquear (Admin)

POST /api/wallets/{id}/activate - Ativar (Admin)

Transactions
POST /api/transactions/deposit - Depósito

POST /api/transactions/transfer - Transferência

GET /api/transactions/wallet/{walletId} - Listar transações

GET /api/transactions/wallet/{walletId}/balance - Resumo de saldo

POST /api/transactions/{id}/reverse - Reverter transação

🤝 Contribuindo
Fork o projeto

Crie sua branch (git checkout -b feature/AmazingFeature)

Commit suas mudanças (git commit -m 'Add some AmazingFeature')

Push para a branch (git push origin feature/AmazingFeature)

Abra um Pull Request

📄 Licença
Este projeto está sob a licença MIT.