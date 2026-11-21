package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
@UsePlaywright
@DisplayName("Негативный вход")
class LoginNegativeTest {
  @BeforeEach
  void useEnglishMessages(Page page) {
    page.context().setExtraHTTPHeaders(Map.of("Accept-Language", "en-US"));
  }

  @Test
  @DisplayName("Существующий пользователь с неверным паролем не входит")
  void wrongPasswordIsRejected(Page page, @User TestUser user) {
    new LandingPage(page).open()
        .clickLogin()
        .fillLoginForm(user.username(), "Wrong" + UUID.randomUUID().toString().substring(0, 6))
        .clickSignInRejected()
        .checkThatError("Bad credentials");
  }

  @Test
  @DisplayName("Несуществующий пользователь не входит")
  void unknownUserIsRejected(Page page) {
    TestUser stranger = TestUser.unique();
    new LandingPage(page).open()
        .clickLogin()
        .fillLoginForm(stranger.username(), stranger.password())
        .clickSignInRejected()
        .checkThatError("Bad credentials");
  }
}
