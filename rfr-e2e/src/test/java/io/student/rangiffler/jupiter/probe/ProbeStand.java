package io.student.rangiffler.jupiter.probe;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class ProbeStand implements AutoCloseable {
  private final HttpServer backend;
  private final HttpServer front;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final java.util.concurrent.ExecutorService handlers = Executors.newVirtualThreadPerTaskExecutor();
  private final CountDownLatch releaseBody = new CountDownLatch(1);
  private final AtomicInteger requests = new AtomicInteger();
  private final AtomicInteger apiStatus = new AtomicInteger(200);
  private final AtomicInteger upgradeRequests = new AtomicInteger();
  private final java.util.concurrent.atomic.AtomicBoolean authRedirect = new java.util.concurrent.atomic.AtomicBoolean();
  private final java.util.concurrent.atomic.AtomicBoolean rejectUpgrade = new java.util.concurrent.atomic.AtomicBoolean();
  private final Map<String, String> original = new HashMap<>();

  ProbeStand(Path results, long frontDelayMillis, boolean stalledBody) {
    HttpServer allocatedBackend = null;
    try {
      allocatedBackend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      backend = allocatedBackend;
      front = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException exception) {
      if (allocatedBackend != null) {
        allocatedBackend.stop(0);
      }
      scheduler.shutdownNow();
      handlers.shutdownNow();
      throw new UncheckedIOException(exception);
    }
    backend.setExecutor(handlers);
    front.setExecutor(handlers);
    backend.createContext("/", exchange -> {
      requests.incrementAndGet();
      exchange.getRequestBody().readAllBytes();
      boolean redirect = authRedirect.get() && exchange.getRequestURI().getPath().contains("openid-configuration");
      if (redirect) {
        exchange.getResponseHeaders().set("Location", "http://127.0.0.1:1/absent");
      }
      exchange.sendResponseHeaders(redirect ? 302
          : exchange.getRequestURI().getPath().equals("/graphql") ? apiStatus.get() : 200, -1);
      exchange.close();
    });
    java.util.concurrent.atomic.AtomicBoolean frontReady = new java.util.concurrent.atomic.AtomicBoolean(frontDelayMillis == 0);
    java.util.concurrent.atomic.AtomicBoolean startScheduled = new java.util.concurrent.atomic.AtomicBoolean();
    front.createContext("/", exchange -> {
      requests.incrementAndGet();
      if (rejectUpgrade.get() && exchange.getRequestHeaders().containsKey("Upgrade")) {
        upgradeRequests.incrementAndGet();
        exchange.sendResponseHeaders(426, -1);
        exchange.close();
        return;
      }
      if (!frontReady.get()) {
        if (frontDelayMillis > 0 && startScheduled.compareAndSet(false, true)) {
          scheduler.schedule(() -> frontReady.set(true), frontDelayMillis, TimeUnit.MILLISECONDS);
        }
        exchange.sendResponseHeaders(503, -1);
        exchange.close();
        return;
      }
      exchange.sendResponseHeaders(200, stalledBody ? 10 : -1);
      if (stalledBody) {
        try {
          releaseBody.await();
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
        }
      }
      exchange.close();
    });
    backend.start();
    front.start();
    property("rfr.dbUrl", "jdbc:mysql://127.0.0.1:" + backend.getAddress().getPort());
    property("rfr.authUrl", backendUrl());
    property("rfr.apiUrl", backendUrl());
    property("rfr.frontUrl", frontUrl());
    property("rfr.standTimeout", "5");
    property("rfr.standCheck", "true");
    property("rfr.migrationCheck", "false");
    property("allure.results.directory", results.toString());
    property("rfr.commit", "abc1234");
  }

  void property(String key, String value) {
    if (!original.containsKey(key)) {
      original.put(key, System.getProperty(key));
    }
    System.setProperty(key, value);
  }

  String backendUrl() {
    return "http://127.0.0.1:" + backend.getAddress().getPort();
  }

  String frontUrl() {
    return "http://127.0.0.1:" + front.getAddress().getPort();
  }

  int requests() {
    return requests.get();
  }

  void apiStatus(int status) {
    apiStatus.set(status);
  }

  void authRedirect() {
    authRedirect.set(true);
  }

  void rejectHttp2Upgrade() {
    rejectUpgrade.set(true);
  }

  int upgradeRequests() {
    return upgradeRequests.get();
  }

  @Override
  public void close() {
    scheduler.shutdownNow();
    releaseBody.countDown();
    front.stop(0);
    backend.stop(0);
    handlers.shutdownNow();
    original.forEach((key, value) -> {
      if (value == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, value);
      }
    });
  }
}
