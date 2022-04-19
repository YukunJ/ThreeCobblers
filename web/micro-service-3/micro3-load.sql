/* Step 0 clear and create database */
SET global local_infile = TRUE; 

DROP DATABASE IF EXISTS `twitter`; 

CREATE DATABASE `twitter`; 

USE `twitter`; 

-- -------------------------------------------------------

DROP TABLE IF EXISTS `unified_table`;

CREATE TABLE `unified_table`
  (
     `content`              VARCHAR(500) DEFAULT NULL,
     `is_reply`             VARCHAR(10) DEFAULT NULL,
     `receiver`             BIGINT NOT NULL,
     `sender`               BIGINT NOT NULL,
     `tags`                 VARCHAR(250) DEFAULT NULL,
     `both_latest`          VARCHAR(500) DEFAULT NULL,
     `latest_description`   VARCHAR(250) NOT NULL,
     `latest_screen`        VARCHAR(50) NOT NULL,
     `product_score`        DECIMAL(14, 13) NOT NULL,
     `latest_retweet_reply` VARCHAR(500) DEFAULT NULL
  )
engine=myisam
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_bin; 

CREATE INDEX user_index ON unified_table (sender);
-- -------------------------------------------------------

/* Step 2 create the dynamic table2
        for the storage of hashtags and contents of each tweet */
DROP TABLE IF EXISTS `dynamic_table`; 

CREATE TABLE `dynamic_table` 
  ( 
     `is_reply` VARCHAR(10) DEFAULT NULL,
     `content`  VARCHAR(500) DEFAULT NULL, 
     `tags`     VARCHAR(100) DEFAULT NULL, 
     `sender`   BIGINT NOT NULL, 
     `receiver` BIGINT NOT NULL
  ) ENGINE=MyISAM DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

-- ---------------This following part should be executed separately, not as a infile .sql----------------------------------------

-- -------------------------------------------------------
/* Step 4 load data into static table */
cd static_table
for f in *;
do sudo mysql -u clouduser --local-infile=1 -pdbroot -e  "USE twitter; SET NAMES utf8mb4 COLLATE utf8mb4_bin; LOAD DATA LOCAL INFILE '"$f"'  INTO TABLE static_table CHARACTER SET UTF8MB4 fields enclosed by '\"' ESCAPED BY '\b' terminated BY ',' lines terminated by '\r\n\n';"; 
done;
cd ..

-- -------------------------------------------------------
/* Step 5 load data into dynamic table */
cd dynamic_table
for f in *;
do sudo mysql -u clouduser --local-infile=1 -pdbroot -e  "USE twitter; SET NAMES utf8mb4 COLLATE utf8mb4_bin; LOAD DATA LOCAL INFILE '"$f"'  INTO TABLE dynamic_table CHARACTER SET UTF8MB4 fields enclosed by '\"' ESCAPED BY '\b' terminated BY ',' lines terminated by '\r\n\n';"; 
done;
cd ..


CREATE INDEX user_index ON dynamic_table (sender);
CREATE INDEX user_index ON static_table (user_a);
