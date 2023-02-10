package twitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Random;
import redis.clients.jedis.Jedis;

// timelines can be a list of tweet_ids, while retrieving it i can just get the first ten
// getTimeline returns a list of tweet_id, and then map it to the corresponding values.
// when posting a new tweet -  the values as a map of tweet_ts, user_id, tweet_text
//keeps track of the followers - all the users that follow user_id
// followers - key (user_id), followers (list of followers) using Lpush, Rpush
// 1 2 3 - 4 1 2 3
// 1 3 2 -  1 3 2 4
// flushAll after running every postTweet in intialization of APi
//tweet_db.set(next_tweet_id, 0) then INCR this next_tweetI-d and GET it and set it to tweet_id
// keep track of next_tweet_id
    /* todo
    // make list of followers of each user_id
    // method to get the followers of user_id
    // call post tweet
      // for each user posting a tweet, search the list of followers
      //  make timeline for each user_id - using lset user_id tweet_id
      // timeline structure - key: tweet_id, value fields:user_id, tweet_ts, tweet_text
      // add the tweet_id to timeline of every follower
      // lrange timeline 0 9
     */
public class TweetDatabaseRedis implements TweetDatabaseAPI {
  Jedis tweet_db;

  //constrcutor
  TweetDatabaseRedis() {
     tweet_db = new Jedis("http://localhost:6379"); //todo do not have in both
    tweet_db.flushDB();

  }

  //variable used to generate unique ids for each twitter object
  int unique_tweet_id = 0;
  String filePathFollows = "/Users/wendionwuakpa/Desktop/Spring 2023/DS4300/hw1_data/follows.csv";

  //todo: seperate outfunctions here
  @Override
  public void postTweet(Tweet t) {

    int unique_id = this.getUniqueId();
    String tweet_id = String.valueOf(unique_id);
    String user_id = String.valueOf(t.getUser_id());
    String tweet_ts = currentDateTime();
    String tweet_text = t.getTweet_text();

    List<String> followersUser_id = getFollowers(user_id);
    //System.out.println("User" + user_id + "is followed by: " + followersUser_id); //todo: returns empty
    try {
      System.out.println(tweet_id + user_id + tweet_ts + tweet_text);
      postToRedis(tweet_id, user_id, tweet_ts, tweet_text);
      postToTimelines(followersUser_id, tweet_id);

      tweet_db.close();
    } catch (Exception e) {
      System.err.println("ERROR: Could not post tweet");
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }

  private void postToRedis(String tweet_id, String user, String ts, String text) {
    tweet_db.hset(tweet_id, "user_id", user);
    tweet_db.hset(tweet_id, "tweet_ts", ts);
    tweet_db.hset(tweet_id, "tweet_text", text);
  }

  private void postToTimelines(List<String> followersUser_id, String tweet_id) {
    for (String follower_id_string : followersUser_id) {
      String timeline_follower_id_string = follower_id_string;
      tweet_db.lpush(follower_id_string, tweet_id); //push tweet_text, retrieve tweet_text, getTImeline needs to extract tweet obj from tweet id
      List<String> timelines = tweet_db.lrange(timeline_follower_id_string, 0, 9);
      System.out.println("For user: " + timeline_follower_id_string + " the timeline is: " + timelines);
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

  //////////////////////// METHODS FOR TRACKING TIMELINES RETRIEVED //////////////////////////////
  List<Follows> follows = new ArrayList<>();

  /**
   * Reads follows csv and
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

//  public String[] readCsvFollows(String filePath) {
//    List<String> recordsList = new ArrayList<>();
//    String[] records = new String[recordsList.size()];
//    List<String> record = new ArrayList<String>();
//    try {
//      BufferedReader reader = new BufferedReader(new FileReader(filePath));
//      reader.readLine(); //skip the first row
//      String line;
//      //tweet_db.flushDB();
//      while ((line = reader.readLine()) != null) {
//        records = line.split(",");
//        recordsList = Arrays.asList(records);
//
//        //TODO: changed returning a follows object ; do not need it.
//      }
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//    System.out.println(recordsList);
//    for(String s : recordsList) {
//      System.out.println(s);
//    }
//
//    return records;
//  }

  /**
   * Returns the list of followers of user_id
   *
   * @param user_id provided user_id to find followers.
   * @return
   */
  public List<String> getFollowers(String user_id) {
    List<String> followers = new ArrayList<String>();
    List<Follows> follows = readCsvFollows(filePathFollows);
    String follows_id = "";

      //iterate through list of Follows object
      for(int i = 0; i < follows.size(); i++) {
        // find followers for user_id passed in parameter
        if(Integer.valueOf(user_id) == follows.get(i).getFollows_id()) {
          //find who follows user_id
          String user_following_userid = String.valueOf(follows.get(i).getUser_id());
          tweet_db.lpush(String.valueOf(user_id), user_following_userid);
        }
    }

//    System.out.println("User:" + user_id + "Followsid: " + follows_id);
//    String keyName = "Followers:" + follows_id;
//    tweet_db.lpush(keyName, user_id);
    followers = tweet_db.lrange(user_id, 0, -1);

    //String follows_id = records[1];

//    System.out.println("User:"+  user_id + "Followsid: " + follows_id);
//    String keyName = "Followers:" + follows_id;
//    tweet_db.lpush(keyName, user_id);
//    followers = tweet_db.lrange(keyName, 0, -1);
//    System.out.println(followers);

    return followers;
  }
  //todo
  //


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

//  public Follows createFollow(String[] records) {
//    String user_id = records[0];
//    String follows_id = records[1];
//    try {
//      //set the user_id as key and follows_id as the value
//      tweet_db.set(user_id, follows_id);
//      List<String> following = new ArrayList<String>();
//      String s = tweet_db.get(String.valueOf(user_id));
//    } catch (Exception e) {
//      System.err.println("ERROR: Could not add follows to Redis");
//      System.err.println(e.getMessage());
//      e.printStackTrace();
//    }
//    //System.out.println("User " + user_id + " follows " + tweet_db.get(user_id));
//    return new Follows(Integer.valueOf(user_id), Integer.valueOf(follows_id));
//  }

  /**
   * Choose a random user_id from the list of follows objects generated from the csv
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
    //fetch timelines from posttweet method of a radnom user
    return null;
  }

  /**
   * Returns a list of users following user_id
   *
   * @param user_id twitter user to find followers
   * @return list of integers
   */
//  public List<Integer> getFollowers(Integer user_id) {
//    List<Integer> followers = new ArrayList<Integer>();
//    List<String> resultFollowers = new ArrayList<String>();
//    //string argument for lpush
//    String user_id_string = String.valueOf(user_id);
//    //iterate through list of Follows object
//    for (int i = 0; i < follows.size(); i++) {
//      // find followers for user_id passed in parameter
//      if (user_id == follows.get(i).getFollows_id()) {
//        //find who follows user_id
//        String user_following_userid = String.valueOf(follows.get(i).getUser_id());
//        tweet_db.lpush(user_id_string, user_following_userid);
//      }
//      resultFollowers = tweet_db.lrange(user_id_string, 0, -1);
//      //System.out.println(resultFollowers);
//    }
//    //convert from List<String> to List<Integer>    for(String s: resultFollowers) {
//    followers.add(Integer.parseInt(s));
//
//    return followers;
//}
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
//    /*
//    List<Integer>//    iterate through list of followers, where follows_id = user_id;
//    lpush followers follows[0]
//     */
//  }
/**
 * Returns list of users user_id is following
 */

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