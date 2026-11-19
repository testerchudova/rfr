# rfr-e2e

E2E-тесты Rangiffler: JUnit 6.1.3, Playwright Java 1.63 (`@UsePlaywright`), Allure 3, `JdbcClient` для фикстур.

## Запуск

```bash
./gradlew :rfr-e2e:installPlaywright                      # один раз: браузер Chromium
./gradlew e2e                                             # все UI-тесты (тег ui)
./gradlew :rfr-e2e:e2e --tests '*MainPageTest'            # один тест
./gradlew :rfr-e2e:test                                   # без тега ui; DB-пробам нужна MySQL, браузер и серверы не нужны; с --tests тег ui не отсекается
./gradlew :rfr-e2e:allureReport                           # rfr-e2e/build/reports/allure-report/allureReport/index.html
```

Стенд: MySQL `:3306`, `rfr-auth :9001`, `rfr-api :8081`, фронт `:3001` (см. корневой README).

В PowerShell используйте `./gradlew.bat`. Задача `e2e` не требует Bash.

Тесты, которым нужен браузер и стенд, помечены `@Tag("ui")` и запускаются задачей `e2e`. Задача `test` исключает тег `ui`: DB-пробы используют MySQL, но не требуют браузер, auth/API или фронт. Обе задачи запускают тесты при каждом вызове, `--rerun-tasks` не нужен. `e2e` исключает `engine-probe`, чтобы внутренние UI-мишени не попадали в обычный браузерный набор.

`StandReadinessExtension` перед первым UI-классом ждёт MySQL (TCP), auth (OIDC discovery), API (GraphQL `{__typename}`) и фронт (HTTP). Опрос ограничен `rfr.standTimeout`, по умолчанию 180 с; отдельная попытка — не дольше 1 с, включая получение тела HTTP. Проверка работает также при прямом JUnit-запуске класса или метода из IDEA. Ошибка перечисляет неготовые сервисы и команды запуска. `rfr.standCheck=false` отключает ожидание; это не отключает отдельную проверку миграций. Старая задача `waitForStand` оставлена для ручной диагностики и не является зависимостью `e2e`.

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
| `rfr.standCheck` / `rfr.standTimeout` | `true` / `180` секунд |
| `rfr.migrationCheck` | `true`, применяется только к UI-классам |
| `rfr.commit` | Gradle передаёт `git rev-parse --short HEAD`; при прямом IDE-запуске `unknown` |
| `allure.results.directory` | `rfr-e2e/build/allure-results`; Gradle передаёт абсолютный путь, прямой JUnit — `build/allure-results` относительно рабочей папки модуля |

Через один ServiceLoader-файл глобально подключены `AllureEnvironmentExtension` → `StandReadinessExtension` → `StandSchemaExtension`. Для обычных классов без `ui` они ничего не регистрируют и не обращаются к стенду. `SuiteExtension` допускает фильтрацию контекста до записи в корневой Store; подставные реализации проб сохраняют прежний контракт. При закрытии корневого Store окружение пишется даже после ошибки подготовки. Версии схем читаются через Flyway с ограниченными JDBC-таймаутами; при недоступной MySQL записывается `unavailable`.

## Архитектура

Один source set `src/test`, пакет `io.student.rangiffler`:

| Пакет | Что лежит |
|---|---|
| `config` | `TestConfig` — единственная точка чтения настроек |
| `jupiter` | аннотации и расширения JUnit, включая три глобальных `SuiteExtension`; `condition` выключает `@ProbeTarget` вне EngineTestKit; `probe` проверяет жизненный цикл, владение ресурсами и подготовку стенда |
| `data` | тестовые данные и их владельцы: `TestUser`, `UserFixture` с реализацией `SingleUserFixture` и декораторами `UserWithFriendsFixture`, `UserWithIncomeInvitationsFixture`, `UserRequest` собирает их цепочку; `db` — `Databases`, `Jdbc` и неизменяемые объекты строк (`Row`/`Rows`: `AuthAccount`, `ApiProfile`, `Friendship`, `Friendships`), которые прячут весь SQL за `create`/`delete`/`exists`; `Migrations` сверяет схему БД с миграциями бэкенда через Flyway validate |
| `utils` | статические утилиты без состояния: `RandomDataUtils` — случайные username/password; сюда же будущие генераторы (описания фото и т.п.) |
| `ui` | `page` — Page Objects, `component` — общие части страниц |
| `test` | только тесты, по фичам: `auth`, `profile`, позже `people`, … |

Правила: в `jupiter` нет SQL и локаторов; в `data` нет JUnit; в `test` нет локаторов и SQL; `ThreadLocal` для состояния теста не используется — состояние живёт в Store расширения.

## Агент

Page Objects пишет Claude Code по skill `rfr-page-objects`; UI исследуется через Playwright MCP (`.mcp.json`).

Доказательство корректности теста - запуск gradle таски.
