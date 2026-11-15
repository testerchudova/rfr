package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;
import io.student.rangiffler.utils.RandomDataUtils;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UsePlaywright
@DisplayName("Ошибки входа")
class LoginNegativeTest {
  @BeforeEach
  void useEnglishMessages(Page page) {
    page.context().setExtraHTTPHeaders(Map.of("Accept-Language", "en-US"));
  }

  @Test
  @DisplayName("Неверный пароль: Bad credentials")
  void wrongPasswordIsRejected(Page page, @User TestUser user) {
    new LandingPage(page).open().clickLogin()
        .fillLoginForm(user.username(), user.password() + "wrong")
        .clickSignInWithInvalidCredentials()
        .checkThatLoginError("Bad credentials");
  }

  @Test
  @DisplayName("Несуществующий пользователь: Bad credentials")
  void unknownUserIsRejected(Page page) {
    new LandingPage(page).open().clickLogin()
        .fillLoginForm(RandomDataUtils.randomUsername(), "Aa1Invalid")
        .clickSignInWithInvalidCredentials()
        .checkThatLoginError("Bad credentials");
  }
}
