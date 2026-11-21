# Rangiffler

> **Приватный репозиторий.** Здесь доступен исходный код из видеоуроков курса
> **QA.GURU Java Advanced (Playwright)**.
>
> Исходно репозиторий является копией [qa-guru/rfr](https://github.com/qa-guru/rfr).


# Текущая структура проекта

```
rfr/
├── rfr-auth/              # OAuth 2.0 Authorization Server (готов)
├── rfr-api/               # GraphQL API Backend (базовая реализация)
│   ├── Mock контроллеры для фотографий
│   ├── Реальные контроллеры для пользователей и стран
│   └── Flyway миграции БД
├── rfr-gql-client/        # React + TypeScript Frontend (готов)
└── rfr-e2e/               # E2E тесты: JUnit 6 + Playwright + Allure 3
```

**Порты:**
- rfr-auth: 9001
- rfr-api: 8081
- rfr-gql-client: 3001

**Технологии:**
- Сборка: Java 25, Gradle 9.7.1
- Backend: Spring Boot 4.1.1, Spring for GraphQL, Spring Security, JPA, Flyway
- Frontend: React 19, TypeScript 6, Vite 8, Apollo Client 4, Material UI 9, React Router 8
- Auth: Spring Authorization Server
- БД: MySQL 8.4
- E2E: JUnit 6, Playwright Java 1.63, Allure 3


#### 1. Запусти зависимости (БД):

```bash
docker compose up -d --wait
```

MySQL 8.4.7 поднимается из `compose.yaml` (контейнер `rfr-mysql`, данные в volume `mysql-volume`), команда ждёт статуса `healthy`. `bash localenv.sh` делает то же самое. Остановить: `docker compose stop`, пересоздать данные: `docker compose down -v`.

#### 2. Запусти rfr-auth и rfr-api:

```bash
./gradlew runAll
```

Задача запускает `bootRun` обоих сервисов параллельно, Ctrl+C останавливает оба. По отдельности: `./gradlew :rfr-auth:bootRun` и `./gradlew :rfr-api:bootRun`, или main class из IDE.

Auth доступен на порту 9001: http://localhost:9001, rfr-api — на порту 8081: http://localhost:8081/graphql

#### 3. Обнови зависимости и запускай фронт:

```bash
cd rfr-gql-client
npm ci
npm run dev
```

Фронт стартанет на порту 3001: http://localhost:3001/

#### 4. Прогони E2E-тесты:

```bash
./gradlew :rfr-e2e:installPlaywright   # один раз: браузер Chromium
./gradlew e2e
```

Подробности — `rfr-e2e/README.md`.

#### 5. Проверь работоспособность

Кнопка "Login" работает через сервис auth. После успешной авторизации ты попадешь на главную страницу Rangiffler.

Mock контроллеры в rfr-api будут возвращать статические данные из JSON файлов для ленты фотографий.
Реальная работа с пользователями и странами уже реализована через базу данных.

## Codex

Инструкции проекта — в `AGENTS.md` корня и модулей. Общие skills находятся в `.claude/skills`; для обнаружения Codex используются ссылки, не копии:

```powershell
./scripts/setup-codex.ps1
```

В Linux/macOS:

```bash
mkdir -p .agents/skills
for skill in .claude/skills/*; do
  ln -s "../../$skill" ".agents/skills/$(basename "$skill")"
done
```

PostToolUse описан в `.codex/hooks.json`: правки Java компилируются, TS/TSX проверяются TypeScript и ESLint через `scripts/agent/compile.mjs`. В `.codex/config.toml` включена поддержка hooks. Новое определение хука пользователь доверяет через `/hooks`; агент доверие не меняет. Ручной запуск `node scripts/agent/compile.mjs <путь>` проверяет обработчик, но не подтверждает автоматическое событие. Ограничения команд Codex — в `.codex/rules/project.rules`.
