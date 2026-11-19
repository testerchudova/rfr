package io.student.rangiffler.data;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public final class StaticUserFixture implements UserFixture {
  private static final BlockingQueue<TestUser> USERS = new ArrayBlockingQueue<>(3, true, List.of(
      new TestUser("static-pool-1", "pool-password-1"),
      new TestUser("static-pool-2", "pool-password-2"),
      new TestUser("static-pool-3", "pool-password-3")));
  private final Duration timeout;
  private TestUser leased;

  public StaticUserFixture() {
    this(Duration.ofSeconds(2));
  }

  public StaticUserFixture(Duration timeout) {
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("Static user timeout must be positive");
    }
    this.timeout = timeout;
  }

  @Override
  public synchronized TestUser user() {
    if (leased == null) {
      throw new IllegalStateException("No static user is leased by this fixture");
    }
    return leased;
  }

  @Override
  public synchronized void create() {
    if (leased != null) {
      throw new IllegalStateException("This fixture already leases a static user");
    }
    try {
      leased = USERS.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for a static user", failure);
    }
    if (leased == null) {
      throw new IllegalStateException("Static user pool is exhausted; no user became available within " + timeout);
    }
  }

  @Override
  public synchronized void close() {
    if (leased != null) {
      if (!USERS.offer(leased)) {
        throw new IllegalStateException("Cannot return the leased static user: pool is full");
      }
      leased = null;
    }
  }
}
