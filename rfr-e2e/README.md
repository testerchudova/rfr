# rfr-e2e

E2E-тесты Rangiffler: JUnit 6.1.3, Playwright Java 1.63 (`@UsePlaywright`), Allure 3, `JdbcClient` для фикстур.

## Запуск

```bash
./gradlew :rfr-e2e:installPlaywright                      # один раз: браузер Chromium
./gradlew :rfr-e2e:installPlaywrightWithDeps              # CI на Ubuntu: Chromium и системные библиотеки
./gradlew e2e                                            # все тесты
./gradlew :rfr-e2e:test --tests '*RegistrationTest'
./gradlew :rfr-e2e:test --tests '*MainPageTest'
./gradlew :rfr-e2e:allureReport                           # rfr-e2e/build/reports/allure-report/allureReport/index.html
```

Стенд: MySQL `:3306`, `rfr-auth :9001`, `rfr-api :8081`, фронт `:3001` (см. корневой README).

`e2e` не поднимает стенд: сначала `docker compose up -d --wait --wait-timeout 180`, в другом терминале `./gradlew runAll`, в третьем — фронтенд. Тестовая задача выполняется при каждом вызове без `--rerun-tasks`; в PowerShell используйте `.\gradlew.bat`.

## Конфигурация

`TestConfig` читает `-Drfr.<ключ>` → `RFR_<КЛЮЧ>` → значение по умолчанию. Gradle передаёт все `-Drfr.*` в тестовую JVM.

| Ключ | По умолчанию |
|---|---|
| `rfr.frontUrl` | `http://localhost:3001` |
| `rfr.authUrl` | `http://localhost:9001` |
| `rfr.apiUrl` | `http://localhost:8081` |
| `rfr.dbUrl` | `jdbc:mysql://localhost:3306` (схема добавляется кодом) |
| `rfr.dbUser` / `rfr.dbPassword` | `root` / `secret` |
| `rfr.browser` / `rfr.headless` | `chromium` / `true` |

## Архитектура

Один source set `src/test`, пакет `io.student.rangiffler`:

| Пакет | Что лежит |
|---|---|
| `config` | `TestConfig` — единственная точка чтения настроек |
| `jupiter` | всё про JUnit: `annotation` (`@User`), `extension` (`UserExtension`), позже `condition`, `probe` |
| `data` | тестовые данные и их владельцы: `TestUser`, `UserFixture`; `db` — `Databases`, `Jdbc` |
| `utils` | статические утилиты без состояния: `RandomDataUtils` — случайные username/password; сюда же будущие генераторы (описания фото и т.п.) |
| `ui` | `page` — Page Objects, `component` — общие части страниц |
| `test` | только тесты, по фичам: `auth`, `profile`, позже `people`, … |

Правила: в `jupiter` нет SQL и локаторов; в `data` нет JUnit; в `test` нет локаторов и SQL; `ThreadLocal` для состояния теста не используется — состояние живёт в Store расширения.

## Агент

Проектные правила Codex — в [AGENTS.md](AGENTS.md), корневой файл направляет к инструкциям модулей. Page Objects создаются по единственному skill `.claude/skills/rfr-page-objects/SKILL.md`. Подключение skill ссылкой и Playwright MCP описано в корневом README; `.mcp.json` используется только поддерживающими его клиентами.

Доказательство корректности сценария — фактический запуск тестов Gradle и их результаты. Проход через MCP подтверждает только исследование UI.

## E2E в GitHub Actions

Job `e2e` в [ci.yml](../.github/workflows/ci.yml) выполняется для изменений модулей, общих файлов Gradle, workflow и его скриптов в `scripts/ci`. MySQL запускается отдельным service-контейнером с healthcheck; Chromium и библиотеки Ubuntu устанавливает `installPlaywrightWithDeps`.

После запуска `runAll` и фронтенда скрипт `node scripts/ci/wait-for-ports.mjs 9001 8081 3001` ждёт готовности всех портов до 180 секунд. Таймаут можно изменить переменной `RFR_STARTUP_TIMEOUT_SECONDS`. Затем выполняется обычная команда `./gradlew e2e` в headless-режиме.

Во вкладке Summary прогона сохраняются время старта приложения и время тестов. Полную длительность job показывает GitHub Actions. Артефакт `e2e-allure-results` содержит исходные результаты Allure, а `e2e-diagnostics` — логи серверов, XML JUnit и HTML-отчёт Gradle. Оба загружаются и при падении; срок хранения — 7 дней.
