package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import java.util.List;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeoplePage {
  public static final String URL = TestConfig.frontUrl() + "/people";
  private final Page page;
  private final Locator allPeople;
  private final Locator friends;
  private final Locator incomeInvitations;
  private final Locator outcomeInvitations;
  private final Locator empty;
  private final Locator search;
  private final Locator rows;

  public PeoplePage(Page page) {
    this.page = page;
    this.allPeople = page.getByRole(AriaRole.TAB,
        new Page.GetByRoleOptions().setName("All People").setExact(true));
    this.friends = page.getByRole(AriaRole.TAB,
        new Page.GetByRoleOptions().setName("Friends").setExact(true));
    this.incomeInvitations = page.getByRole(AriaRole.TAB,
        new Page.GetByRoleOptions().setName("Income invitations").setExact(true));
    this.outcomeInvitations = page.getByRole(AriaRole.TAB,
        new Page.GetByRoleOptions().setName("Outcome invitations").setExact(true));
    this.empty = page.getByRole(AriaRole.TABPANEL).getByText("There are no users yet",
        new Locator.GetByTextOptions().setExact(true));
    this.search = page.getByRole(AriaRole.TEXTBOX,
        new Page.GetByRoleOptions().setName("search people").setExact(true));
    this.rows = page.getByRole(AriaRole.TABPANEL).getByRole(AriaRole.ROW)
        .filter(new Locator.FilterOptions().setHas(page.getByRole(AriaRole.ROWHEADER)));
  }

  @Step("Открыть вкладку All People")
  public PeoplePage clickAllPeople() {
    allPeople.click();
    return this;
  }

  @Step("Открыть вкладку Income invitations")
  public PeoplePage clickIncomeInvitations() {
    page.waitForResponse(
        response -> response.url().endsWith("/graphql")
            && String.valueOf(response.request().postData()).contains("GetInvitations"),
        incomeInvitations::click).finished();
    return this;
  }

  @Step("Открыть вкладку Friends")
  public PeoplePage clickFriends() {
    friends.click();
    return this;
  }

  @Step("Открыть вкладку Outcome invitations")
  public PeoplePage clickOutcomeInvitations() {
    page.waitForResponse(
        response -> response.url().endsWith("/graphql")
            && String.valueOf(response.request().postData()).contains("GetOutcomeInvitations"),
        outcomeInvitations::click).finished();
    return this;
  }

  @Step("Найти пользователя {username}")
  public PeoplePage searchPeople(String username) {
    search.fill(username);
    search.press("Enter");
    return this;
  }

  @Step("Нажать Add у пользователя {username} и дождаться ответа API")
  public PeoplePage clickAdd(String username) {
    page.waitForResponse(
        response -> response.url().endsWith("/graphql")
            && String.valueOf(response.request().postData()).contains("FriendshipAction"),
        () -> row(username).getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Add").setExact(true)).click());
    return this;
  }

  @Step("Проверить, что в таблице ровно пользователи {usernames}")
  public PeoplePage checkThatPeople(List<String> usernames) {
    assertThat(rows).hasCount(usernames.size());
    usernames.forEach(username -> assertThat(row(username)).isVisible());
    return this;
  }

  @Step("Проверить, что приглашение пользователю {username} ждёт ответа")
  public PeoplePage checkThatInvitationSent(String username) {
    assertThat(row(username)).containsText("Waiting...");
    return this;
  }

  @Step("Проверить отсутствие пользователя {username} во вкладке")
  public PeoplePage checkThatPersonAbsent(String username) {
    assertThat(row(username)).hasCount(0);
    return this;
  }

  @Step("Проверить пустую вкладку People")
  public PeoplePage checkThatEmpty() {
    assertThat(empty).isVisible();
    assertThat(rows).hasCount(0);
    return this;
  }

  @Step("Проверить кнопку Remove у друга {username}")
  public PeoplePage checkThatFriendActions(String username) {
    assertThat(row(username).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Remove").setExact(true))).isVisible();
    assertThat(row(username).getByRole(AriaRole.BUTTON)).hasCount(1);
    return this;
  }

  @Step("Проверить кнопки Accept и Decline у приглашения от {username}")
  public PeoplePage checkThatIncomeInvitationActions(String username) {
    assertThat(row(username).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Accept").setExact(true))).isVisible();
    assertThat(row(username).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Decline").setExact(true))).isVisible();
    assertThat(row(username).getByRole(AriaRole.BUTTON)).hasCount(2);
    return this;
  }

  @Step("Проверить Waiting и кнопку Cancel у приглашённого {username}")
  public PeoplePage checkThatOutcomeInvitationActions(String username) {
    assertThat(row(username)).containsText("Waiting...");
    assertThat(row(username).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Cancel").setExact(true))).isVisible();
    assertThat(row(username).getByRole(AriaRole.BUTTON)).hasCount(1);
    return this;
  }

  @Step("Найти строку пользователя {username}")
  private Locator row(String username) {
    return rows.filter(new Locator.FilterOptions().setHas(page.getByRole(AriaRole.CELL,
        new Page.GetByRoleOptions().setName(username).setExact(true))));
  }
}
