package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.config.TestConfig;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.PropertiesUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import static io.student.rangiffler.jupiter.probe.StandReadinessExtensionTest.assertExecuted;
import static io.student.rangiffler.jupiter.probe.StandReadinessExtensionTest.failureContains;
import static io.student.rangiffler.jupiter.probe.StandReadinessExtensionTest.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
@Tag("engine-probe")
@DisplayName("Окружение Allure")
class AllureEnvironmentExtensionTest {
  @Test
  @DisplayName("Прямой JUnit без override пишет Allure и окружение в один каталог")
  void defaultDirectoryMatchesActualAllureWriter(@TempDir Path ownedAttachments) throws IOException {
    String previous = System.getProperty("allure.results.directory");
    String uuid = UUID.randomUUID().toString();
    String source = uuid + "-result.json";
    try {
      System.clearProperty("allure.results.directory");
      AllureLifecycle lifecycle = new AllureLifecycle();
      AllureExternalKey key = AllureExternalKey.random(AllureEnvironmentExtensionTest.class);
      lifecycle.scheduleTest(key, new TestResult().setUuid(uuid).setName("directory probe"));
      lifecycle.writeTest(key);
      assertTrue(Files.isRegularFile(TestConfig.allureResultsDirectory().resolve(source)),
          "Allure writer and environment must share their default directory");
    } finally {
      try {
        Path actualDirectory = Path.of(PropertiesUtils.loadAllureProperties()
            .getProperty("allure.results.directory", "allure-results"));
        Path ownedAttachment = actualDirectory.resolve(source);
        if (Files.exists(ownedAttachment)) {
          Files.move(ownedAttachment, ownedAttachments.resolve(source));
        }
      } finally {
        if (previous == null) {
          System.clearProperty("allure.results.directory");
        } else {
          System.setProperty("allure.results.directory", previous);
        }
      }
    }
  }

  @Test
  @DisplayName("После прогона записаны все настройки и недоступные версии БД")
  void writesEnvironmentWithoutDatabase(@TempDir Path resultsDirectory) throws IOException {
    try (ProbeStand stand = new ProbeStand(resultsDirectory, 0, false)) {
      stand.property("rfr.standCheck", "false");
      stand.property("rfr.browser", "firefox");
      stand.property("rfr.headless", "false");
      assertExecuted(run(StandReadinessExtensionTest.UiTarget.class));
      Properties environment = readEnvironment(resultsDirectory);
      assertEquals(Map.of(
          "frontUrl", stand.frontUrl(), "authUrl", stand.backendUrl(), "apiUrl", stand.backendUrl(),
          "browser", "firefox", "headless", "false", "commit", "abc1234",
          "authSchemaVersion", "unavailable", "apiSchemaVersion", "unavailable"), environment);
    }
  }

  @Test
  @DisplayName("Окружение сохраняется после падения readiness")
  void writesEnvironmentAfterReadinessFailure(@TempDir Path resultsDirectory) throws IOException {
    try (ProbeStand stand = new ProbeStand(resultsDirectory, -1, false)) {
      stand.property("rfr.standTimeout", "1");
      var result = run(StandReadinessExtensionTest.UiTarget.class);
      assertEquals(0, result.testEvents().started().count());
      assertTrue(failureContains(result, "rfr-gql-client"));
      assertEquals("abc1234", readEnvironment(resultsDirectory).getProperty("commit"));
    }
  }

  @Test
  @DisplayName("Окружение сохраняется после падения пользовательского beforeAll")
  void writesEnvironmentAfterBeforeAllFailure(@TempDir Path resultsDirectory) throws IOException {
    try (ProbeStand stand = new ProbeStand(resultsDirectory, 0, false)) {
      stand.property("rfr.standCheck", "false");
      var result = run(BrokenBeforeAll.class);
      assertEquals(0, result.testEvents().started().count());
      assertTrue(failureContains(result, "beforeAll fails on purpose"));
      assertEquals("unavailable", readEnvironment(resultsDirectory).getProperty("authSchemaVersion"));
    }
  }

  private Properties readEnvironment(Path resultsDirectory) throws IOException {
    Properties environment = new Properties();
    try (var input = Files.newInputStream(resultsDirectory.resolve("environment.properties"))) {
      environment.load(input);
    }
    return environment;
  }

  @ProbeTarget
  @Tag("ui")
  @Tag("engine-probe")
  static final class BrokenBeforeAll {
    @BeforeAll
    static void fail() {
      throw new IllegalStateException("beforeAll fails on purpose");
    }

    @Test
    void observe(TestReporter reporter) {
      reporter.publishEntry("probe", "must not execute");
    }
  }
}
