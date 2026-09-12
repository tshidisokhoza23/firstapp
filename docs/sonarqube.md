# SonarQube / SonarCloud (Android)

Free analysis for the Kotlin/Compose app. Uses **SonarScanner for Gradle** (the official Android path). Do not use `sonar-scanner` CLI or `sonar-project.properties` here.

**Use SonarCloud in CI** (recommended). Local Community Edition is optional and offline only.

This is a separate workflow from Android Build / pipeline. It does not assemble release or deploy.

## 1. SonarCloud (CI — do this)

1. Open [https://sonarcloud.io](https://sonarcloud.io) and sign in with GitHub.
2. Import the **firstapp** GitHub repository (separate project from `firstappServer`).
3. Copy:
   - **Organization key**
   - **Project key**
4. Create a token: My Account → Security → Generate Token.
5. In the **firstapp** GitHub repo: **Settings → Secrets and variables → Actions**

   Secret:

   - `SONAR_TOKEN`

   Variables:

   - `SONAR_ORGANIZATION` — required
   - `SONAR_PROJECT_KEY` — optional; defaults to `firstapp`
   - `SONAR_HOST_URL` — optional; defaults to `https://sonarcloud.io`

6. Push to `main`/`master`/`develop` or open a PR. Workflow: `.github/workflows/sonarcloud.yml`.

If `SONAR_TOKEN` is missing, the job prints a skip message and does not fail the repo.

PR quality gate runs after the first successful analysis on the default branch. Run one scan on `main` first.

Install the **SonarQube Cloud** GitHub App when offered so issues show on pull requests.

Create a **different** SonarCloud project than the Go backend. Same organization is fine; keys must differ (`firstapp` vs `firstappServer`).

## 2. Local scan

```bash
export SONAR_TOKEN=your_token
export SONAR_ORGANIZATION=your_org
export SONAR_PROJECT_KEY=firstapp          # optional
export SONAR_HOST_URL=https://sonarcloud.io  # or http://localhost:9000
bash ./scripts/sonar_scan.sh
```

Local Community Edition can run from the server repo:

```bash
cd ../firstappServer
docker compose --profile sonar up -d sonarqube
```

Then point `SONAR_HOST_URL` at `http://localhost:9000` and omit `SONAR_ORGANIZATION`.

## 3. What is analyzed

- `app` Kotlin/Java sources
- Unit-test coverage for `betaDebug` (JaCoCo XML)
- Android lint report for `betaDebug`
- Excludes generated Room/Hilt/BuildConfig and `build/`

Do not put tokens in Gradle files.
