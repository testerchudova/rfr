package io.student.rangiffler.jupiter.extension;

import java.time.Instant;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

public final class StandLauncherSessionListener implements LauncherSessionListener {
  @Override
  public void launcherSessionOpened(LauncherSession session) {
    System.out.println("HW04_LAUNCHER_SESSION pid=" + ProcessHandle.current().pid() + " at=" + Instant.now());
    StandSchemaExtension.verifySchema();
  }
}
