package twitter;

import java.util.List;

/**
 * This interface represents the interaction between java and any database as it relates
 * to Twitter functionality, including posting tweets and retrieving timelines
 */
public interface TweetDatabaseAPI {


  ////////////////////////////// METHODS FOR TRACKING TWEET POSTS ////////////////////////////////

  /**
   * Posts a Tweet object (user_id, tweet_text) to the tweet table in the desired database
   * @param t this tweet
   */
  void postTweet(Tweet t);


  // future implementations will aim to abstract readCsv so that it does not require
  // repition for each csv type
  /**
   * Creates a list of tweets, generated from the given tweet csv
   * Ignores the header rows of the table when reading
   * @param filePath the filepath of the given tweet csv
   * @return a list of tweets
   */
  List<Tweet> readCsvTweet(String filePath);

  /**
   * Creates a Tweet object from the String Array of records generated from readCsvTweet
   * @param records the user_id and tweet_text record read from the tweet csv
   * @return a Tweet
   */
  Tweet createTweet(String[] records);

  /**
   *  Tracks the number of tweets inserted to the databse per second
   * @param tweets the list of tweets to be inserted to the database
   */
  void trackTweetsPerSecond(List<Tweet> tweets);


  //////////////////////// METHODS FOR TRACKING TIMELINES RETRIEVED //////////////////////////////

  /**
   * Creates a list of follows objects (user_id, follows_id), generated from the given follows csv
   * Ignores the header rows of the table when reading
   * @param filePath the filepath of the given follows csv
   * @return a list of follows
   */
  List<Follows> readCsvFollows(String filePath);

  /**
   *  Choose a random user_id from the list of follows objects generated from the csv
   */
  int pickRandomUser(List<Follows> follows);

  /**
   * Calculates the number of timelines retrieved from the database per second
   * @param randomUserId a randomly selected user from the follows table
   */
  void trackTimelinesPerSecond(int randomUserId);

  /**
   * From the randomly selected user_id, returns the top 10 most recent tweets from all of the
   * users that user_id follows (referred to as a timeline)
   * @param randomUserId a randomly selected user from the follows table
   * @return List<Tweet>
   */
  List<Tweet> getTimeline(Integer randomUserId);

  ////////////////////// UNUSED METHODS THAT MAY BE USED IN FUTURE IMPLEMENTATIONS //////////////

  /**
   * returns all the followers of user_id
   * @param user_id twitter user to find followers
   * @return
   */
  List<Integer> getFollowers(Integer user_id); // who is following user_id

  /**
   * returns all the followees of user_id
   * @param user_id
   * @return
   */
  List<Integer> getFollowees(Integer user_id); // who is user_id following?

  /**
   * retrieves tweets posted by a user_id
   * @param user_id
   * @return
   */
  List<Tweet> getTweets(Integer user_id); // tweets posted by user_id


  ///////////////////////// METHODS FOR CONNECTING TO THE DATABASE ////////////////////////////////

  /**
   * Set connection settings
   * @param url the url for the database server
   * @param user username for database access
   * @param password password for database access
   */
  void authenticate(String url, String user, String password);

  /**
   * Close the connection when application finishes
   */
  void closeConnection();

}
