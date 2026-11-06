package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.Path;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class ProfilePage {
  public static final String URL = TestConfig.frontUrl() + "/profile";
  private final Page page;
  private final Locator usernameInput;
  private final Locator firstName;
  private final Locator surname;
  private final Locator location;
  private final Locator save;
  private final Locator reset;
  private final Locator avatarInput;
  private final Locator avatar;

  public ProfilePage(Page page) {
    this.page = page;
    this.usernameInput = page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true));
    this.firstName = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("First Name").setExact(true));
    this.surname = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Surname").setExact(true));
    this.location = page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Location").setExact(true));
    this.save = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true));
    this.reset = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset").setExact(true));
    this.avatarInput = page.getByRole(AriaRole.MAIN).locator("input[type='file']");
    this.avatar = page.getByRole(AriaRole.MAIN).locator("img:not([alt])[src^='data:image/']");
  }

  @Step("Открыть профиль")
  public ProfilePage open() {
    page.navigate(URL);
    return this;
  }

  @Step("Перезагрузить страницу профиля")
  public ProfilePage reload() {
    page.reload();
    return this;
  }

  @Step("Заполнить имя в профиле: {firstName}")
  public ProfilePage fillFirstName(String firstName) {
    this.firstName.fill(firstName);
    return this;
  }

  @Step("Заполнить фамилию в профиле: {surname}")
  public ProfilePage fillSurname(String surname) {
    this.surname.fill(surname);
    return this;
  }

  @Step("Выбрать страну в профиле: {country}")
  public ProfilePage selectLocation(String country) {
    location.click();
    page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHas(
        page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName(country).setExact(true)))).click();
    return this;
  }

  @Step("Загрузить аватар из файла: {path}")
  public ProfilePage uploadAvatar(Path path) {
    avatarInput.setInputFiles(path);
    return this;
  }

  @Step("Сохранить профиль")
  public ProfilePage clickSave() {
    page.waitForResponse(response -> response.url().endsWith("/graphql")
        && "POST".equals(response.request().method())
        && response.request().postData() != null
        && response.request().postData().contains("\"operationName\":\"UpdateUser\""), save::click);
    return this;
  }

  @Step("Сбросить несохранённые изменения профиля")
  public ProfilePage clickReset() {
    reset.click();
    return this;
  }

  @Step("Проверить имя в профиле: {firstName}")
  public ProfilePage checkThatFirstName(String firstName) {
    assertThat(this.firstName).hasValue(firstName);
    return this;
  }

  @Step("Проверить фамилию в профиле: {surname}")
  public ProfilePage checkThatSurname(String surname) {
    assertThat(this.surname).hasValue(surname);
    return this;
  }

  @Step("Проверить страну в профиле: {country}")
  public ProfilePage checkThatLocation(String country) {
    assertThat(location).hasText(country);
    return this;
  }

  @Step("Проверить сохранённый аватар: {expectedDataUrl}")
  public ProfilePage checkThatAvatar(String expectedDataUrl) {
    assertThat(avatar).hasAttribute("src", expectedDataUrl);
    return this;
  }

  @Step("Проверить имя пользователя в профиле: {username}")
  public ProfilePage checkThatUsername(String username) {
    assertThat(usernameInput).hasValue(username);
    return this;
  }
}
