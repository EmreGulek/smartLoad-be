# Deploy backend to Render (free tier)

## Prerequisites

- GitHub repo pushed (`smartLoad-backend`)
- [Neon](https://neon.tech) PostgreSQL project with connection details
- JWT secret: `openssl rand -base64 32`

## Render setup

1. [dashboard.render.com](https://dashboard.render.com) → **New** → **Web Service**
2. Connect repository **smartLoad-be** (or your backend repo name)
3. Settings:
   | Field | Value |
   |-------|--------|
   | **Language** | **Docker** |
   | **Branch** | `main` |
   | **Root Directory** | *(empty)* |
   | **Instance type** | Free |

   Do **not** use Node/yarn — this is a Java app; the `Dockerfile` handles build and start.

4. **Environment Variables** (required):

   | Key | Example |
   |-----|---------|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST/neondb?sslmode=require` |
   | `SPRING_DATASOURCE_USERNAME` | `neondb_owner` |
   | `SPRING_DATASOURCE_PASSWORD` | *(Neon password)* |
   | `SECURITY_JWT_SECRET_KEY` | *(Base64 from openssl)* |
   | `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |

   After frontend deploy (Cloudflare Pages):

   | Key | Value |
   |-----|--------|
   | `CORS_ALLOWED_ORIGINS` | `https://head.smartload-fe.pages.dev,https://smartload-fe.pages.dev` |

   **E-posta doğrulama (Resend — recommended on Render free)**

   Render **free** blocks Gmail SMTP (port 587). Use [Resend](https://resend.com) (free tier, HTTPS):

   1. Sign up at resend.com with your Gmail
   2. **API Keys** → Create API Key → copy `re_...`
   3. Render env:

   | Key | Value |
   |-----|--------|
   | `RESEND_API_KEY` | `re_xxxxxxxx` |
   | `SMARTLOAD_MAIL_FROM` | `SmartLoad <onboarding@resend.dev>` |

   **Testing without custom domain:** `onboarding@resend.dev` can only send **to the same email you used to sign up for Resend**. For 2–3 testers with other addresses, add your domain in Resend → **Domains** → DNS records, then set `SMARTLOAD_MAIL_FROM=SmartLoad <noreply@yourdomain.com>`.

   Optional Gmail SMTP (paid Render only): `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, etc.

5. **Create Web Service** → wait for deploy (~5–15 min first time).

6. Verify: `https://YOUR-SERVICE.onrender.com/api/health` → `"status":"OK"`.

7. After adding mail env vars: **Manual Deploy**, then sign up / **Resend code** and check Render **Logs** (no `Mail not configured` line).

## Notes

- Free tier sleeps after ~15 min idle; first request may take 30–60 s.
- Never commit `.env` or secrets to Git.
- If SMTP is missing, verification codes appear only in Neon `users.verification_code` and Render logs.

## Local Docker test

```bash
docker build -t smartload-backend .
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/smartload \
  -e SPRING_DATASOURCE_USERNAME=smartload \
  -e SPRING_DATASOURCE_PASSWORD=smartload_dev \
  -e SECURITY_JWT_SECRET_KEY=YOUR_BASE64_SECRET \
  smartload-backend
```

Then open http://localhost:8080/api/health
