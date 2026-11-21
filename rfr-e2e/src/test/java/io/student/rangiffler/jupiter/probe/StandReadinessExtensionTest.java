package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@Isolated
@Tag("engine-probe")
@DisplayName("Ожидание готовности стенда")
class StandReadinessExtensionTest {
  @Test
  @DisplayName("Проверка dev-сервера использует HTTP1.1 без upgrade")
  void usesHttp11ForDevelopmentServer(@TempDir Path resultsDirectory) {
    try (ProbeStand stand = new ProbeStand(resultsDirectory, 0, false)) {
      stand.rejectHttp2Upgrade();
      assertExecuted(run(UiTarget.class));
      assertEquals(0, stand.upgradeRequests(), "Readiness must not attempt an HTTP2 upgrade");
    }
  }

  @Test
  @DisplayName("OIDC302 подтверждает доступность без перехода по Location как curl без -L")
  void acceptsRedirectWithoutFollowingIt(@TempDir Path resultsDirectory) {
    try (ProbeStand stand = new ProbeStand(resultsDirectory, 0, false)) {
      stand.authRedirect();
      assertExecuted(run(UiTarget.class));
    }
  }

  @Test
  @DisplayName("HTTP401 API подтверждает доступность как curl без -f")
  void acceptsApiUnauthorizedResponse(@TempDir Path resultsDirectory) {
    try (ProbeStand stand = new ProbeStand(resultsDirectory, 0, false)) {
      stand.apiStatus(401);
      assertExecuted(run(UiTarget.class));
    }
  }

  @Test
  @DisplayName("Фронт начал отвечать через две секунды — тест выполняется")
  void waitsForLateService(@TempDir Path resultsDirectory) {
    long started = System.nanoTime();
    try (ProbeStand stand = new ProbeStand(resultsDirectory, 2000, false)) {
      EngineExecutionResults result = run(UiTarget.class);
      assertExecuted(result);
      assertTrue(stand.requests() >= 3, "Readiness did not query all HTTP services");
      assertTrue(Duration.ofNanos(System.nanoTime() - started).toMillis() >= 2000,
          "Test ran before the delayed service started");
    }
  }

  @Test
  @DisplayName("Таймаут называет недоступный фронт и блокирует тело теста")
  void namesMissingServiceAndBlocksTests(@TempDir Path resultsDirectory) {
    try (ProbeStand stand = new ProbeStand(resultsDirectory, -1, false)) {
      stand.property("rfr.standTimeout", "3");
      EngineExecutionResults result = run(UiTarget.class);
      assertEquals(0, result.testEvents().started().count());
      assertTrue(failureContains(result, "Stand was not ready within 3 s: rfr-gql-client: npm --prefix rfr-gql-client run dev"));
    }
  }

  @Test
  @DisplayName("Зависшее тело HTTP-ответа ограничено таймаутом")
  void boundsStalledResponseBody(@TempDir Path resultsDirectory) {
    long started = System.nanoTime();
    try (ProbeStand stand = new ProbeStand(resultsDirectory, 0, true)) {
      stand.property("rfr.standTimeout", "1");
      EngineExecutionResults result = run(UiTarget.class);
      assertEquals(0, result.testEvents().started().count());
      assertTrue(failureContains(result, "rfr-gql-client"));
      assertTrue(Duration.ofNanos(System.nanoTime() - started).toSeconds() < 8,
          "Readiness waited indefinitely for a response body");
    }
  }

  @Test
  @DisplayName("Класс без ui не обращается к стенду")
  void nonUiMakesNoRequests(@TempDir Path resultsDirectory) {
    try (ProbeStand stand = new ProbeStand(resultsDirectory, 0, false)) {
      assertExecuted(run(NonUiTarget.class));
      assertEquals(0, stand.requests());
    }
  }

  @Test
  @DisplayName("Выключенная проверка не обращается к HTTP-сервисам")
  void disabledCheckMakesNoRequests(@TempDir Path resultsDirectory) {
    try (ProbeStand stand = new ProbeStand(resultsDirectory, -1, false)) {
      stand.property("rfr.standCheck", "false");
      assertExecuted(run(UiTarget.class));
      assertEquals(0, stand.requests());
    }
  }

  static EngineExecutionResults run(Class<?> target) {
    return EngineTestKit.engine("junit-jupiter")
        .configurationParameter(ProbeTargetCondition.PARAMETER, "true")
        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
        .selectors(selectClass(target)).execute();
  }

  static void assertExecuted(EngineExecutionResults result) {
    var failures = result.containerEvents().failed().stream()
        .map(event -> event.getRequiredPayload(TestExecutionResult.class))
        .flatMap(outcome -> outcome.getThrowable().stream()).toList();
    assertTrue(failures.isEmpty(), () -> "Probe containers failed: " + failures);
    result.testEvents().assertStatistics(statistics -> statistics.started(1).succeeded(1).failed(0));
    assertEquals(1, result.testEvents().reportingEntryPublished().count(), "Probe body did not publish its observation");
  }

  static boolean failureContains(EngineExecutionResults result, String message) {
    return result.containerEvents().failed().stream()
        .map(event -> event.getRequiredPayload(TestExecutionResult.class))
        .flatMap(outcome -> outcome.getThrowable().stream())
        .flatMap(error -> Stream.iterate(error, cause -> cause != null, Throwable::getCause))
        .anyMatch(error -> String.valueOf(error.getMessage()).contains(message));
  }

  @ProbeTarget
  @Tag("ui")
  @Tag("engine-probe")
  static final class UiTarget {
    @Test
    void observe(TestReporter reporter) {
      reporter.publishEntry("probe", "executed");
    }
  }

  @ProbeTarget
  @Tag("engine-probe")
  static final class NonUiTarget {
    @Test
    void observe(TestReporter reporter) {
      reporter.publishEntry("probe", "executed");
    }
  }
}
