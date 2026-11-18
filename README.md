# Rangiffler

> **Приватный репозиторий.** Здесь доступен исходный код из видеоуроков курса
> **QA.GURU Java Advanced (Playwright)**.
>
> Исходно репозиторий является копией [qa-guru/rfr](https://github.com/qa-guru/rfr).


# Текущая структура проекта

```
rfr/
├── rfr-auth/              # OAuth 2.0 Authorization Server (готов)
├── rfr-api/               # GraphQL API: БД для профиля и дружбы, моки для фото
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


## Локальный запуск

Нужны JDK 25, Node.js 22+ и запущенный Docker Engine. Команды ниже выполняются из корня проекта в отдельных терминалах. В PowerShell заменяйте `./gradlew` на `.\gradlew.bat`.

### 1. MySQL через Compose

```bash
docker compose up -d --wait --wait-timeout 180
docker compose ps
```

Контейнер `rfr-mysql` должен иметь статус `healthy`. Compose использует MySQL `8.4.7`, порт 3306 и том с явным именем `mysql-volume`.

`bash localenv.sh` запускает только MySQL этого Compose-проекта и ждёт его готовности. Другие Docker-контейнеры скрипт не останавливает и не удаляет.

Если `rfr-mysql` создан ранее через `docker run`, сначала проверьте его Mounts: `/var/lib/mysql` должен быть подключён к `mysql-volume`. Затем остановите старый контейнер и переименуйте его либо удалите только контейнер, сохранив том. После этого повторите `docker compose up`. Новый контейнер подключит прежние данные. Команда `docker compose stop` останавливает MySQL с сохранением данных; `docker compose down -v` удаляет том и для обычной остановки не используется.

### 2. Бэкенды одной командой

```bash
./gradlew runAll
```

Команда параллельно запускает `rfr-auth` на 9001 и `rfr-api` на 8081; терминал остаётся занят. Дождитесь сообщений о запуске обоих приложений. `runAll` не запускает MySQL и фронтенд.

Сервис авторизации можно проверить по `http://localhost:9001/.well-known/openid-configuration`. GraphQL принимает POST на `http://localhost:8081/graphql`. GraphiQL на `/graphiql` требует аутентификации, поэтому ответ 401 на обычный GET без токена не означает, что API не запущен.

### 3. Фронтенд в другом терминале

```bash
npm --prefix rfr-gql-client ci
npm --prefix rfr-gql-client run dev
```

Откройте `http://localhost:3001`. Настройки `VITE_*` находятся в `rfr-gql-client/.env`; значения и назначение описаны в [README фронтенда](rfr-gql-client/README.md#конфигурация). Порт 3001 согласован с OAuth redirect URI.

### 4. E2E и отчёт

После готовности портов 9001, 8081 и 3001:

```bash
./gradlew :rfr-e2e:installPlaywright
./gradlew e2e
./gradlew :rfr-e2e:allureReport
```

Установка Chromium нужна один раз. Тестовая задача выполняется при каждом вызове `e2e`, даже если код не менялся; `--rerun-tasks` не требуется. `e2e` проверяет уже запущенный стенд и не поднимает его самостоятельно. Отчёт: `rfr-e2e/build/reports/allure-report/allureReport/index.html`.

## Что действительно сохраняется

Регистрация и вход работают через `rfr-auth`. Профиль, страны, люди и дружба реализованы в БД `rfr-api`. Фото, лайки, лента и статистика работают через моки: успешный ответ и snackbar не гарантируют сохранение. Для реализованных операций дождитесь ответа мутации и проверьте результат новым сетевым чтением после перезагрузки. Подробности — в [README API](rfr-api/README.md#что-реализовано-а-что-mock).

## Инструкции агента

Codex использует [корневой AGENTS.md](AGENTS.md) и инструкции модулей. `CLAUDE.md` ссылаются на те же правила; поддерживать два независимых набора не нужно. Корневой `AGENTS.md` явно требует читать инструкцию модуля перед работой с его файлами.

Skill `rfr-page-objects` хранится в `.claude/skills/rfr-page-objects`. Для подключения в Windows из корня выполните:

```powershell
.\scripts\setup-codex.ps1
```

Скрипт создаёт локальную junction-ссылку `.agents/skills/rfr-page-objects` без копирования исходника и работает из любого каталога проекта. Повторный запуск проверяет существующую ссылку. Локальная ссылка игнорируется Git; после клонирования создайте её скриптом снова. Если политика PowerShell запрещает выполнение скриптов, эту же папку можно подключить символической ссылкой из Git Bash с разрешённым созданием Windows symlinks.

На Linux/macOS:

```bash
mkdir -p .agents/skills
ln -s ../../.claude/skills/rfr-page-objects .agents/skills/rfr-page-objects
```

Playwright MCP подключается в настройках используемого клиента Codex. Для Codex CLI:

```bash
codex mcp add playwright -- npx @playwright/mcp@0.0.83 --isolated
codex mcp list
```

`.mcp.json` описывает тот же сервер для клиентов, которые читают этот файл. Наличие `.claude/settings.json` не подключает его права и хуки в Codex.

Для проверки подхвата начните новую сессию Codex в корне и попросите перечислить источники инструкций и ответить: «Как поднять стенд и прогнать e2e?», «Что в rfr-api настоящее, а что мок?», «Почему успешный snackbar не доказывает сохранение?». При запуске Codex из модуля цепочка также включает его `AGENTS.md`. Для работы из корня чтение модульного файла предписано корневой инструкцией. Изменение документа не доказывает, что уже открытая сессия перечитала его автоматически.

### Ограничения команд и хук проверки правок

Ограничения Codex находятся в [`.codex/rules/project.rules`](.codex/rules/project.rules). Для указанных там вариантов `git push`, `git -C . push`, `git rm`, `unlink`, удаления через `rm`/`Remove-Item` и `find . -delete` задано `forbidden`: отказ без запроса разрешения. Перенос файла в скрытый резервный файл с датой этими правилами не ограничен. Это проверяемые префиксы команд, а не запрет любого возможного удаления через произвольный скрипт.

Проверить решение штатного движка, не выполняя саму команду:

```bash
codex execpolicy check --rules .codex/rules/project.rules -- git -C . push origin hw/02-env-agent
codex execpolicy check --rules .codex/rules/project.rules -- mv example.txt .example.20261116000000.bak
```

Первый вызов должен вернуть `decision: forbidden`; второй — отсутствие совпавших запретов. Это не проверка фактического отказа текущей сессии. Codex читает проектные правила при запуске, если проектный слой конфигурации доверенный. Правила команд вне песочницы не следует считать полной защитой всех инструментов и интерпретаторов; отдельно проверяйте их применение в используемом клиенте. Подробности — в [документации правил Codex](https://learn.chatgpt.com/docs/agent-configuration/rules).

В [`.codex/hooks.json`](.codex/hooks.json) зарегистрирован `PostToolUse`: после правок через `apply_patch` и указанные инструменты IDE он вызывает [общий скрипт проверки](scripts/agent/compile.mjs). Изменённый Java-модуль компилируется Gradle; в Windows используется `--no-daemon`, чтобы фоновый процесс не задерживал завершение хука. Для TS/TSX в `rfr-gql-client/src` выполняются TypeScript и ESLint. ESLint получает изменённые файлы относительно `rfr-gql-client` и запускается даже после ошибки TypeScript. У каждой проверки есть таймаут. Успех не добавляет вывод в чат. Ошибки события `PostToolUse` возвращаются агенту через JSON с `decision: "block"` и текстом в `reason`: это сохраняет диагностику при запуске через Windows PowerShell, который может заменить ненулевой код скрипта кодом 1. При ручном запуске с путями файлов скрипт пишет ошибки в stderr и возвращает код 2. Проверка после правки не отменяет само изменение: ошибку нужно исправить.

В новой сессии Codex откройте `/hooks`, проверьте источник `.codex/hooks.json`, команду `scripts/agent/codex-hook.ps1` для Windows и включите доверие к этому хуку. Пока определение не доверено пользователем, автоматический запуск пропускается. После изменения определения требуется новый просмотр. Требование описано в [официальной документации hooks](https://learn.chatgpt.com/docs/hooks#review-and-trust-hooks); скрипты проекта не выставляют доверие автоматически.

Ручная диагностика из корня, которая не заменяет проверку автоматического события:

```bash
node scripts/agent/compile.mjs rfr-api/src/main/java/io/student/rangiffler/RangifflerApiApplication.java rfr-gql-client/src/App.tsx
```

После подключения проверьте заведомо неверную правку в Java и TSX: агент должен автоматически получить ошибку и исправить её. Сохраните вывод обоих срабатываний для PR. Правки в IDE руками и записи через произвольные shell-команды не входят в перечисленные события хука; для них запускайте диагностику явно.

Личные настройки менторства и уведомлений не входят в репозиторий.
