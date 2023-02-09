package twitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.Jedis;

public class TweetDatabaseRedis implements TweetDatabaseAPI {
  Jedis tweet_db = new Jedis("http://localhost:6379");

  //variable used to generate unique ids for each twitter object
  int unique_tweet_id = 0;

  @Override
  public void postTweet(Tweet t) {
    int unique_id = this.getUniqueId();
    String tweet_id = String.valueOf(unique_id);
    String user_id = String.valueOf(t.getUser_id());
    String tweet_ts = currentDateTime();
    String tweet_text = t.getTweet_text();

    try {
      //todo INCR with Redis is slower? ~3500 tweets/sec
//      tweet_db.set("tweet_id", "0");
//      String tweet_id = String.valueOf(tweet_db.incr("tweet_id"));
      tweet_db.hset(tweet_id, "user_id", user_id);
      tweet_db.hset(tweet_id, "tweet_ts", tweet_ts);
      tweet_db.hset(tweet_id, "tweet_text", tweet_text);
      tweet_db.flushDB(); //todo do we need to flush after eveyr run

      tweet_db.close();
    }
    catch (Exception e) {
      System.err.println("ERROR: Could not post tweet");
      System.err.println(e.getMessage());
      e.printStackTrace();
    }

  }

  /**
   * generates a unique integer for a tweet_id by incrementing a counter by 1 each time
   * postTweet is run
   * @return tweet_id
   */
  //todo: improve the runtime of getting unique id
  public int getUniqueId() {
    unique_tweet_id = unique_tweet_id + 1;
    return unique_tweet_id;
  }

  /**
   * Returns the current date and time in YYYY-MM-DD hh:mm:ss format
   * @return current date and time
   */
  public static String currentDateTime() {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime now = LocalDateTime.now();
    return dtf.format(now);
  }

  @Override
  public List<Tweet> readCsvTweet(String filePath) {
    List<Tweet> tweets = new ArrayList<>();

    try {
      BufferedReader reader = new BufferedReader(new FileReader(filePath));
      reader.readLine(); //skip the first row
      String line;
      while ((line = reader.readLine()) != null) {

        String[] records = line.split(",");
        Tweet tweet = createTweet(records);
        tweets.add(tweet);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return tweets;
  }

  /**
   * Creates a Tweet object from the String Array of records generated from readCsvTweet
   * @param records the user_id and tweet_text record read from the tweet csv
   * @return a Tweet
   */
  public Tweet createTweet(String[] records) {
    int user_id = Integer.parseInt(records[0]);
    String tweet_text = records[1];
    return new Tweet(user_id, tweet_text);
  }

  @Override
  public void trackTweetsPerSecond(List<Tweet> tweets) {
    int counter = 0;
    double startTime = System.currentTimeMillis();

    for (int i = 0; i < tweets.size(); i+=5) {
      postTweet(tweets.get(i));
      counter++;

      if (counter % 10000 == 0 || i == tweets.size() - 1) {
        double endTime = System.currentTimeMillis();
        double timeTaken = (endTime - startTime) / 1000.0;
        double tweetsPerSecond = 10000.0 / timeTaken;
        System.out.println("Tweets per second: " + tweetsPerSecond);;
        counter = 0;
        startTime = System.currentTimeMillis();
      }
    }
  }

  //////////////////////// METHODS FOR TRACKING TIMELINES RETRIEVED //////////////////////////////

  @Override
  public List<Follows> readCsvFollows(String filePath) {
    return null;
  }

  @Override
  public int pickRandomUser(List<Follows> follows) {
    return 0;
  }

  @Override
  public void trackTimelinesPerSecond(int randomUserId) {

  }

  @Override
  public List<Tweet> getTimeline(Integer randomUserId) {
    return null;
  }

  @Override
  public List<Integer> getFollowers(Integer user_id) {
    return null;
  }

  @Override
  public List<Integer> getFollowees(Integer user_id) {
    return null;
  }

  @Override
  public List<Tweet> getTweets(Integer user_id) {
    return null;
  }

  @Override
  public void authenticate(String url, String user, String password) {

  }

  @Override
  public void closeConnection() {

  }

  // timelines can be a list of tweet_ids, while retrieving it i can just get the first ten
  // getTimeline returns a list of tweet_id, and then map it to the corresponding values.

  // when posting a new tweet - could have the values as a map of tweet_ts, user_id, tweet_text

  //keeps track of the followers - all the users that follow user_id
  // followers - key (user_id), followers (list of followers) using Lpush, Rpush
  // 1 2 3 -
  // 1 3 2

  // flushAll after running every postTweet in intialization of APi
  //tweet_db.set(next_tweet_id, 0) then INCR this next_tweetI-d and GET it and set it to tweet_id
  // keep track of next_tweet_id

    /* todo
    // make timeline for each user_id - tweet_ids
    // make list of followers of each user_id
    // method to get the followers of user_id
      // timeline structure - key: tweet_id, value fields:user_id, tweet_ts, tweet_text
      // add the tweet_id to timeline of every follower
      // lrange timeline 0 9

     */
}
