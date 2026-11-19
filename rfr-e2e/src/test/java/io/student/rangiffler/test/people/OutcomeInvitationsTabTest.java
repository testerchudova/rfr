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
@DisplayName("People: Outcome invitations")
class OutcomeInvitationsTabTest {
  @Test
  @DisplayName("Видно своё исходящее приглашение с Waiting и Cancel, чужое приглашение не видно")
  void ownInvitationAndActions(Page page, @User(outcomeInvitations = 1) TestUser user,
                              @User(outcomeInvitations = 1) TestUser other) {
    String invitee = user.outcomeInvitations().getFirst().username();
    people(page, user).clickOutcomeInvitations()
        .checkThatPeople(List.of(invitee))
        .checkThatPersonAbsent(other.outcomeInvitations().getFirst().username())
        .checkThatOutcomeInvitationActions(invitee);
  }

  @Test
  @DisplayName("Без исходящих приглашений вкладка пуста, даже если другой отправил приглашение")
  void emptyWithoutOwnInvitations(Page page, @User TestUser user,
                                  @User(outcomeInvitations = 1) TestUser other) {
    people(page, user).clickOutcomeInvitations()
        .checkThatEmpty()
        .checkThatPersonAbsent(other.outcomeInvitations().getFirst().username());
  }

  private PeoplePage people(Page page, TestUser user) {
    return new LandingPage(page).open().clickLogin()
        .fillLoginForm(user.username(), user.password()).clickSignIn().clickPeople();
  }
}
