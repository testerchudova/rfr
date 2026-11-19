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
@DisplayName("People: Friends")
class FriendsTabTest {
  @Test
  @DisplayName("Виден только свой друг с кнопкой Remove, чужая дружба не видна")
  void ownFriendAndActions(Page page, @User(friends = 1) TestUser user,
                          @User(friends = 1) TestUser other) {
    String friend = user.friends().getFirst().username();
    people(page, user).clickFriends()
        .checkThatPeople(List.of(friend))
        .checkThatPersonAbsent(other.friends().getFirst().username())
        .checkThatFriendActions(friend);
  }

  @Test
  @DisplayName("Без друзей вкладка пуста, даже если у другого пользователя есть друг")
  void emptyWithoutOwnFriends(Page page, @User TestUser user, @User(friends = 1) TestUser other) {
    people(page, user).clickFriends()
        .checkThatEmpty()
        .checkThatPersonAbsent(other.friends().getFirst().username());
  }

  private PeoplePage people(Page page, TestUser user) {
    return new LandingPage(page).open().clickLogin()
        .fillLoginForm(user.username(), user.password()).clickSignIn().clickPeople();
  }
}
