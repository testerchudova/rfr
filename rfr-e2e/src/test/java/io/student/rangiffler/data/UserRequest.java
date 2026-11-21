package io.student.rangiffler.data;

public record UserRequest(int friends, int incomeInvitations, int outcomeInvitations) {
  public UserRequest {
    if (friends < 0) {
      throw new IllegalArgumentException("Friends count must be non-negative, got " + friends);
    }
    if (incomeInvitations < 0) {
      throw new IllegalArgumentException("Income invitations count must be non-negative, got " + incomeInvitations);
    }
    if (outcomeInvitations < 0) {
      throw new IllegalArgumentException("Outcome invitations count must be non-negative, got " + outcomeInvitations);
    }
  }

  public UserFixture fixture() {
    UserFixture fixture = new SingleUserFixture(TestUser.unique());
    if (friends > 0) {
      fixture = new UserWithFriendsFixture(fixture, friends);
    }
    if (incomeInvitations > 0) {
      fixture = new UserWithIncomeInvitationsFixture(fixture, incomeInvitations);
    }
    if (outcomeInvitations > 0) {
      fixture = new UserWithOutcomeInvitationsFixture(fixture, outcomeInvitations);
    }
    return fixture;
  }
}
