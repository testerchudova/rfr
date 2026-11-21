package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;
import java.util.ArrayList;
import java.util.List;

public final class UserWithOutcomeInvitationsFixture implements UserFixture {
  private final UserFixture origin;
  private final int count;
  private final List<UserFixture> invitees = new ArrayList<>();

  public UserWithOutcomeInvitationsFixture(UserFixture origin, int count) {
    this.origin = origin;
    this.count = count;
  }

  @Override
  public TestUser user() {
    return origin.user().withOutcomeInvitations(invitees.stream().map(UserFixture::user).toList());
  }

  @Override
  public void create() {
    origin.create();
    for (int index = 0; index < count; index++) {
      UserFixture invitee = new SingleUserFixture(TestUser.unique());
      invitees.add(invitee);
      invitee.create();
      new Friendship(origin.user().username(), invitee.user().username(), FriendshipStatus.PENDING).create();
    }
  }

  @Override
  public void close() {
    new TearDown()
        .withEach(invitees, UserFixture::close)
        .with(origin::close)
        .run();
  }
}
