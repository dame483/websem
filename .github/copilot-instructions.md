# WebSem AI Coding Agent Instructions

## Project Overview

WebSem is a **full-stack semantic web application** (Java 17 + Spring Boot, Python FastAPI, React) that combines structured data exploration (SPARQL/DBpedia) with AI-powered conversational interfaces. The system bridges knowledge graphs with LLM capabilities to answer natural language queries.

### Architecture

```
Frontend (React:3000) 
    ↓ HTTP
Java Backend (Spring Boot:8080)
    ├─ REST API (/api/conversation, /api/movies)
    ├─ SPARQL execution against DBpedia
    └─ Fallback AI answers via HTTP → Python backend
    
Python Backend (FastAPI:8000)
    ├─ SPARQL query generation (LLM)
    └─ Fallback answer generation (LLM)
```

**Key Integration Pattern**: Java backend orchestrates the conversation flow; Python backend provides LLM capabilities (SPARQL generation, fallback answers).

---

## Critical Workflows & Commands

### Docker Compose (Recommended)
```bash
docker compose up --build  # Builds and starts all services
# Access: React http://localhost:3000, Java http://localhost:8080, FastAPI http://localhost:8000
```

### Java Backend (Manual)
```bash
cd backend-java
./mvnw clean install
./mvnw spring-boot:run
# Tests: ./mvnw test
```

### Python Backend (Manual)
```bash
cd backend-python
python -m venv env && source env/bin/activate
pip install -r app/requirements.txt
python -m fastapi dev app/main.py
# Requires: .env file with OPENAI_API_KEY (uses Ollama endpoint, not OpenAI)
```

### Frontend (Manual)
```bash
cd frontend-react
npm install && npm start
# Port 3000
```

---

## Essential Code Patterns

### 1. Inter-Backend Communication (Java → Python)
Location: [backend-java/src/main/java/fr/insalyon/websem/service/ConversationService.java](backend-java/src/main/java/fr/insalyon/websem/service/ConversationService.java#L17-L22)

```java
@Value("${backend.python.url:http://backend-python:8000}")
private String pythonBackendUrl;

// Calls Python backend for SPARQL generation
String sparqlQuery = callPythonBackend(question);
```

**Default URL**: `http://backend-python:8000` (Docker), override via property.

### 2. Request/Response DTOs (Java)
Location: [backend-java/src/main/java/fr/insalyon/websem/dto/](backend-java/src/main/java/fr/insalyon/websem/dto/)

- `ConversationRequest`: Simple `question: String`
- `ConversationResponse`: Returns `question`, `sparqlQuery`, `results[]`, `aiAnswer`

Use **Lombok** annotations (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`).

### 3. SPARQL Query Generation (Python)
Location: [backend-python/app/services/openai_services.py](backend-python/app/services/openai_services.py#L11-L40)

**Critical**: Generates only the `WHERE` clause body (not full SPARQL). Uses DBpedia ontology:
- `dbo:` for properties (e.g., `dbo:director`, `dbo:starring`)
- `dbr:` for resources (e.g., `dbr:Christopher_Nolan`)
- Language filters mandatory: `FILTER (lang(?label) = 'en' || lang(?label) = 'fr')`

### 4. SPARQL Execution (Java)
Location: [backend-java/src/main/java/fr/insalyon/websem/service/ConversationService.java](backend-java/src/main/java/fr/insalyon/websem/service/ConversationService.java#L25-L40)

Executes SPARQL queries against DBpedia endpoint (`https://dbpedia.org/sparql`). Gracefully falls back to AI-generated answers if results are empty.

### 5. CORS Configuration (Java)
Location: [backend-java/src/main/java/fr/insalyon/websem/config/CorsConfig.java](backend-java/src/main/java/fr/insalyon/websem/config/CorsConfig.java)

Allows requests from `http://localhost:3000` (React frontend).

---

## Project-Specific Conventions

1. **Packages Structure (Java)**:
   - `controller/`: REST endpoints
   - `service/`: Business logic (inter-backend calls, SPARQL execution)
   - `dto/`: Data transfer objects
   - `config/`: Framework configuration
   - `model/`: Domain entities

2. **Errors & Fallbacks**:
   - Empty SPARQL results → generate AI answer (no knowledge graph data)
   - SPARQL generation failures → fall back to Python `/answer` endpoint
   - Network failures → HTTP exceptions propagate with clear error messages

3. **Dependencies**:
   - Java: **Spring Boot 3.5.9**, **Apache Jena 4.10.0** (SPARQL client)
   - Python: **FastAPI**, **OpenAI SDK** (uses custom Ollama endpoint)
   - Frontend: **React** with **Axios** (HTTP client)

4. **Configuration**:
   - Java: `application.properties` (minimal)
   - Python: `.env` file (OPENAI_API_KEY required)
   - Docker Compose: Handles all inter-service networking

---

## External Dependencies & APIs

- **DBpedia SPARQL Endpoint**: `https://dbpedia.org/sparql` (production data)
- **Ollama LLM API**: `https://ollama-ui.pagoda.liris.cnrs.fr/api` (custom inference server, not OpenAI)
- **Model**: `llama3:70b` (specified in Python backend)

---

## Before Making Changes

1. **Understand the data flow**: User query → Java controller → Python SPARQL generation → DBpedia execution → Fallback AI if empty
2. **Test inter-backend calls**: Ensure Python backend URL is correct (environment-dependent)
3. **SPARQL generation requires expertise**: Modify prompts carefully; always include language filters
4. **Docker takes precedence**: When in doubt about URLs/ports, refer to `docker-compose.yml`
