# Quick Start: Deploy to Render in 5 Minutes

Follow these steps to deploy your Divine Corner backend to Render.

## Step 1: Test Docker Build Locally (Optional but Recommended)

### Windows:
```bash
build-docker.bat
```

### Linux/Mac:
```bash
chmod +x build-docker.sh
./build-docker.sh
```

### Or using Docker directly:
```bash
docker build -t divine-corner-backend .
```

**Expected result**: Docker image builds successfully (takes 5-10 minutes first time)

## Step 2: Push Code to Git

```bash
git add .
git commit -m "Add Docker and Render configuration"
git push origin main
```

## Step 3: Create Render Account

1. Go to https://render.com
2. Sign up with GitHub/GitLab
3. Connect your repository

## Step 4: Create MySQL Database

1. In Render Dashboard, click **"New +"** → **"PostgreSQL"**
   - OR use external MySQL (Railway, PlanetScale, etc.)

2. For **MySQL on Render** (not native, need external):
   - Recommended: Use **Railway** for MySQL (free tier available)
   - Or **PlanetScale** (serverless MySQL)
   - Or **AWS RDS** (production grade)

### Using Railway for MySQL (Recommended for Free Tier):

1. Go to https://railway.app
2. Create new project → "Provision MySQL"
3. Copy connection details:
   ```
   Host: containers-us-west-xxx.railway.app
   Port: 7XXX
   Username: root
   Password: <generated>
   Database: railway
   ```

4. Build connection string:
   ```
   jdbc:mysql://containers-us-west-xxx.railway.app:7XXX/railway?useSSL=true&serverTimezone=UTC
   ```

## Step 5: Create Web Service on Render

1. Click **"New +"** → **"Web Service"**

2. **Connect Repository**: Select your GitHub repo

3. **Configure Service**:
   - **Name**: `divine-corner-backend`
   - **Region**: Oregon (or closest to you)
   - **Branch**: `main`
   - **Root Directory**: Leave blank (or `backend/divinecorner` if in subdirectory)
   - **Environment**: **Docker**
   - **Dockerfile Path**: `./Dockerfile`
   - **Instance Type**: Select "Starter" ($7/month) or "Free" (with sleep)

4. **Click "Advanced"** and add environment variables:

## Step 6: Set Environment Variables

Copy and paste these, replacing values:

```bash
# Database (from Railway or your MySQL provider)
DATABASE_URL=jdbc:mysql://YOUR_HOST:PORT/DATABASE_NAME?useSSL=true&serverTimezone=UTC
DB_USERNAME=your_username
DB_PASSWORD=your_password

# Redis (Optional - use Render Redis or external)
REDIS_HOST=redis-xxxxx.onrender.com
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT Secrets - Generate these!
JWT_ACCESS_SECRET=<paste-64-char-random-string>
JWT_REFRESH_SECRET=<paste-64-char-random-string>

# App URLs (update after deployment)
APP_BASE_URL=https://divine-corner-backend.onrender.com/api
ALLOWED_ORIGINS=https://your-frontend.com,https://www.your-frontend.com

# Cookie settings for production
COOKIE_DOMAIN=.onrender.com
COOKIE_SECURE=true
COOKIE_SAME_SITE=none

# Admin credentials
ADMIN_EMAIL=admin@divinecorner.com
ADMIN_PASSWORD=<set-strong-password>

# File uploads
FILE_UPLOAD_DIR=/app/uploads

# Spring profile
SPRING_PROFILES_ACTIVE=prod
```

### Generate JWT Secrets:

**Windows PowerShell:**
```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

**Linux/Mac:**
```bash
openssl rand -base64 64
```

**Online:** Use https://generate-secret.vercel.app/64

## Step 7: Configure Health Check

In the Advanced section:
- **Health Check Path**: `/api/actuator/health`
- **Health Check Grace Period**: 60 seconds

## Step 8: Deploy

1. Click **"Create Web Service"**
2. Wait for build (5-10 minutes first time)
3. Watch logs for any errors

## Step 9: Verify Deployment

Once deployed, test these endpoints:

### Health Check:
```bash
curl https://your-service.onrender.com/api/actuator/health
```

**Expected**: `{"status":"UP"}`

### Test Login:
```bash
curl -X POST https://your-service.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@divinecorner.com",
    "password": "your-admin-password"
  }'
