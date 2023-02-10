package twitter;

//imports
import database.DBUtils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The MySQL implementation of the TwitterDatabaseAPI Interface
 * Allows for Twitter functionality when using a MySQL database
 */
public class TweetDatabaseMySQL implements TweetDatabaseAPI {


  // pass the parameters of a DBUtils Object to main method to dynamically construct a DB
  // (applies for any class)
  DBUtils dbu = new DBUtils("jdbc:mysql://localhost:3306/twitter",
          System.getenv("TWITTER_USER"), System.getenv("TWITTER_PASSWORD"));

  //variable used to generate unique ids for each twitter object
  int unique_tweet_id = 0;

  ////////////////////////////// METHODS FOR TRACKING TWEET POSTS ////////////////////////////////
  /**
   * Posts a Tweet object (user_id, tweet_text) to the tweet table in the desired database
   * @param t this tweet
   */
  public void postTweet(Tweet t) {
    int tweet_id = getUniqueId();
    int user_id = t.getUser_id();
    String tweet_ts = currentDateTime();
    String tweet_text = t.getTweet_text();

    String sql = "INSERT INTO tweet (tweet_id, user_id, tweet_ts, tweet_text) VALUES " + "(" +
            tweet_id + ", " +
            user_id + ", " + "\"" +
            tweet_ts+ "\", " + "\"" +
            tweet_text + "\")";

    try {

      // get connection and initialize statement
      Connection con = dbu.getConnection(); // get the active connection
      Statement stmt = con.createStatement();

      stmt.executeUpdate(sql);

      // Cleanup
      stmt.close();

    } catch (SQLException e) {
      System.err.println("ERROR: Could not post tweet: " + sql);
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
    return unique_tweet_id++;
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

  /**
   * Creates a list of tweets, generated from the given tweet csv
   * @param filePath the filepath of the given tweet csv
   * @return a list of tweets
   */
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

  /**
   * Tracks the number of tweets posted per second, every 10,000 inserts into the databse
   * @param values the tweets to be posted (inserted) to the database
   */
  public void trackTweetsPerSecond(List<Tweet> values) {
    int counter = 0;
    double startTime = System.currentTimeMillis();

    for (int i = 0; i < values.size(); i+=5) {
      postTweet(values.get(i));
      counter++;

      if (counter % 10000 == 0 || i == values.size() - 1) {
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

  /**
   * Creates a list of follows objects (user_id, follows_id), generated from the given follows csv
   * @param filePath the filepath of the given follows csv
   * @return a list of follows
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
    return null;
  }

  /**
   * Creates a Follows object from the String Array of records generated from readCsvFollows
   * @param records the user_id and tweet_text record read from the follows csv
   * @return a Follows object
   */
  private static Follows createFollow(String[] records) {
    int user_id = Integer.parseInt(records[0]);
    int follows_id = Integer.parseInt(records[1]);
    return new Follows(user_id, follows_id);
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
   * @param randomUserId a randomly selected user from the follows table
   */
  public void trackTimelinesPerSecond(int randomUserId) {
    double start_time = System.currentTimeMillis();
    int counter = 0;
    while(true) {
      getTimeline(randomUserId);
      counter++;
      double end = System.currentTimeMillis();
      double timeTaken = (end - start_time) / 1000.0;
      System.out.println("Timelines per second: " + counter/timeTaken);
    }

  }


  /**
   * From the randomly selected user_id, returns the top 10 most recent tweets from all of the
   * users that user_id follows (referred to as a timeline)
   * @param randomUserId a randomly selected user from the follows table
   * @return List<Tweet>
   */
  public List<Tweet> getTimeline(Integer randomUserId) {

    //initialize list of tweets to store timeline
    List<Tweet> timeline = new ArrayList<Tweet>();

    //while loop generate random user and call get timeline in main. call the method here

    String sql =
            "SELECT tweet_id, tweet.user_id, tweet_text, tweet_ts "
                    + "FROM tweet "
                    + "JOIN follows ON follows.follows_id = tweet.user_id "
                    + "WHERE follows.user_id =" + randomUserId
                    + " ORDER BY tweet_ts DESC LIMIT 10";

    try {

      // get connection and initialize statement
      Connection con = dbu.getConnection(); // get the active connection
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(sql); //execute the query and save in a result set

      while(rs.next()) {
        Tweet addedTweet = new Tweet(rs.getInt("user_id"),
                rs.getString("tweet_text"));

        timeline.add(addedTweet);
      }
      stmt.close();

    } catch (SQLException e) {
      System.err.println("ERROR: Could not retrieve user timelines: " + sql);
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
    return timeline;
  }

  @Override
  public List<String> getFollowers(String user_id) {
    return null;
  }

//  @Override
//  public List<String> getFollowers(String user_id) {
//    return null;
//  }

////////////////////// UNUSED METHODS THAT MAY BE USED IN FUTURE IMPLEMENTATIONS //////////////

  /**
   * Returns a list of the users following user_id
   */
  public List<Integer> getFollowers(Integer user_id) {
    List<Integer> followersIds = new ArrayList<Integer>();
    String sql = "SELECT user_id from follows "
        + "WHERE follows_id" + user_id;

    try {

      //get connection and intialize statement
      Connection con = dbu.getConnection();
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(sql);;

      while(rs.next()) {
        followersIds.add(rs.getInt("user_id"));
      }

      stmt.close();

    } catch(SQLException e) {
      System.err.println("ERROR: Could not retrieve user " + user_id + " followers");
      System.err.println(e.getMessage());
      e.printStackTrace();

    }

    return followersIds;
  }


  /**
   * Returns list of users user_id is following
   */
  public List<Integer> getFollowees(Integer user_id) {
    List<Integer> following = new ArrayList<Integer>();

    String sql = "SELECT follows_id FROM follows "
        + "WHERE user_id = " + user_id;

    try {
      Connection con = dbu.getConnection();
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      following.add(rs.getInt("follows_id"));

    } catch(SQLException e) {
      System.err.println("Error: Could not retrieve user's followees");
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
    return following;

  }

  /**
   * returns the list of tweets posted by a user
   * @param user_id
   * @return
   */
  public List<Tweet> getTweets(Integer user_id) {
    List<Tweet> tweetsPosted = new ArrayList<Tweet>();
    String sql = "SELECT user_id, tweet_text FROM tweets where user_id =" + user_id;

    try {
      Connection con = dbu.getConnection();
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      while(rs.next()) {
        tweetsPosted.add(new Tweet(rs.getInt(0), rs.getString(1)));
      }

    } catch(SQLException e) {
      System.err.println("Error: Could not retrieve user's followees");
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
    return tweetsPosted;

  }


  ///////////////////////// METHODS FOR CONNECTING TO THE DATABASE ////////////////////////////////

  /**
   * T
   * @param url the url for the database server
   * @param user username for database access
   * @param password password for database access
   */
  ////////////////////// authenticastion methods (update constructor) ////////////////
  public void authenticate(String url, String user, String password) {

    dbu = new DBUtils(url, user, password);
  }

  /**
   * Close the connection when application finishes
   */
  public void closeConnection() {
    dbu.closeConnection();
  }

}
