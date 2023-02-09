package twitter;

/**
 * a Class to represent a Tweet message on Twitter
 * A Tweet is comprised of a tweet_id, user_id, tweet_ts, and tweet_text
 * This assigns a unique id to the tweet, tracks the user who sent it,
 * the timestamp when the tweet was posted, and the content of the Tweet itself
 */
public class Tweet {

  private int user_id;
  private String tweet_text;

  /**
   * Constructs a Tweet
   * @param user_id the user who posted the Tweet
   * @param tweet_text The content of the Tweet itself
   */
  public Tweet(int user_id, String tweet_text) {
    this.user_id = user_id;
    this.tweet_text = tweet_text;

  }

  /**
   * Returns the user_id of the user who posted this Tweet
   * @return this user_id
   */
  public int getUser_id() { return user_id; }

  /**
   * Returns the tweet content (tweet_text) of this Tweet
   * @return this tweet_text
   */
  public String getTweet_text() { return tweet_text; }


}
