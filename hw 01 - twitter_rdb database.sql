CREATE SCHEMA twitter;

USE twitter;

CREATE TABLE tweet (
	tweet_id INT PRIMARY KEY,
	user_id INT,
    tweet_ts DATETIME,
    tweet_text VARCHAR(140)
    );

CREATE TABLE follows (
	user_id INT,
    follows_id INT
    );
    

