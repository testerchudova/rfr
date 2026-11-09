package io.student.rangiffler.test.profile;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;
import io.student.rangiffler.ui.page.ProfilePage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UsePlaywright
@DisplayName("Редактирование профиля")
class ProfileTest {
  @Test
  @DisplayName("Имя сохраняется после перезагрузки")
  void firstNameIsPersisted(Page page, @User TestUser user) {
    loginToProfile(page, user).fillFirstName("Katy").clickSave().reload()
        .checkThatFirstName("Katy");
  }

  @Test
  @DisplayName("Фамилия сохраняется после перезагрузки")
  void surnameIsPersisted(Page page, @User TestUser user) {
    loginToProfile(page, user).fillSurname("Chudova").clickSave().reload()
        .checkThatSurname("Chudova");
  }

  @Test
  @DisplayName("Страна сохраняется и меняется после перезагрузки")
  void locationCanBeChanged(Page page, @User TestUser user) {
    loginToProfile(page, user).selectLocation("France").clickSave().reload()
        .checkThatLocation("France")
        .selectLocation("Germany").clickSave().reload()
        .checkThatLocation("Germany");
  }

  @Test
  @DisplayName("Аватар сохраняется после перезагрузки при отложенном чтении файла")
  void avatarIsPersisted(Page page, @User TestUser user) throws Exception {
    page.addInitScript("""
        const readAsDataURL = FileReader.prototype.readAsDataURL;
        FileReader.prototype.readAsDataURL = function(file) {
          setTimeout(() => readAsDataURL.call(this, file), 1000);
        };
        """);
    Path avatar = Path.of(Objects.requireNonNull(
        ProfileTest.class.getResource("/images/avatar.png"), "Missing test resource: /images/avatar.png").toURI());
    String expectedDataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(avatar));
    loginToProfile(page, user).uploadAvatar(avatar).clickSave().reload()
        .checkThatAvatar(expectedDataUrl);
  }

  @Test
  @DisplayName("Reset возвращает сохранённые имя, фамилию, страну и аватар")
  void resetRestoresSavedValues(Page page, @User TestUser user) throws Exception {
    Path avatar = Path.of(Objects.requireNonNull(
        ProfileTest.class.getResource("/images/avatar.png"), "Missing test resource: /images/avatar.png").toURI());
    Path replacement = Path.of(Objects.requireNonNull(
        ProfileTest.class.getResource("/images/replacement.png"), "Missing test resource: /images/replacement.png").toURI());
    String expectedDataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(avatar));
    loginToProfile(page, user)
        .fillFirstName("Katy").fillSurname("Chudova").selectLocation("France").uploadAvatar(avatar)
        .clickSave().reload()
        .checkThatFirstName("Katy").checkThatSurname("Chudova").checkThatLocation("France")
        .checkThatAvatar(expectedDataUrl)
        .fillFirstName("Anna").fillSurname("Ivanova").selectLocation("Germany").uploadAvatar(replacement)
        .clickReset()
        .checkThatFirstName("Katy").checkThatSurname("Chudova").checkThatLocation("France")
        .checkThatAvatar(expectedDataUrl)
        .reload()
        .checkThatFirstName("Katy").checkThatSurname("Chudova").checkThatLocation("France")
        .checkThatAvatar(expectedDataUrl);
  }

  private ProfilePage loginToProfile(Page page, TestUser user) {
    return new LandingPage(page).open().clickLogin()
        .fillLoginForm(user.username(), user.password()).clickSignIn()
        .clickProfile().checkThatUsername(user.username());
  }
}
