package twitter;

import java.io.FileNotFoundException;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Uses the TweetDatabaseAPI interface. It only explicitly describes the MYSQL database when the API is being
 * instantiated in the variable TweetDatabaseAPI.
 * There is no other description of using MySQL as a database.
 *
 * To access the user name and password, enter the used environment variables to the zshrc system file
 */
public class MainMethod {

  //api instance of mysql database
  private static TweetDatabaseAPI api = new TweetDatabaseRedis();

  public static void main(String[] args) throws FileNotFoundException {


//    // Authenticate access to database server
      String url = "jdbc:mysql://localhost:3306/twitter";

//
//    // Environment variables
//    String user = System.getenv("TWITTER_USER");
//    String password = System.getenv("TWITTER_PASSWORD");
//
    //file paths to .csv files
    String filePathTweets = "/Users/wendionwuakpa/Desktop/Spring 2023/DS4300/hw1_data/tweet.csv";
    String filePathFollows = "/Users/pratheek_mandalapu/Downloads/hw1_data/follows.csv";

    api.trackTweetsPerSecond(api.readCsvTweet(filePathTweets));

//    // Generates a list of tweets from the tweet csv (filepath above) and stores it so that
//    // it may be passed to the trackTweetsPerSecond method call
//    List<Tweet> tweets = api.readCsvTweet(filePathTweets);
//
//    // tracks the number of tweets posted to the database per second
//    api.trackTweetsPerSecond(tweets);
//
//    // Generates a list of follows from the follows csv (filepath above) and stores it so that
//    // it may be passed to the trackTimelinesPerSecond method call
//    List<Follows> follows = api.readCsvFollows(filePathFollows);
//
//    // generates and stores a random user from the set of user_ids found in the follows csv
//    int randomUser = api.pickRandomUser(follows);
//
//    // tracks the number of timelines retrieved from the database per second
//    api.trackTimelinesPerSecond(randomUser);
//
//    //sets the user connections settings
//    api.authenticate(url, user, password);
//
//    //closes the dbutils connection
//    api.closeConnection();
  }

}