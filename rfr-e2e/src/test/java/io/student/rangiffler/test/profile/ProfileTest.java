package io.student.rangiffler.test.profile;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;
import io.student.rangiffler.ui.page.ProfilePage;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
@UsePlaywright
@DisplayName("Профиль пользователя")
class ProfileTest {
  @Test
  @DisplayName("Save сохраняет выбранный аватар даже при медленном FileReader")
  void avatarWaitsForAsyncReadBeforeSave(Page page, @User TestUser user) throws URISyntaxException {
    page.addInitScript("""
        const originalRead = FileReader.prototype.readAsDataURL;
        FileReader.prototype.readAsDataURL = function(file) {
          setTimeout(() => originalRead.call(this, file), 1500);
        };
        """);
    Path file = Path.of(getClass().getResource("/img/avatar.png").toURI());
    profile(page, user)
        .uploadAvatar(file)
        .clickSave()
        .reload()
        .checkThatAvatarUploaded();
  }

  @Test
  @DisplayName("Имя сохраняется и читается после перезагрузки")
  void firstNameSurvivesReload(Page page, @User TestUser user) {
    String firstName = "Имя" + random();
    profile(page, user)
        .fillFirstName(firstName)
        .clickSave()
        .reload()
        .checkThatFirstName(firstName);
  }

  @Test
  @DisplayName("Фамилия сохраняется и читается после перезагрузки")
  void surnameSurvivesReload(Page page, @User TestUser user) {
    String surname = "Фамилия-" + random();
    profile(page, user)
        .fillSurname(surname)
        .clickSave()
        .reload()
        .checkThatSurname(surname);
  }

  @Test
  @DisplayName("Новая страна сохраняется и читается после перезагрузки")
  void locationChangeSurvivesReload(Page page, @User TestUser user) {
    profile(page, user)
        .selectLocation("Japan")
        .clickSave()
        .reload()
        .checkThatLocation("Japan");
  }

  @Test
  @DisplayName("Загруженный аватар сохраняется и показывается после перезагрузки")
  void avatarSurvivesReload(Page page, @User TestUser user) throws URISyntaxException {
    profile(page, user)
        .uploadAvatar(Path.of(getClass().getResource("/img/avatar.png").toURI()))
        .clickSave()
        .reload()
        .checkThatAvatarUploaded();
  }

  @Test
  @DisplayName("Reset возвращает сохранённые значения после несохранённой правки")
  void resetRestoresSavedValues(Page page, @User TestUser user) {
    String firstName = "Сохранённое" + random();
    String surname = "Сохранённая" + random();
    profile(page, user)
        .fillFirstName(firstName)
        .fillSurname(surname)
        .selectLocation("France")
        .clickSave()
        .reload()
        .fillFirstName("Черновик" + random())
        .fillSurname("Черновик" + random())
        .selectLocation("Canada")
        .clickReset()
        .checkThatFirstName(firstName)
        .checkThatSurname(surname)
        .checkThatLocation("France");
  }

  private ProfilePage profile(Page page, TestUser user) {
    return new LandingPage(page).open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .clickProfile()
        .checkThatUsername(user.username());
  }

  private String random() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}
