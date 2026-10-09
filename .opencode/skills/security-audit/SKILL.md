---
name: security-audit
description: Use when performing security audits, vulnerability scanning, or security reviews on the Quini6 Analytics codebase. Trigger on requests like "security audit", "check for vulnerabilities", "scan for secrets", "review security", "find security issues", "hardcoded credentials check", "dependency audit", "Docker security review".
---

# Security Audit Skill

Comprehensive security audit procedures for the Quini6 Analytics platform.

## Quick Audit Checklist

Use this checklist for rapid security assessment:

```bash
# 1. Search for hardcoded secrets
grep -rn "password\|secret\|token\|apikey\|api_key" --include="*.java" --include="*.yml" --include="*.yaml" --include="*.properties" --include="*.ts" --include="*.tsx" --include="*.env*"

# 2. Search for hardcoded credentials in Java
grep -rn "admin123\|quini6_dev\|dev-only" --include="*.java"

# 3. Check for committed .env files
find . -name ".env" -o -name ".env.*" | head -20

# 4. Check Docker security
grep -n "xpack.security\|POSTGRES_HOST_AUTH_METHOD\|EXPOSE\|RUN.*chmod.*777" docker-compose.yml docker-compose.override.yml */Dockerfile

# 5. Check for SQL injection risks
grep -rn "createQuery\|createNativeQuery\|@Query.*+" --include="*.java"

# 6. Check for missing validation
grep -rn "@RequestParam\|@PathVariable" --include="*.java" | grep -v "validation\|@Valid"

# 7. Check Spring Security config
cat backend/src/main/java/com/quini6/analytics/config/SecurityConfig.java
```

## Known Vulnerabilities in This Codebase

Based on previous audits, these are confirmed security issues:

### CRITICAL
1. **Hardcoded JWT Secret** — `application.yml:122` contains `jwt.secret: dev-only-change-in-prod-256bit-key-here`
2. **Committed .env with Credentials** — `docker/.env` contains `ADMIN_PASSWORD=admin123`, `POSTGRES_PASSWORD=quini6_dev_2026`, `JWT_SECRET=dev-only-change-in-prod-256bit-key-here`
3. **Hardcoded RabbitMQ Credentials** — `MonitoringController.java:282` has `"quini6:quini6_dev_2026"` hardcoded in Basic Auth string

### HIGH
4. **Default Admin Credentials** — V1 migration seeds `admin`/`admin123` user
5. **Public Monitoring Endpoints** — `/api/monitoring/**` exposed without authentication
6. **Elasticsearch Security Disabled** — `docker-compose.yml:34` sets `xpack.security.enabled=false`

### MEDIUM
7. **No Rate Limiting** — Login endpoint `/api/auth/login` has no brute-force protection
8. **PostgreSQL Trust Auth** — `docker-compose.yml:16` uses `POSTGRES_HOST_AUTH_METHOD: trust`
9. **Missing HTTP Security Headers** — No Content-Security-Policy, X-Frame-Options, etc.

### LOW
10. **CSRF Disabled** — Acceptable for stateless API but documented for awareness
11. **No Certificate Pinning** — Web scraping via Jsoup doesn't pin certificates

## Audit Workflow

### Step 1: Secrets Scan
```bash
# Comprehensive secret search
rg -n "(?i)(password|secret|token|apikey|api_key|private_key|credential)" --type java --type yaml --type py

# Check git history for leaked secrets
git log --all --oneline --diff-filter=D -- "*.env" "*.env.*" | head -10
```

### Step 2: Dependency Audit
```bash
# Backend: Check Maven dependencies
cd backend && mvn dependency:tree -DoutputType=text | grep -v "compile\|provided"

# Frontend: Check npm audit
cd frontend && npm audit --json 2>/dev/null | jq '.vulnerabilities | length'
```

### Step 3: Configuration Review
```bash
# Check all YAML configs
find . -name "application*.yml" -o -name "application*.yaml" | xargs grep -l "secret\|password\|credential"

# Check Docker configs
grep -n "environment:" docker-compose.yml -A 20 | grep -i "password\|secret\|token"
```

### Step 4: Code Analysis
```bash
# Check for unsafe deserialization
grep -rn "ObjectMapper\|readValue\|fromJson" --include="*.java"

# Check for path traversal
grep -rn "PathVariable\|RequestParam" --include="*.java" | grep -i "file\|path\|dir"

# Check for XXE vulnerabilities
grep -rn "DocumentBuilderFactory\|SAXParser\|XMLReader" --include="*.java"
```

### Step 5: Docker Security
```bash
# Check Dockerfiles for security issues
find . -name "Dockerfile" -exec grep -l "chmod\|RUN.*root\|EXPOSE\|ADD\|COPY" {} \;

# Check docker-compose for port exposure
grep -n "ports:" docker-compose.yml -A 3
```

## Severity Rating Guide

| Severity | Description | Response Time |
|----------|-------------|---------------|
| CRITICAL | Immediate exploitation risk, data breach potential | Fix immediately |
| HIGH | Exploitable with moderate effort, significant impact | Fix before production |
| MEDIUM | Requires specific conditions, limited impact | Fix within sprint |
| LOW | Best practice improvement, defense in depth | Schedule for backlog |
| INFO | Observation, no immediate action needed | Document only |

## Remediation Templates

### Fix Hardcoded Secret
```java
// Before (VULNERABLE)
@Value("${jwt.secret:dev-only-change-in-prod-256bit-key-here}")
private String jwtSecret;

// After (SECURE)
@Value("${jwt.secret}")
@NotBlank
private String jwtSecret;
```

### Fix Committed .env
```bash
# Add to .gitignore if not present
echo "docker/.env" >> .gitignore

# Remove from git tracking (keeps local file)
git rm --cached docker/.env

# Rotate all exposed credentials immediately
```

### Add Rate Limiting
```java
// Add to SecurityConfig or via Bucket4j
@Bean
public MeterRegistryCustomizer<PrometheusMeterRegistry> rateLimitMetrics() {
    return registry -> registry.gauge("rate_limit_hits", counter);
}
```

## Compliance References

- **OWASP Top 10 (2021)**: A01-Broken Access Control, A02-Cryptographic Failures, A03-Injection
- **CWE-798**: Use of Hard-coded Credentials
- **CWE-259**: Use of Hard-coded Password
- **CWE-321**: Use of Hard-coded Cryptographic Key
- **NIST SP 800-53**: SC-8 (Transmission Confidentiality), AC-6 (Least Privilege)

## Important Disclaimer

This security audit tool is for **defensive security purposes only**. It helps identify and remediate vulnerabilities in the Quini6 Analytics codebase. Findings should be validated by a human security engineer before implementing fixes.
