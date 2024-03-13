DROP SCHEMA IF EXISTS `EventSource`;

CREATE SCHEMA `EventSource`;
use `EventSource`;

CREATE TABLE `post_events`
(
    `id`        BIGINT  NOT NULL AUTO_INCREMENT,
    `post_id`   VARCHAR(36)  NOT NULL,
    `event`     JSON    NOT NULL,
    `time`      BIGINT  NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `post_snapshots`
(
    `post_id`   VARCHAR(36) NOT NULL,
    `data`      JSON        NOT NULL,
    `cursor`    BIGINT      NOT NULL,
    PRIMARY KEY (`post_id`)
);

CREATE TABLE `views`
(
    `name`      VARCHAR(50) NOT NULL,
    `cursor`    BIGINT      NOT NULL,
    PRIMARY KEY (`name`)
);

CREATE TABLE `post_summary_view`
(
    `cursor`        BIGINT  NOT NULL AUTO_INCREMENT,
    `post_id`       VARCHAR(36) NOT NULL,
    `author_mail`   VARCHAR(50) NOT NULL,
    `title`         VARCHAR(50) NOT NULL,
    `update_time`   BIGINT      NOT NULL,
    `creation_time` BIGINT      NOT NULL,
    PRIMARY KEY (`cursor`),
    CONSTRAINT uc_post_id UNIQUE (post_id)
);

CREATE INDEX idx_author_mail ON post_summary_view (author_mail);