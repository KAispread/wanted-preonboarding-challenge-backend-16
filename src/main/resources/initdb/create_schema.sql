-- schema.sql
CREATE TABLE IF NOT EXISTS `performance`
(
    `id`         BINARY(16) default (uuid_to_bin(uuid())) NOT NULL COMMENT '공연/전시 ID',
    `name`       varchar(255)                             NOT NULL COMMENT '공연/전시 이름',
    `price`      INT                                      NOT NULL COMMENT '가격',
    `round`      INT                                      NOT NULL COMMENT '회차',
    `type`       INT                                      NOT NULL COMMENT 'NONE, CONCERT, EXHIBITION',
    `start_date` datetime                                 NOT NULL COMMENT '공연 일시',
    `is_reserve` varchar(255)                             NOT NULL DEFAULT 'disable',
    `created_at` DATETIME   DEFAULT NOW()                 NOT NULL,
    `updated_at` DATETIME   DEFAULT NOW()                 NOT NUll,
    PRIMARY KEY (id),
    UNIQUE KEY (id, round)
);

CREATE TABLE IF NOT EXISTS `performance_seat_info`
(
    `id`             INT(10)                NOT NULL AUTO_INCREMENT,
    `performance_id` BINARY(16)             NOT NULL COMMENT '공연전시ID',
    `round`          INT                    NOT NULL COMMENT '회차(FK)',
    `gate`           INT                    NOT NULL COMMENT '입장 게이트',
    `line`           CHAR(2)                NOT NULL COMMENT '좌석 열',
    `seat`           INT                    NOT NULL COMMENT '좌석 행',
    `is_reserve`     varchar(255)           NOT NULL default 'enable',
    `created_at`     DATETIME DEFAULT NOW() NOT NULL,
    `updated_at`     DATETIME DEFAULT NOW() NOT NUll,
    PRIMARY KEY (id),
    UNIQUE KEY performance_seat_info_unique (performance_id, round, `line`, seat)
);

CREATE TABLE IF NOT EXISTS `reservation`
(
    `id`             INT(10)                NOT NULL AUTO_INCREMENT,
    `performance_id` BINARY(16)             NOT NULL COMMENT '공연전시ID',
    `name`           varchar(255)           NOT NULL COMMENT '예약자명',
    `phone_number`   varchar(255)           NOT NULL COMMENT '예약자 휴대전화 번호',
    `round`          INT                    NOT NULL COMMENT '회차(FK)',
    `gate`           INT                    NOT NULL COMMENT '입장 게이트',
    `line`           CHAR                   NOT NULL COMMENT '좌석 열',
    `seat`           INT                    NOT NULL COMMENT '좌석 행',
    `created_at`     DATETIME DEFAULT NOW() NOT NULL,
    `updated_at`     DATETIME DEFAULT NOW() NOT NUll,
    PRIMARY KEY (id),
    UNIQUE KEY reservation_round_row_seat (performance_id, round, `line`, seat)
);

CREATE TABLE IF NOT EXISTS `discount`
(
    `id`             INT(10)                NOT NULL AUTO_INCREMENT,
    `performance_id` BINARY(16)             NOT NULL COMMENT '공연전시ID',
    `name`           varchar(255)           NOT NULL COMMENT '할인명',
    `type`           TINYINT                NOT NULL COMMENT '할인 종류',
    `amount`         INT                    NOT NULL COMMENT '할인률 || 할인 금액',
    `end_date`       DATETIME               NOT NULL COMMENT '유효기간',
    `created_at`     DATETIME DEFAULT NOW() NOT NULL,
    `updated_at`     DATETIME DEFAULT NOW() NOT NUll,
    PRIMARY KEY (id),
    UNIQUE KEY discount_name_type_amount_unique (`name`, `type`, `amount`, `end_date`)
);

CREATE TABLE IF NOT EXISTS `notification`
(
    `id`             INT(10)                NOT NULL AUTO_INCREMENT,
    `target_id`      varchar(255)           NOT NULL COMMENT '알람 주체 ID (ex Performance)',
    `name`           varchar(255)           NOT NULL COMMENT '알람 등록자명',
    `phone_number`   varchar(255)           NOT NULL COMMENT '알람 등록자 휴대전화 번호',
    `created_at`     DATETIME DEFAULT NOW() NOT NULL,
    `updated_at`     DATETIME DEFAULT NOW() NOT NUll,
    PRIMARY KEY (id),
    UNIQUE KEY notification_target_name_phone (`target_id`, `name`, `phone_number`)
);