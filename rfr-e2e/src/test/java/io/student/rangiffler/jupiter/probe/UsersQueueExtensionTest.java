package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.data.StaticUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@Tag("engine-probe")
@Execution(ExecutionMode.SAME_THREAD)
@DisplayName("Аренда пользователей из общего пула")
class UsersQueueExtensionTest {
  @Test
  @DisplayName("После ошибки подготовки четвёртого параметра три аренды возвращаются")
  void partialPreparationReturnsPreviouslyLeasedUsers() {
    EngineExecutionResults results = run(FourParameters.class);
    results.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
    Throwable failure = results.testEvents().failed().stream()
        .map(event -> event.getRequiredPayload(TestExecutionResult.class))
        .flatMap(result -> result.getThrowable().stream()).findFirst().orElseThrow();
    assertInstanceOf(IllegalStateException.class, failure);
    assertTrue(failure.getMessage().contains("pool is exhausted"));
    assertAllThreeUsersAreAvailable();
  }

  @Test
  @DisplayName("Четыре последовательных теста проходят с пулом из трёх пользователей")
  void fourTestsReuseThreeUsers() {
    run(FourSequential.class).testEvents().assertStatistics(stats -> stats.started(4).succeeded(4));
    assertAllThreeUsersAreAvailable();
  }

  @Test
  @DisplayName("После падения теста арендованный пользователь возвращается")
  void failedTestReturnsItsUser() {
    EngineExecutionResults results = run(Failing.class);
    results.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
    String leased = results.allEvents().reportingEntryPublished().stream()
        .map(event -> event.getRequiredPayload(ReportEntry.class))
        .map(entry -> entry.getKeyValuePairs().get("leased"))
        .findFirst().orElseThrow();
    assertTrue(assertAllThreeUsersAreAvailable().contains(leased));
  }

  @Test
  @DisplayName("Пустой пул завершается по таймауту, повторный close не добавляет лишнего пользователя")
  void emptyPoolHasBoundedWaitAndCloseIsIdempotent() {
    try (StaticUserFixture first = new StaticUserFixture();
         StaticUserFixture second = new StaticUserFixture();
         StaticUserFixture third = new StaticUserFixture();
         StaticUserFixture waiting = new StaticUserFixture(Duration.ofMillis(100))) {
      first.create();
      second.create();
      third.create();
      IllegalStateException failure = assertTimeout(Duration.ofSeconds(2),
          () -> assertThrows(IllegalStateException.class, waiting::create));
      assertTrue(failure.getMessage().contains("pool is exhausted"));
      waiting.close();
      assertThrows(IllegalStateException.class, waiting::create);
      first.close();
      first.close();
      waiting.create();
      assertEquals(3, Set.of(waiting.user(), second.user(), third.user()).size());
    }
    assertAllThreeUsersAreAvailable();
  }

  private Set<String> assertAllThreeUsersAreAvailable() {
    try (StaticUserFixture first = new StaticUserFixture();
         StaticUserFixture second = new StaticUserFixture();
         StaticUserFixture third = new StaticUserFixture()) {
      Set<String> names = new HashSet<>();
      for (StaticUserFixture fixture : List.of(first, second, third)) {
        fixture.create();
        names.add(fixture.user().username());
      }
      assertEquals(3, names.size(), "Pool must contain all three distinct users after each target");
      return names;
    }
  }

  private EngineExecutionResults run(Class<?> target) {
    return EngineTestKit.engine("junit-jupiter")
        .configurationParameter(ProbeTargetCondition.PARAMETER, "true")
        .selectors(selectClass(target))
        .execute();
  }

  @ProbeTarget
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  @Execution(ExecutionMode.SAME_THREAD)
  static final class FourSequential {
    @Test
    @Order(1)
    void first(@StaticUser TestUser user) {
      assertTrue(user.username().startsWith("static-pool-"));
    }

    @Test
    @Order(2)
    void second(@StaticUser TestUser user) {
      assertTrue(user.username().startsWith("static-pool-"));
    }

    @Test
    @Order(3)
    void third(@StaticUser TestUser user) {
      assertTrue(user.username().startsWith("static-pool-"));
    }

    @Test
    @Order(4)
    void fourth(@StaticUser TestUser user) {
      assertTrue(user.username().startsWith("static-pool-"));
    }
  }

  @ProbeTarget
  static final class FourParameters {
    @Test
    void observe(@StaticUser TestUser first, @StaticUser TestUser second,
                 @StaticUser TestUser third, @StaticUser TestUser fourth) {
      throw new IllegalStateException("The fourth lease must time out before the test body");
    }
  }

  @ProbeTarget
  static final class Failing {
    @Test
    void fail(TestReporter reporter, @StaticUser TestUser user) {
      reporter.publishEntry("leased", user.username());
      throw new IllegalStateException("Static user probe fails on purpose");
    }
  }
}
