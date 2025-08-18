# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sonic Server is the backend microservices component of the Sonic Cloud Real Machine Platform, built with Spring Boot 3.0.3 and Spring Cloud 2022.0.1. It provides REST APIs, WebSocket endpoints, and scheduled jobs for mobile device testing automation.

## Development Commands

### Build and Package
```bash
# Build all modules from root directory
mvn package

# Build specific module
cd sonic-server-{module} && mvn package

# Clean and rebuild
mvn clean package
```

### Running Services

**Critical startup order (must be followed):**
```bash
# 1. Start Eureka first (service registry)
cd sonic-server-eureka && mvn spring-boot:run

# 2. Start Gateway second (API routing) 
cd sonic-server-gateway && mvn spring-boot:run

# 3. Start Controller and Folder (can run in parallel)
cd sonic-server-controller && mvn spring-boot:run
cd sonic-server-folder && mvn spring-boot:run
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AgentsServiceImplTest

# Run specific test method
mvn test -Dtest=AgentsServiceImplTest#testCreateAgent
```

## Architecture Overview

### Microservices Modules

1. **sonic-server-eureka** (Port: 8761)
   - Netflix Eureka Server for service discovery
   - Spring Security for authentication
   - Uses Undertow instead of Tomcat

2. **sonic-server-gateway** (Port: 3000)
   - Spring Cloud Gateway for API routing
   - JWT authentication via AuthFilter
   - CORS configuration
   - Knife4j API documentation gateway

3. **sonic-server-controller** (Dynamic Port)
   - Main business logic and REST APIs
   - WebSocket endpoints for real-time communication
   - Quartz job scheduling
   - MyBatis Plus with Actable for database operations
   - Feign clients for inter-service communication

4. **sonic-server-folder** (Dynamic Port)
   - File upload/download service
   - Multi-format file support
   - Static file serving

5. **sonic-server-common**
   - Shared utilities, configurations, and models
   - JWT token tools
   - i18n support (5 languages)
   - Common exception handling

### Key Technologies

- **Framework**: Spring Boot 3.0.3, Spring Cloud 2022.0.1
- **Database**: MyBatis Plus 3.5.5 with MySQL 8.0.32
- **Migration**: Actable 1.5.0 for automatic table creation/updates
- **Scheduling**: Quartz with JDBC job store and clustering
- **WebSocket**: Spring WebSocket with Undertow
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Testing**: JUnit 4.13.2, Mockito 5.0.0, ByteBuddy
- **Documentation**: Knife4j/SpringDoc OpenAPI 3

### Database Configuration

- **Actable** automatically creates/updates tables from domain models
- **MyBatis Plus** handles ORM with custom type handlers
- **Quartz** uses JDBC job store with clustering support
- Connection via environment variables: `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`

### Configuration Profiles

Located in `sonic-server-common/src/main/resources/`:
- `application-eureka.yml` - Eureka server settings
- `application-jdbc.yml` - Database and MyBatis configuration  
- `application-feign.yml` - Feign client settings
- `application-logging.yml` - Logging configuration
- `application-user.yml` - User authentication settings
- `application-sonic-server-{service}.yml` - Service-specific configs

## Development Patterns

### Controller Layer
- REST controllers in `controller/` package
- DTOs for request/response in `models/dto/`
- WebSocket endpoints for real-time communication
- Uses `@RestController` and follows REST conventions

### Service Layer
- Service interfaces in `services/` package
- Implementations in `services/impl/` package
- Uses `@Transactional` for database operations
- Feign clients for inter-service communication with fallbacks

### Data Layer
- Domain models in `models/domain/` with Actable annotations
- MyBatis mappers in `mapper/` package
- Custom type handlers for complex data types (arrays, JSON)
- Uses MyBatis Plus for enhanced ORM capabilities

### Configuration Management
- Profile-based configuration with Spring profiles
- Environment variable support for deployment
- Common configurations shared via `sonic-server-common`
- Service-specific configurations in individual modules

### Scheduling and Jobs
- Quartz scheduler with JDBC persistence
- Clustered job execution support
- Job classes in `quartz/` package
- Automatic job scheduling for test execution

### WebSocket Communication
- Real-time device control and monitoring
- Transport layer for agent communication
- Session management and routing

## Docker Deployment

Uses multi-container deployment with `docker-compose.yml`:
- Service dependencies properly configured
- Volume mounts for logs and file storage
- Environment variable configuration
- Network isolation with `sonic-network`

## Testing Strategy

- Unit tests with JUnit 4 and Mockito
- Service layer integration tests  
- WebSocket communication tests
- Test classes follow `{ClassName}Test` pattern
- Mock configurations for external dependencies

## Common Issues & Solutions

- **Service startup order**: Always start eureka → gateway → controller/folder
- **Port conflicts**: Services use dynamic ports (`server.port: 0`) except gateway (3000) and eureka (8761)
- **Database connection**: Check MySQL connectivity if Actable fails to create tables
- **WebSocket issues**: Verify agent can connect to controller transport endpoints
- **Feign circuit breaker**: Check fallback implementations if service calls fail

## Important Development Notes

- Services must register with Eureka before accepting traffic
- Database schema updates are handled automatically by Actable
- Quartz jobs require database persistence for clustering
- WebSocket endpoints use custom transport protocol
- JWT tokens validated at gateway level
- Multi-language support requires property file updates in common module