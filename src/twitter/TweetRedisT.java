package twitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jdk.swing.interop.SwingInterOpUtils;
import redis.clients.jedis.Jedis;

public class TweetRedisT implements TweetDatabaseAPI {
  Jedis tweet_db1;

  //constructor
  TweetRedisT() {
    tweet_db1 = new Jedis("http://localhost:6379"); //todo do not have in both
    tweet_db1.flushDB();
    tweet_db1.set("id", "0");
  }

  //variable used to generate unique ids for each twitter object
  int unique_tweet_id = 0;

  //follows.csv
  String filePathFollows = "/Users/wendionwuakpa/Desktop/Spring 2023/DS4300/hw1_data/follows.csv";

  @Override
  //posts a tweet to a users timeline
  public void postTweet(Tweet t) {
    //String tweet_id = String.valueOf(this.getUniqueId());
    String tweet_id = String.valueOf(tweet_db1.incr(tweet_db1.get("id")));
    String user_id = String.valueOf(t.getUser_id());
    String tweet_ts = currentDateTime();
    String tweet_text = t.getTweet_text();

    List<String> followersUser_id = getFollowers(user_id);

    try {
      postToRedis(tweet_id, user_id, tweet_ts, tweet_text);
      postToTimelines(followersUser_id, tweet_id);

      tweet_db1.close();
    } catch (Exception e) {
      System.err.println("ERROR: Could not post tweet");
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Uses hset to add a tweet_id key to the Redis DB
   * @param tweet_id
   * @param user
   * @param ts
   * @param text
   */
  private void postToRedis(String tweet_id, String user, String ts, String text) {
    tweet_db1.hset(tweet_id, "user_id", user);
    tweet_db1.hset(tweet_id, "tweet_ts", ts);
    tweet_db1.hset(tweet_id, "tweet_text", text);
  }

  /**
   * Posts a Tweet using its tweet_id to the timelines of user_id's followers.
   */
  private void postToTimelines(List<String> followers_userid, String tweetPosted_tweetid) {
    List<String> timelines = new ArrayList<String>();
    String keyName;
    for(String follower : followers_userid) {
      keyName = "Timeline:"+ follower;
      //push the tweet posted to the user's timeline
      tweet_db1.lpush(keyName, tweetPosted_tweetid);
      timelines = tweet_db1.lrange(keyName, 0, -1);
      //System.out.println("The timeline is " + keyName + "\n" + timelines);
    }

  }

  /**
   * generates a unique integer for a tweet_id by incrementing a counter by 1 each time postTweet is
   * run
   *
   * @return tweet_id
   */
  //todo: improve the runtime of getting unique id
  public int getUniqueId() {
    unique_tweet_id = unique_tweet_id + 1;
    return unique_tweet_id;
  }

  /**
   * Returns the current date and time in YYYY-MM-DD hh:mm:ss format
   *
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
   *
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
    //todo
    for (int i = 0; i < tweets.size(); i += 5) {
      postTweet(tweets.get(i));
      counter++;
      if (counter % 10000 == 0 || i == tweets.size() - 1) {
        double endTime = System.currentTimeMillis();
        double timeTaken = (endTime - startTime) / 1000.0;
        double tweetsPerSecond = 10000.0 / timeTaken;
        System.out.println("Tweets per second: " + tweetsPerSecond);
        ;
        counter = 0;
        startTime = System.currentTimeMillis();
      }
    }
  }

  /**
   * Reads follows csv and returns list of Follows
   *
   * @param filePath the filepath of the given follows csv
   * @return
   */
  public List<Follows> readCsvFollows(String filePath) {
    List<Follows> follows = new ArrayList<>();

    try {
      BufferedReader reader = new BufferedReader(new FileReader(filePath));
      reader.readLine(); //skip the first row
      String line;
      while ((line = reader.readLine()) != null) {

        String[] records = line.split(",");
        Follows follow = createFollow(records);
        follows.add(follow);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return follows;
  }

  /**
   * Creates a Follows object from the String Array of records generated from readCsvFollows
   *
   * @param records the user_id and tweet_text record read from the follows csv
   * @return a Follows object
   */
  private static Follows createFollow(String[] records) {
    int user_id = Integer.parseInt(records[0]);
    int follows_id = Integer.parseInt(records[1]);
    return new Follows(user_id, follows_id);
  }

  /**
   * List of followers of user_id
   * @param user_id twitter user to find followers
   * @return
   */
  public List<String> getFollowers(String user_id) {
    List<String> followers = new ArrayList<String>();
    List<Follows> follows = readCsvFollows(filePathFollows);
    String keyname = "FollowersOf" + user_id;;

    Integer user_id_int = Integer.valueOf(user_id);

    for(Follows f : follows) {
      // find corresponding row with user_id == follows_id
      if(user_id_int == f.getFollows_id()) {
        // find follower of user_id
        String follower_user_id = String.valueOf(f.getUser_id());
//        System.out.println("The follower added is " + follower_user_id);
        tweet_db1.lpush(keyname, follower_user_id);
      }
    }
    followers = tweet_db1.lrange(keyname, 0, -1);
    //System.out.println("The followers of " + user_id + " is " + followers);
    return followers;
  }
  /**
   *  Choose a random user_id from the list of follows objects generated from the csv
   */
  public int pickRandomUser(List<Follows> follows) {
    List<Integer> listOfUsers = new ArrayList<>();
    for (Follows follow : follows) {
      int user = follow.getUser_id();
      listOfUsers.add(user);
    }
    // randomly selects a user from the follows table by indexing a random position within
    // the list of users
    Random rand = new Random();
    int randomIndex = rand.nextInt(listOfUsers.size());
    return listOfUsers.get(randomIndex);
  }

  /**
   * Calculates the number of timelines retrieved from the database per second
   *
   * @param randomUserId a randomly selected user from the follows table
   */
  @Override
  public void trackTimelinesPerSecond(int randomUserId) {
    double start_time = System.currentTimeMillis();
    int counter = 0;
    while (true) {
      getTimeline(randomUserId);
      counter++;
      double end = System.currentTimeMillis();
      double timeTaken = (end - start_time) / 1000.0;
      System.out.println("Timelines per second: " + counter / timeTaken);
    }
  }

  /**
   * Returns the top 10 tweets in the timeline of a user_id
   *
   * @param randomUserId a randomly selected user from the follows table
   * @return
   */
  public List<Tweet> getTimeline(Integer randomUserId) {
    //fetch timelines from post tweet method of a random user

    //list of tweets to store the timeline
    List<Tweet> timeline = new ArrayList<Tweet>();


    //get timelines of tweet_id from posttoTimeline function call
    List<String> timelinesTweet_id = new ArrayList<String>();

    //key to pass to lrange
    String s_randomUserId = String.valueOf(randomUserId);
    //String s_randomUserId = "Timeline:" + String.valueOf(randomUserId);
    timelinesTweet_id = tweet_db1.lrange(s_randomUserId, 0, 9);
    System.out.println(timelinesTweet_id);

    //Iterate through list of tweet_ids and find user_id and tweet_text
    for(String tweet_id : timelinesTweet_id) {
      String user_id = tweet_db1.hget(tweet_id, "user_id");
      String tweet_text = tweet_db1.hget(tweet_id, "tweet_text");
      System.out.println("User of " + tweet_id + " " + user_id);
      System.out.println(tweet_text);
      Tweet tweetPosted = new Tweet(Integer.parseInt(user_id), tweet_text);
      timeline.add(tweetPosted);
    }
    System.out.println(timeline);
    return timeline;
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
}
