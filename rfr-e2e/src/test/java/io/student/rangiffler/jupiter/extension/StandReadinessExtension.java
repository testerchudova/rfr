package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class StandReadinessExtension implements SuiteExtension {
  @Override
  public boolean supports(ExtensionContext context) {
    return context.getTags().contains("ui") && TestConfig.standCheck();
  }

  @Override
  public void beforeSuite(ExtensionContext context) {
    Duration timeout = TestConfig.standTimeout();
    long deadline = System.nanoTime() + timeout.toNanos();
    ExecutorService connections = Executors.newVirtualThreadPerTaskExecutor();
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1))
        .build();
    List<String> pending = List.of();
    List<String> lastPending = List.of();
    try {
      do {
        pending = new ArrayList<>();
        if (!mysql(connections, deadline)) {
          pending.add("MySQL: docker compose up -d --wait");
        }
        if (!http(client, TestConfig.authUrl() + "/.well-known/openid-configuration", false, deadline)) {
          pending.add("rfr-auth: ./gradlew runAll");
        }
        if (!http(client, TestConfig.apiUrl() + "/graphql", true, deadline)) {
          pending.add("rfr-api: ./gradlew runAll");
        }
        if (!http(client, TestConfig.frontUrl(), false, deadline)) {
          pending.add("rfr-gql-client: npm --prefix rfr-gql-client run dev");
        }
        if (pending.isEmpty()) {
          return;
        }
        if (!pending.equals(lastPending)) {
          System.getLogger(getClass().getName()).log(System.Logger.Level.INFO,
              "Waiting for stand: " + String.join("; ", pending));
          lastPending = List.copyOf(pending);
        }
        long remaining = deadline - System.nanoTime();
        if (remaining > 0) {
          TimeUnit.NANOSECONDS.sleep(Math.min(remaining, Duration.ofMillis(250).toNanos()));
        }
      } while (System.nanoTime() < deadline);
      throw new IllegalStateException("Stand was not ready within " + timeout.toSeconds() + " s: "
          + String.join("; ", pending));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Stand readiness wait was interrupted", exception);
    } finally {
      client.shutdownNow();
      connections.shutdownNow();
    }
  }

  private boolean mysql(ExecutorService connections, long deadline) throws InterruptedException {
    long remaining = requestBudget(deadline);
    if (remaining <= 0) {
      return false;
    }
    Future<Boolean> attempt = connections.submit(() -> {
      URI database = URI.create(TestConfig.dbUrl("rangiffler-auth").substring("jdbc:".length()));
      try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(database.getHost(), database.getPort() < 0 ? 3306 : database.getPort()),
            (int) Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)));
        return true;
      }
    });
    try {
      return attempt.get(remaining, TimeUnit.NANOSECONDS);
    } catch (ExecutionException | TimeoutException exception) {
      return false;
    } finally {
      attempt.cancel(true);
    }
  }

  private boolean http(HttpClient client, String url, boolean graphql, long deadline) throws InterruptedException {
    long remaining = requestBudget(deadline);
    if (remaining <= 0) {
      return false;
    }
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofNanos(remaining));
    if (graphql) {
      request.header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{__typename}\"}"));
    }
    CompletableFuture<HttpResponse<Void>> response = client.sendAsync(request.build(), HttpResponse.BodyHandlers.discarding());
    try {
      int status = response.get(remaining, TimeUnit.NANOSECONDS).statusCode();
      return graphql || status >= 200 && status < 400;
    } catch (ExecutionException | TimeoutException exception) {
      return false;
    } finally {
      response.cancel(true);
    }
  }

  private long requestBudget(long deadline) {
    return Math.min(deadline - System.nanoTime(), Duration.ofSeconds(1).toNanos());
  }
}
