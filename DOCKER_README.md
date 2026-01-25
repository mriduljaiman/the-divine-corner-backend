# Divine Corner Backend - Docker Setup

This directory contains Docker configuration for deploying the Divine Corner Spring Boot backend.

## Files Overview

- **Dockerfile** - Multi-stage Docker build for production
- **docker-compose.yml** - Complete local development stack (MySQL + Redis + Backend)
- **.dockerignore** - Excludes unnecessary files from Docker build
- **render.yaml** - Render.com deployment configuration
- **.env.example** - Example environment variables
- **application-prod.yaml** - Production Spring Boot configuration

## Quick Start

### Local Development with Docker Compose

1. **Copy environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your local settings
   ```

2. **Start all services**
   ```bash
   docker-compose up --build
   ```

   This will start:
   - MySQL on port 3307
   - Redis on port 6380
   - Backend on port 8080

3. **Access the API**
   ```
   http://localhost:8080/api/actuator/health
   ```

4. **Stop services**
   ```bash
   docker-compose down
   ```

5. **Clean up volumes**
   ```bash
   docker-compose down -v
   ```

### Build Docker Image Only

**Linux/Mac:**
```bash
chmod +x build-docker.sh
./build-docker.sh
```

**Windows:**
```bash
build-docker.bat
```

**Or manually:**
```bash
docker build -t divine-corner-backend .
```

### Run Single Container

```bash
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:mysql://host.docker.internal:3306/divinecorner \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=root \
  -e JWT_ACCESS_SECRET=your-secret \
  -e JWT_REFRESH_SECRET=your-secret \
  divine-corner-backend
```

## Environment Variables

See `.env.example` for all required variables.

### Required for Production

```bash
DATABASE_URL              # MySQL connection string
DB_USERNAME               # Database username
DB_PASSWORD               # Database password
JWT_ACCESS_SECRET         # JWT signing secret (min 256 bits)
JWT_REFRESH_SECRET        # JWT refresh token secret
ALLOWED_ORIGINS           # CORS allowed origins (comma-separated)
ADMIN_PASSWORD            # Admin user password
APP_BASE_URL              # Backend base URL
```

### Optional

```bash
REDIS_HOST                # Redis host (default: localhost)
REDIS_PORT                # Redis port (default: 6379)
REDIS_PASSWORD            # Redis password (if any)
FILE_UPLOAD_DIR           # File upload directory
COOKIE_DOMAIN             # Cookie domain
COOKIE_SECURE             # Cookie secure flag (true/false)
```

## Deployment to Render

See [RENDER_DEPLOYMENT_GUIDE.md](../../RENDER_DEPLOYMENT_GUIDE.md) for complete deployment instructions.

### Quick Deploy to Render

1. **Push to Git**
   ```bash
   git add .
   git commit -m "Add Docker configuration"
   git push origin main
   ```

2. **Create New Web Service on Render**
   - Environment: Docker
   - Dockerfile Path: `./Dockerfile`
   - Health Check Path: `/api/actuator/health`

3. **Set Environment Variables** (see guide)

4. **Deploy**

Your backend will be available at: `https://your-service.onrender.com/api`

## Docker Image Details

### Multi-Stage Build

1. **Build Stage**
   - Uses Maven 3.9 with JDK 17
   - Downloads dependencies
   - Compiles source code
   - Creates JAR file

2. **Runtime Stage**
   - Uses JRE 17 Alpine (smaller image)
   - Creates non-root user
   - Copies JAR from build stage
   - Exposes port 8080
   - Includes health check

### Image Size

- Build stage: ~800 MB (discarded)
- Final image: ~200-250 MB

### Health Check

The image includes a built-in health check:
- **Endpoint**: `http://localhost:8080/actuator/health`
- **Interval**: 30 seconds
- **Timeout**: 3 seconds
- **Retries**: 3

## Volumes

### Local Development (docker-compose)

- `mysql-data` - MySQL database files
- `redis-data` - Redis persistence
- `uploads-data` - Uploaded files

### Production

Create a persistent disk for uploads:
```bash
# In Render or your cloud provider
Mount: /app/uploads
Size: 1 GB (or as needed)
```

## Networking

### Docker Compose Networking

All services are on the same Docker network and can communicate using service names:
- Backend connects to MySQL at `mysql:3306`
- Backend connects to Redis at `redis:6379`

### Port Mapping

- MySQL: `3307` (host) → `3306` (container)
- Redis: `6380` (host) → `6379` (container)
- Backend: `8080` (host) → `8080` (container)

## Troubleshooting

### Build Fails

1. **Check Java version**
   ```bash
   docker run --rm eclipse-temurin:17-jre-alpine java -version
   ```

2. **Check Maven dependencies**
   ```bash
   mvn dependency:tree
   ```

3. **Clean build**
   ```bash
   docker-compose down -v
   docker-compose build --no-cache
   ```

### Container Crashes

1. **Check logs**
   ```bash
   docker-compose logs backend
   ```

2. **Increase memory**
   ```yaml
   # In docker-compose.yml
   backend:
     deploy:
       resources:
         limits:
           memory: 1G
   ```

### Database Connection Issues

1. **Check MySQL is running**
   ```bash
   docker-compose ps
   ```

2. **Test connection**
   ```bash
   docker-compose exec mysql mysql -u root -p
   ```

3. **Check connection string**
   ```bash
   docker-compose exec backend env | grep DATABASE
   ```

### Redis Connection Issues

1. **Test Redis**
   ```bash
   docker-compose exec redis redis-cli ping
   ```

2. **Check connection**
   ```bash
   docker-compose logs redis
   ```

## Performance Optimization

### Database Connection Pool

In `application-prod.yaml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

### JVM Options

Add to Dockerfile ENTRYPOINT:
```dockerfile
ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx512m", \
  "-XX:+UseG1GC", \
  "-jar", \
  "app.jar"]
```

### Build Time

- First build: ~5-10 minutes
- Subsequent builds: ~2-3 minutes (cached layers)

## Security

### Non-Root User

The Docker image runs as a non-root user `spring:spring` for security.

### Secrets Management

Never commit secrets to Git:
- Use environment variables
- Use secrets management (AWS Secrets Manager, etc.)
- Use Render's secret environment variables

### Network Security

- Containers communicate on internal Docker network
- Only necessary ports are exposed
- Use HTTPS in production

## Monitoring

### Health Check

```bash
curl http://localhost:8080/api/actuator/health
```

### Logs

```bash
docker-compose logs -f backend
```

### Metrics

```bash
curl http://localhost:8080/api/actuator/metrics
```

## Updating the Application

1. **Pull latest code**
   ```bash
   git pull origin main
   ```

2. **Rebuild and restart**
   ```bash
   docker-compose up --build -d
   ```

3. **Check logs**
   ```bash
   docker-compose logs -f backend
   ```

## Production Checklist

- [ ] Set strong JWT secrets
- [ ] Set strong admin password
- [ ] Configure CORS allowed origins
- [ ] Set COOKIE_SECURE=true
- [ ] Configure database backups
- [ ] Set up persistent disk for uploads
- [ ] Enable HTTPS
- [ ] Configure monitoring/alerts
- [ ] Test health check endpoint
- [ ] Review and set resource limits

## Support

For deployment issues, see:
- [Render Documentation](https://render.com/docs)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
