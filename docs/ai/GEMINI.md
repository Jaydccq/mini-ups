# Mini-UPS Project

This project is a miniature version of a UPS (United Parcel Service) system. It is a full-stack application with a Java backend, a React frontend, and a PostgreSQL database.

## Project Structure

- `backend/`: Spring Boot application providing the API.
- `frontend/`: React application for the user interface.
- `database/`: SQL scripts for database schema creation and initialization.
- `proto/`: Protocol Buffer definitions for communication between services.
- `aws/`: Scripts and configuration for AWS deployment.
- `docker-compose.yml`: Docker Compose file for local development.

## Key Technologies

- **Backend**: Java, Spring Boot, Maven, PostgreSQL, Protobuf
- **Frontend**: React, TypeScript, Vite, Tailwind CSS
- **Database**: PostgreSQL
- **Deployment**: Docker, AWS (ECS, CloudFormation)

## Local Development (Java Backend)

To run the Java backend locally without full Docker:

```bash
# Step 1: Start infrastructure services only
cd /Users/hongxichen/Desktop/mini-ups
docker compose -f docker-compose.local.yml up -d

# Step 2: Run Java backend
cd backend
./run-local.sh

# Or auto-start infrastructure with the script
./run-local.sh --auto-start
```

**Endpoints:**
- Backend: http://localhost:8081
- Swagger UI: http://localhost:8081/swagger-ui.html
- RabbitMQ: http://localhost:15672 (guest/guest)
