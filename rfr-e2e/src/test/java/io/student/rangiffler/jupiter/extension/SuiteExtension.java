package io.student.rangiffler.jupiter.extension;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public interface SuiteExtension extends BeforeAllCallback {

  @Override
  default void beforeAll(ExtensionContext context) throws Exception {
    if (!supports(context)) {
      return;
    }
    final ExtensionContext rootContext = context.getRoot();
    rootContext.getStore(ExtensionContext.Namespace.GLOBAL)
        .computeIfAbsent(getClass(), key -> new SuiteRun(this, rootContext), SuiteRun.class)
        .start();
  }

  default boolean supports(ExtensionContext context) {
    return true;
  }

  default void beforeSuite(ExtensionContext context) {
  }

  default void afterSuite(ExtensionContext context) {
  }
}
