DROP SCHEMA IF EXISTS `EventSource`;

CREATE SCHEMA `EventSource`;
use `EventSource`;

CREATE TABLE `views`
(
    `name`      VARCHAR(50) NOT NULL,
    `cursor`    BIGINT      NOT NULL,
    PRIMARY KEY (`name`)
);

CREATE TABLE `person_events`
(
    `id`    BIGINT  NOT NULL AUTO_INCREMENT,
    `pk`    BIGINT  NOT NULL,
    `event` JSON    NOT NULL,
    `time`  BIGINT  NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `person_snapshots`
(
    `pk`        BIGINT  NOT NULL,
    `data`      JSON    NOT NULL,
    `cursor`    BIGINT  NOT NULL,
    PRIMARY KEY (`pk`)
);

CREATE TABLE `person_summary_view`
(
    id      BIGINT      NOT NULL,
    name    VARCHAR(50) NOT NULL,
    PRIMARY KEY (`id`)
);