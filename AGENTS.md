# Rangiffler

Общие правила проекта для Codex. Перед чтением и изменением файлов модуля прочитай его `AGENTS.md`; подробности ищи в README этого модуля. Если инструкции расходятся с кодом, исправь документацию на основании проверенного поведения.

| Модуль | Назначение и порт | Инструкции |
|---|---|---|
| rfr-auth | OAuth/OIDC, 9001 | [rfr-auth/AGENTS.md](rfr-auth/AGENTS.md) |
| rfr-api | GraphQL `/graphql`, 8081; часть операций — моки | [rfr-api/AGENTS.md](rfr-api/AGENTS.md) |
| rfr-gql-client | Фронтенд, 3001; запускается отдельно от Gradle | [rfr-gql-client/AGENTS.md](rfr-gql-client/AGENTS.md) |
| rfr-e2e | JUnit, Playwright Java, Allure | [rfr-e2e/AGENTS.md](rfr-e2e/AGENTS.md) |

## Стенд и проверки

Нужны JDK 25, Node.js 22+ и Docker Desktop с работающим Engine. Команды выполняются из корня; в PowerShell используй `./gradlew.bat` вместо `./gradlew`.

1. `docker compose up -d --wait --wait-timeout 180` — MySQL на 3306, дождаться healthy.
2. В отдельном терминале `./gradlew runAll` — auth и API параллельно; процесс продолжает работать.
3. В ещё одном терминале `npm --prefix rfr-gql-client ci`, затем `npm --prefix rfr-gql-client run dev` — фронтенд. Перед тестами убедись, что доступны порты 9001, 8081 и 3001.
4. Один раз `./gradlew :rfr-e2e:installPlaywright`, затем `./gradlew e2e`. Тестовая задача выполняется при каждом вызове без `--rerun-tasks`; стенд автоматически не поднимает.
5. `./gradlew :rfr-e2e:allureReport` — построить локальный отчёт.

Compose использует существующий именованный том `mysql-volume`. Перед заменой старого контейнера `rfr-mysql` проверь его mount; том с данными сохраняй. `localenv.sh` запускает только MySQL через Compose. Настройки локальной БД: root / secret, схемы `rangiffler-auth` и `rangiffler-api`.

## Работа с агентом

Канонические skills хранятся в `.claude/skills`: `rfr-page-objects` для UI, `rfr-test-data` для предусловий, `rfr-test-review` для ревью. Для обнаружения Codex подключи их ссылками: в Windows запусти `./scripts/setup-codex.ps1`; команды для Linux/macOS — в корневом README. До подключения ссылок читай исходный skill напрямую. Копии не создавай.

`CLAUDE.md` содержит инструкции для Claude; границы модулей согласованы с `AGENTS.md`. Настройки `.claude/settings.json` не управляют Codex. Его ограничения команд находятся в `.codex/rules/project.rules`, а хук проверки правок — в `.codex/hooks.json`; подключение и проверка описаны в README. Новые хуки пользователь доверяет через `/hooks`, агент не меняет доверие за него. Ручной запуск проверки не доказывает автоматическое срабатывание.

Личные настройки обучения и уведомлений остаются в пользовательском каталоге агента. В PR включай воспроизводимые проектные инструкции и результаты выполненных проверок; успех через MCP не заменяет запуск тестов.