```

**Expected**: JSON with tokens

## Step 10: Set Up Persistent Storage (For File Uploads)

1. Go to your web service → **"Disks"** tab
2. Click **"Add Disk"**
3. Configure:
   - **Name**: `uploads`
   - **Mount Path**: `/app/uploads`
   - **Size**: 1 GB (or more as needed)
4. Click **"Add Disk"**
5. Service will redeploy automatically

## Common Issues & Solutions

### Issue 1: Build Fails - "Gradle not found"

**Solution**: Make sure `gradlew` and `gradle/` directory are committed to Git.

```bash
git add gradlew gradle/ -f
git commit -m "Add Gradle wrapper"
git push
```

### Issue 2: Database Connection Fails

**Check**:
1. DATABASE_URL format is correct
2. Username and password are correct
3. Database allows external connections
4. Port is correct (usually 3306 for MySQL)

**Test connection string**:
```bash
mysql -h YOUR_HOST -P PORT -u USERNAME -p
```

### Issue 3: Application Crashes on Startup

**Check logs** in Render dashboard. Common causes:
- Missing environment variables
- Invalid JWT secret
- Database connection issue
- Not enough memory (upgrade instance)

### Issue 4: CORS Errors from Frontend

**Update** `ALLOWED_ORIGINS`:
```bash
ALLOWED_ORIGINS=https://your-actual-frontend.com,https://www.your-actual-frontend.com
```

(No trailing slashes!)

### Issue 5: 503 Service Unavailable (Free Tier)

Free tier services sleep after 15 minutes of inactivity.
- First request takes 30-60 seconds (cold start)
- Upgrade to Starter ($7/month) for always-on service

## Next Steps

### 1. Add Custom Domain

1. Go to service → **Settings** → **Custom Domain**
2. Add your domain: `api.yourdomain.com`
3. Update DNS records as shown
4. Update environment variables:
   ```
   APP_BASE_URL=https://api.yourdomain.com/api
   COOKIE_DOMAIN=.yourdomain.com
   ```

### 2. Enable Auto-Deploy

Already enabled! Every `git push` triggers a new deployment.

### 3. Set Up Monitoring

- Render provides basic metrics
- Add external monitoring (UptimeRobot, StatusCake, etc.)
- Set up error tracking (Sentry, Rollbar, etc.)

### 4. Configure Backups

**Database**:
- Railway: Automated backups included
- PlanetScale: Point-in-time recovery
- AWS RDS: Configure automatic backups

**Uploads**:
- Render Disk: Manual backups needed
- Better: Migrate to S3/Cloudinary for uploads

### 5. Update Frontend

Update your frontend `.env`:
```bash
NEXT_PUBLIC_API_URL=https://divine-corner-backend.onrender.com/api
```

## Cost Breakdown

### Free Tier:
- Web Service: FREE (sleeps after 15 min)
- MySQL (Railway): FREE (500 MB)
- Total: **$0/month**

### Recommended Production:
- Web Service (Starter): $7/month
- MySQL (Railway Starter): $5/month
- Disk (1 GB): Free
- Total: **$12/month**

### With Redis:
- Add Render Redis: +$10/month
- Total: **$22/month**

## Support & Resources

- **Render Docs**: https://render.com/docs
- **Railway Docs**: https://docs.railway.app
- **Your Logs**: Render Dashboard → Your Service → Logs
- **Status**: https://status.render.com

## Deployment Checklist

- [ ] Code pushed to Git
- [ ] Docker builds locally
- [ ] MySQL database created
- [ ] All environment variables set
- [ ] Strong JWT secrets generated
- [ ] Strong admin password set
- [ ] CORS origins configured
- [ ] Health check path set
- [ ] Service deployed successfully
- [ ] Health endpoint responding
- [ ] Login endpoint working
- [ ] Persistent disk added (for uploads)
- [ ] Frontend updated with API URL

## That's It!

Your backend is now live at:
```
https://your-service.onrender.com/api
```

Happy deploying! 🚀
