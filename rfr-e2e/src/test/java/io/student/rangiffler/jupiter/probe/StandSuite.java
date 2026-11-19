package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.extension.StandSchemaExtension;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("io.student.rangiffler.test")
@IncludeEngines("junit-jupiter")
@IncludeTags("ui")
public class StandSuite {
  @BeforeSuite
  static void verifySchema() {
    System.out.println("HW04_BEFORE_SUITE pid=" + ProcessHandle.current().pid()
        + " at=" + System.currentTimeMillis());
    StandSchemaExtension.verifySchema();
  }
}
