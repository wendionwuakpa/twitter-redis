package twitter;

/**
 * represents the many to many following relationship, where follows_id represents a
 * follower / followee relationship between 2 user_ids
 */
public class Follows {
  private int user_id;
  private int follows_id;

  /**
   * Constructor to create a Follows object
   * @param user_id tracks users
   * @param follows_id represents the relationship of following between 2 users
   */
  Follows(int user_id, int follows_id) {
    this.user_id = user_id;
    this.follows_id = follows_id;
  }

  /**
   *  getter method to extract the follows_id
   * @return id of the user
   */
  public int getFollows_id() {
    return follows_id;
  }

  /**
   * getter method to extract the user_id
   * @return id of user
   */
  public int getUser_id() {
    return user_id;
  }
}


