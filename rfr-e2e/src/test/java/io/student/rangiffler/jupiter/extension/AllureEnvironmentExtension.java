package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Migrations;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class AllureEnvironmentExtension implements SuiteExtension {
  @Override
  public boolean supports(ExtensionContext context) {
    return context.getTags().contains("ui");
  }

  @Override
  public void afterSuite(ExtensionContext context) {
    Properties environment = new Properties();
    environment.setProperty("frontUrl", TestConfig.frontUrl());
    environment.setProperty("authUrl", TestConfig.authUrl());
    environment.setProperty("apiUrl", TestConfig.apiUrl());
    environment.setProperty("browser", TestConfig.browser());
    environment.setProperty("headless", String.valueOf(TestConfig.headless()));
    environment.setProperty("commit", TestConfig.commit());
    environment.setProperty("authSchemaVersion", new Migrations(Databases.AUTH, TestConfig.authMigrations()).currentVersion());
    environment.setProperty("apiSchemaVersion", new Migrations(Databases.API, TestConfig.apiMigrations()).currentVersion());
    Path directory = TestConfig.allureResultsDirectory();
    try {
      Files.createDirectories(directory);
      try (var output = Files.newOutputStream(directory.resolve("environment.properties"))) {
        environment.store(output, null);
      }
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot write Allure environment to " + directory, exception);
    }
  }
}
