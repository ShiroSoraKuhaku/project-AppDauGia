CREATE DATABASE IF NOT EXISTS daugiadb;
USE daugiadb;
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: daugiadb
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `items`
--
-- Drop order: child tables first, then parent tables
DROP TABLE IF EXISTS `auction_auto_bids`;
DROP TABLE IF EXISTS `auction_bids`;
DROP TABLE IF EXISTS `auctions`;
DROP TABLE IF EXISTS `bids`;
DROP TABLE IF EXISTS `tokens`;
DROP TABLE IF EXISTS `items`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `username` varchar(45) NOT NULL,
    `password` varchar(45) DEFAULT NULL,
    `role` enum('USER','ADMIN') NOT NULL DEFAULT 'USER',
    PRIMARY KEY (`username`),
    UNIQUE KEY `username_UNIQUE` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `user` (`username`, `password`, `role`)
VALUES ('admin', 'admin', 'ADMIN');

CREATE TABLE `items` (
  `id` int NOT NULL AUTO_INCREMENT,
  `seller_username` varchar(45) NOT NULL,
  `name` varchar(45) NOT NULL,
  `price` double NOT NULL,
  `desc` varchar(255) DEFAULT NULL,
  `item_spec` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_items_seller`
      FOREIGN KEY (`seller_username`) REFERENCES `user` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tokens` (
  `username` varchar(45) NOT NULL,
  `token` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`username`),
  UNIQUE KEY `token_UNIQUE` (`token`),
  CONSTRAINT `username` FOREIGN KEY (`username`) REFERENCES `user` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `bids` (
    `id` int NOT NULL AUTO_INCREMENT,
    `item_id` int NOT NULL,
    `bidder_username` varchar(45) NOT NULL,
    `price` double NOT NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_bids_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
    CONSTRAINT `fk_bids_user` FOREIGN KEY (`bidder_username`) REFERENCES `user` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- -------------------------------------------------------
-- Auction tables
-- -------------------------------------------------------

CREATE TABLE `auctions` (
  `id`          int NOT NULL AUTO_INCREMENT,
  `item_id`     int NOT NULL,
  `title`       varchar(255) NOT NULL,
  `status`      enum('SCHEDULED','ACTIVE','ENDED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
  `start_price` double NOT NULL,
  `cur_price`   double NOT NULL,
  `cur_leader`  varchar(45) DEFAULT NULL,
  `start_time`  datetime NOT NULL,
  `end_time`    datetime NOT NULL,
  `version`     int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_auctions_status_time` (`status`, `start_time`, `end_time`),
  CONSTRAINT `fk_auctions_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `auction_bids` (
  `id`               int NOT NULL AUTO_INCREMENT,
  `auction_id`       int NOT NULL,
  `bidder_username`  varchar(45) NOT NULL,
  `bid_amount`       double NOT NULL,
  `created_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_auction_bids_auction_time` (`auction_id`, `created_at` DESC),
  CONSTRAINT `fk_bids_auction`  FOREIGN KEY (`auction_id`)      REFERENCES `auctions` (`id`),
  CONSTRAINT `fk_bids_bidder`   FOREIGN KEY (`bidder_username`) REFERENCES `user` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `auction_auto_bids` (
    `id` int NOT NULL AUTO_INCREMENT,
    `auction_id` int NOT NULL,
    `bidder_username` varchar(45) NOT NULL,
    `max_amount` double NOT NULL,
    `active` boolean NOT NULL DEFAULT true,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_auto_bid_auction_user` (`auction_id`, `bidder_username`),
    KEY `idx_auto_bid_auction_amount` (`auction_id`, `max_amount` DESC),
    CONSTRAINT `fk_auto_bid_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`),
    CONSTRAINT `fk_auto_bid_user` FOREIGN KEY (`bidder_username`) REFERENCES `user` (`username`)
);
