-- phpMyAdmin SQL Dump
-- version 4.5.4.1deb2ubuntu2
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Mar 01, 2017 at 09:51 AM
-- Server version: 5.7.17-0ubuntu0.16.04.1
-- PHP Version: 7.0.15-0ubuntu0.16.04.2

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `MovieDiary`
--

-- --------------------------------------------------------

--
-- Table structure for table `Movies`
--

CREATE TABLE `Movies` (
  `Id`    INT(11)      NOT NULL,
  `Title` VARCHAR(100) NOT NULL,
  `Year`  SMALLINT(6)  NOT NULL
)
  ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Settings`
--

CREATE TABLE `Settings` (
  `Id`          INT(11)      NOT NULL,
  `Username`    VARCHAR(100) DEFAULT NULL,
  `RuntimeType` VARCHAR(100) NOT NULL,
  `Language`    VARCHAR(100) NOT NULL
)
  ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Users`
--

CREATE TABLE `Users` (
  `Id`        INT(11)      NOT NULL,
  `Firstname` VARCHAR(100) NOT NULL,
  `Lastname`  VARCHAR(100) NOT NULL,
  `Username`  VARCHAR(100) NOT NULL,
  `Password`  VARCHAR(100)          DEFAULT NULL,
  `Salt`      VARCHAR(16)  NOT NULL DEFAULT 'default'
)
  ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Views`
--

CREATE TABLE `Views` (
  `Id`        INT(11)      NOT NULL,
  `Username`  VARCHAR(100) NOT NULL,
  `MovieId`   INT(11)      NOT NULL,
  `Start`     TIMESTAMP    NULL DEFAULT NULL,
  `End`       TIMESTAMP    NULL DEFAULT NULL,
  `WasFirst`  TINYINT(1)   NOT NULL,
  `WasCinema` TINYINT(1)   NOT NULL
)
  ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Wishlist`
--

CREATE TABLE `Wishlist` (
  `Id`      INT(11) NOT NULL,
  `UserId`  INT(11) NOT NULL,
  `MovieId` INT(11) NOT NULL
)
  ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `Movies`
--
ALTER TABLE `Movies`
  ADD PRIMARY KEY (`Id`);

--
-- Indexes for table `Settings`
--
ALTER TABLE `Settings`
  ADD PRIMARY KEY (`Id`);

--
-- Indexes for table `Users`
--
ALTER TABLE `Users`
  ADD PRIMARY KEY (`Id`);

--
-- Indexes for table `Views`
--
ALTER TABLE `Views`
  ADD PRIMARY KEY (`Id`);

--
-- Indexes for table `Wishlist`
--
ALTER TABLE `Wishlist`
  ADD PRIMARY KEY (`Id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `Movies`
--
ALTER TABLE `Movies`
  MODIFY `Id` INT(11) NOT NULL AUTO_INCREMENT,
  AUTO_INCREMENT = 376867;
--
-- AUTO_INCREMENT for table `Settings`
--
ALTER TABLE `Settings`
  MODIFY `Id` INT(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `Users`
--
ALTER TABLE `Users`
  MODIFY `Id` INT(11) NOT NULL AUTO_INCREMENT,
  AUTO_INCREMENT = 65;
--
-- AUTO_INCREMENT for table `Views`
--
ALTER TABLE `Views`
  MODIFY `Id` INT(11) NOT NULL AUTO_INCREMENT,
  AUTO_INCREMENT = 150;
--
-- AUTO_INCREMENT for table `Wishlist`
--
ALTER TABLE `Wishlist`
  MODIFY `Id` INT(11) NOT NULL AUTO_INCREMENT;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
