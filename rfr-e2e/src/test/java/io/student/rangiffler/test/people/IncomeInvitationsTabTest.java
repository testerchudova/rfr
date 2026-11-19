package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;
import io.student.rangiffler.ui.page.PeoplePage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
@UsePlaywright
@DisplayName("People: Income invitations")
class IncomeInvitationsTabTest {
  @Test
  @DisplayName("Видна своя входящая заявка с Accept и Decline, чужая заявка не видна")
  void ownInvitationAndActions(Page page, @User(incomeInvitations = 1) TestUser user,
                              @User(incomeInvitations = 1) TestUser other) {
    String inviter = user.incomeInvitations().getFirst().username();
    people(page, user).clickIncomeInvitations()
        .checkThatPeople(List.of(inviter))
        .checkThatPersonAbsent(other.incomeInvitations().getFirst().username())
        .checkThatIncomeInvitationActions(inviter);
  }

  @Test
  @DisplayName("Без входящих заявок вкладка пуста, даже если другому пришло приглашение")
  void emptyWithoutOwnInvitations(Page page, @User TestUser user,
                                  @User(incomeInvitations = 1) TestUser other) {
    people(page, user).clickIncomeInvitations()
        .checkThatEmpty()
        .checkThatPersonAbsent(other.incomeInvitations().getFirst().username());
  }

  private PeoplePage people(Page page, TestUser user) {
    return new LandingPage(page).open().clickLogin()
        .fillLoginForm(user.username(), user.password()).clickSignIn().clickPeople();
  }
}
