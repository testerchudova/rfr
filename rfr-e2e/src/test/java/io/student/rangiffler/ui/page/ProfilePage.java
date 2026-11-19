package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class ProfilePage {
  public static final String URL = TestConfig.frontUrl() + "/profile";
  private final Page page;
  private final Locator avatar;
  private final Locator avatarInput;
  private final Locator firstNameInput;
  private final Locator surnameInput;
  private final Locator usernameInput;
  private final Locator location;
  private final Locator save;
  private final Locator reset;

  public ProfilePage(Page page) {
    this.page = page;
    this.save = page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Save").setExact(true));
    this.reset = page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Reset").setExact(true));
    this.avatar = page.getByRole(AriaRole.MAIN).getByRole(AriaRole.IMG,
        new Locator.GetByRoleOptions().setName("").setExact(true));
    this.avatarInput = page.getByLabel("Update avatar", new Page.GetByLabelOptions().setExact(true));
    this.firstNameInput = page.getByLabel("First Name", new Page.GetByLabelOptions().setExact(true));
    this.surnameInput = page.getByLabel("Surname", new Page.GetByLabelOptions().setExact(true));
    this.usernameInput = page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true));
    this.location = page.getByRole(AriaRole.COMBOBOX,
        new Page.GetByRoleOptions().setName("Location").setExact(true));
  }

  @Step("Открыть профиль")
  public ProfilePage open() {
    page.navigate(URL);
    return this;
  }

  @Step("Перезагрузить страницу профиля")
  public ProfilePage reload() {
    page.reload();
    return new ProfilePage(page);
  }

  @Step("Заполнить имя: {value}")
  public ProfilePage fillFirstName(String value) {
    firstNameInput.fill(value);
    return this;
  }

  @Step("Заполнить фамилию: {value}")
  public ProfilePage fillSurname(String value) {
    surnameInput.fill(value);
    return this;
  }

  @Step("Выбрать страну: {countryName}")
  public ProfilePage selectLocation(String countryName) {
    location.click();
    option(countryName).click();
    return this;
  }

  @Step("Загрузить аватар {file}")
  public ProfilePage uploadAvatar(Path file) {
    String expectedPayload = avatarPayload(file);
    avatarInput.setInputFiles(file);
    page.waitForCondition(() -> {
      String source = avatar.getAttribute("src");
      return source != null && source.startsWith("data:image/") && source.endsWith("," + expectedPayload);
    });
    return this;
  }

  @Step("Нажать Save и дождаться ответа API")
  public ProfilePage clickSave() {
    page.waitForResponse(
        response -> response.url().endsWith("/graphql")
            && String.valueOf(response.request().postData()).contains("UpdateUser"),
        save::click);
    return this;
  }

  @Step("Нажать Reset")
  public ProfilePage clickReset() {
    reset.click();
    return this;
  }

  @Step("Проверить имя пользователя в профиле: {username}")
  public ProfilePage checkThatUsername(String username) {
    assertThat(usernameInput).hasValue(username);
    return this;
  }

  @Step("Проверить имя в профиле: {value}")
  public ProfilePage checkThatFirstName(String value) {
    assertThat(firstNameInput).hasValue(value);
    return this;
  }

  @Step("Проверить фамилию в профиле: {value}")
  public ProfilePage checkThatSurname(String value) {
    assertThat(surnameInput).hasValue(value);
    return this;
  }

  @Step("Проверить страну в профиле: {countryName}")
  public ProfilePage checkThatLocation(String countryName) {
    assertThat(location).hasText(countryName);
    return this;
  }

  @Step("Проверить, что аватар загружен изображением")
  public ProfilePage checkThatAvatarUploaded() {
    assertThat(avatar).hasAttribute("src", Pattern.compile("^data:image/"));
    return this;
  }

  private String avatarPayload(Path file) {
    try {
      return Base64.getEncoder().encodeToString(Files.readAllBytes(file));
    } catch (IOException failure) {
      throw new UncheckedIOException("Cannot read avatar file " + file, failure);
    }
  }

  @Step("Найти страну {countryName} в списке по флагу")
  private Locator option(String countryName) {
    return page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions()
        .setHas(page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName(countryName).setExact(true))));
  }
}
