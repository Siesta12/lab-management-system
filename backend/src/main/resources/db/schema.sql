CREATE DATABASE IF NOT EXISTS `lab_management_system`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `lab_management_system`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `consumable_stock_log`;
DROP TABLE IF EXISTS `user_violation_record`;
DROP TABLE IF EXISTS `reservation_audit_log`;
DROP TABLE IF EXISTS `lab_maintenance`;
DROP TABLE IF EXISTS `lab_reservation_slot`;
DROP TABLE IF EXISTS `lab_reservation`;
DROP TABLE IF EXISTS `lab_open_slot`;
DROP TABLE IF EXISTS `class_period`;
DROP TABLE IF EXISTS `lab_consumable`;
DROP TABLE IF EXISTS `lab_device`;
DROP TABLE IF EXISTS `lab`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `department`;

CREATE TABLE `department` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `department_name` VARCHAR(100) NOT NULL COMMENT 'Department name',
    `department_code` VARCHAR(50) NOT NULL COMMENT 'Department code',
    `leader_name` VARCHAR(50) NOT NULL COMMENT 'Leader name',
    `phone` VARCHAR(20) NOT NULL COMMENT 'Phone',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 deleted',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_department_code_deleted` (`department_code`, `deleted`),
    UNIQUE KEY `uk_department_name_deleted` (`department_name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Department table';

CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `department_id` BIGINT DEFAULT NULL COMMENT 'Department id',
    `username` VARCHAR(50) NOT NULL COMMENT 'Login username',
    `password` VARCHAR(255) NOT NULL COMMENT 'Login password',
    `real_name` VARCHAR(50) NOT NULL COMMENT 'Real name',
    `user_no` VARCHAR(50) NOT NULL COMMENT 'Student or job number',
    `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '0 unknown, 1 male, 2 female',
    `phone` VARCHAR(20) NOT NULL COMMENT 'Phone',
    `email` VARCHAR(100) NOT NULL COMMENT 'Email',
    `credit_score` INT NOT NULL DEFAULT 100 COMMENT 'Credit score',
    `violation_count` INT NOT NULL DEFAULT 0 COMMENT 'Violation count',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 deleted',
    `last_login_at` DATETIME DEFAULT NULL COMMENT 'Last login time',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username_deleted` (`username`, `deleted`),
    UNIQUE KEY `uk_sys_user_user_no_deleted` (`user_no`, `deleted`),
    UNIQUE KEY `uk_sys_user_phone_deleted` (`phone`, `deleted`),
    UNIQUE KEY `uk_sys_user_email_deleted` (`email`, `deleted`),
    KEY `idx_sys_user_department_id` (`department_id`),
    KEY `idx_sys_user_status_deleted` (`status`, `deleted`),
    CONSTRAINT `fk_sys_user_department`
        FOREIGN KEY (`department_id`) REFERENCES `department` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System user table';

CREATE TABLE `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `role_name` VARCHAR(50) NOT NULL COMMENT 'Role name',
    `role_code` VARCHAR(50) NOT NULL COMMENT 'Role code',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 deleted',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_role_code_deleted` (`role_code`, `deleted`),
    UNIQUE KEY `uk_sys_role_role_name_deleted` (`role_name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System role table';

CREATE TABLE `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User id',
    `role_id` BIGINT NOT NULL COMMENT 'Role id',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_role_user_id_role_id` (`user_id`, `role_id`),
    KEY `idx_sys_user_role_role_id` (`role_id`),
    CONSTRAINT `fk_sys_user_role_user`
        FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_sys_user_role_role`
        FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User role mapping table';

CREATE TABLE `lab` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `department_id` BIGINT DEFAULT NULL COMMENT 'Department id',
    `lab_code` VARCHAR(50) NOT NULL COMMENT 'Lab code',
    `lab_name` VARCHAR(100) NOT NULL COMMENT 'Lab name',
    `lab_type` VARCHAR(50) NOT NULL COMMENT 'Lab type',
    `building_name` VARCHAR(100) NOT NULL COMMENT 'Building name',
    `room_no` VARCHAR(50) NOT NULL COMMENT 'Room number',
    `capacity` INT NOT NULL DEFAULT 0 COMMENT 'Capacity',
    `manager_user_id` BIGINT DEFAULT NULL COMMENT 'Manager user id',
    `open_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 open, 0 closed',
    `lab_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 normal, 2 maintenance, 0 disabled',
    `description` VARCHAR(500) DEFAULT NULL COMMENT 'Description',
    `usage_rule` VARCHAR(500) DEFAULT NULL COMMENT 'Usage rule',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 deleted',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lab_lab_code_deleted` (`lab_code`, `deleted`),
    UNIQUE KEY `uk_lab_name_room_deleted` (`lab_name`, `building_name`, `room_no`, `deleted`),
    KEY `idx_lab_department_id` (`department_id`),
    KEY `idx_lab_manager_user_id` (`manager_user_id`),
    KEY `idx_lab_open_status_deleted` (`open_status`, `deleted`),
    CONSTRAINT `fk_lab_department`
        FOREIGN KEY (`department_id`) REFERENCES `department` (`id`),
    CONSTRAINT `fk_lab_manager_user`
        FOREIGN KEY (`manager_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Lab table';

CREATE TABLE `class_period` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `period_no` INT NOT NULL COMMENT 'Unique period number',
    `period_name` VARCHAR(50) NOT NULL COMMENT 'Period name',
    `start_time` TIME NOT NULL COMMENT 'Start time',
    `end_time` TIME NOT NULL COMMENT 'End time',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_period_period_no` (`period_no`),
    KEY `idx_class_period_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Class period table';

CREATE TABLE `lab_open_slot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `lab_id` BIGINT NOT NULL COMMENT 'Lab id',
    `weekday` TINYINT NOT NULL COMMENT '1 Monday to 7 Sunday',
    `period_id` BIGINT NOT NULL COMMENT 'Period id',
    `allow_student` TINYINT NOT NULL DEFAULT 1 COMMENT 'Student can reserve',
    `allow_teacher` TINYINT NOT NULL DEFAULT 1 COMMENT 'Teacher can reserve',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lab_open_slot_lab_weekday_period` (`lab_id`, `weekday`, `period_id`),
    KEY `idx_lab_open_slot_lab_weekday` (`lab_id`, `weekday`),
    KEY `idx_lab_open_slot_period_id` (`period_id`),
    CONSTRAINT `fk_lab_open_slot_lab`
        FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`),
    CONSTRAINT `fk_lab_open_slot_period`
        FOREIGN KEY (`period_id`) REFERENCES `class_period` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Lab open slot table';

CREATE TABLE `lab_device` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `lab_id` BIGINT NOT NULL COMMENT 'Lab id',
    `device_name` VARCHAR(100) NOT NULL COMMENT 'Device name',
    `device_code` VARCHAR(50) NOT NULL COMMENT 'Device code',
    `brand` VARCHAR(100) NOT NULL COMMENT 'Brand',
    `model_no` VARCHAR(100) NOT NULL COMMENT 'Model',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT 'Quantity',
    `available_quantity` INT NOT NULL DEFAULT 1 COMMENT 'Available quantity',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 normal, 2 maintenance, 3 disabled',
    `purchase_date` DATE NOT NULL COMMENT 'Purchase date',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 deleted',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lab_device_device_code_deleted` (`device_code`, `deleted`),
    UNIQUE KEY `uk_lab_device_lab_name_deleted` (`lab_id`, `device_name`, `model_no`, `deleted`),
    KEY `idx_lab_device_lab_id` (`lab_id`),
    CONSTRAINT `fk_lab_device_lab`
        FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Lab device table';

CREATE TABLE `lab_consumable` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `lab_id` BIGINT NOT NULL COMMENT 'Lab id',
    `consumable_name` VARCHAR(100) NOT NULL COMMENT 'Consumable name',
    `consumable_code` VARCHAR(50) NOT NULL COMMENT 'Consumable code',
    `unit` VARCHAR(20) NOT NULL COMMENT 'Unit',
    `stock_quantity` INT NOT NULL DEFAULT 0 COMMENT 'Stock quantity',
    `warning_threshold` INT NOT NULL DEFAULT 0 COMMENT 'Warning threshold',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 deleted',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lab_consumable_code_deleted` (`consumable_code`, `deleted`),
    UNIQUE KEY `uk_lab_consumable_lab_name_deleted` (`lab_id`, `consumable_name`, `deleted`),
    KEY `idx_lab_consumable_lab_id` (`lab_id`),
    CONSTRAINT `fk_lab_consumable_lab`
        FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Lab consumable table';

CREATE TABLE `consumable_stock_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `consumable_id` BIGINT NOT NULL COMMENT 'Consumable id',
    `change_type` VARCHAR(20) NOT NULL COMMENT 'IN, OUT, ADJUST',
    `change_amount` INT NOT NULL COMMENT 'Changed amount',
    `before_stock` INT NOT NULL COMMENT 'Stock before change',
    `after_stock` INT NOT NULL COMMENT 'Stock after change',
    `operator_user_id` BIGINT DEFAULT NULL COMMENT 'Operator user id',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (`id`),
    KEY `idx_consumable_stock_log_consumable_id` (`consumable_id`),
    KEY `idx_consumable_stock_log_operator_user_id` (`operator_user_id`),
    KEY `idx_consumable_stock_log_created_at` (`created_at`),
    CONSTRAINT `fk_consumable_stock_log_consumable`
        FOREIGN KEY (`consumable_id`) REFERENCES `lab_consumable` (`id`),
    CONSTRAINT `fk_consumable_stock_log_operator_user`
        FOREIGN KEY (`operator_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Consumable stock log table';

CREATE TABLE `lab_reservation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `reservation_no` VARCHAR(50) NOT NULL COMMENT 'Reservation number',
    `lab_id` BIGINT NOT NULL COMMENT 'Lab id',
    `applicant_user_id` BIGINT NOT NULL COMMENT 'Applicant user id',
    `approver_user_id` BIGINT DEFAULT NULL COMMENT 'Approver user id',
    `reservation_type` TINYINT NOT NULL DEFAULT 3 COMMENT '1 course, 2 research, 3 personal',
    `priority_level` TINYINT NOT NULL DEFAULT 3 COMMENT '1 high, 2 medium, 3 low',
    `usage_purpose` VARCHAR(255) NOT NULL COMMENT 'Usage purpose',
    `course_or_project_name` VARCHAR(100) NOT NULL COMMENT 'Course or project name',
    `participant_count` INT NOT NULL DEFAULT 1 COMMENT 'Participant count',
    `contact_phone` VARCHAR(20) NOT NULL COMMENT 'Contact phone',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 pending, 2 approved, 3 rejected, 4 canceled, 5 completed',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT 'Reject reason',
    `check_in_time` DATETIME DEFAULT NULL COMMENT 'Check in time',
    `check_out_time` DATETIME DEFAULT NULL COMMENT 'Check out time',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lab_reservation_reservation_no` (`reservation_no`),
    KEY `idx_lab_reservation_lab_id` (`lab_id`),
    KEY `idx_lab_reservation_applicant_user_id` (`applicant_user_id`),
    KEY `idx_lab_reservation_approver_user_id` (`approver_user_id`),
    KEY `idx_lab_reservation_status_created_at` (`status`, `created_at`),
    CONSTRAINT `fk_lab_reservation_lab`
        FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`),
    CONSTRAINT `fk_lab_reservation_applicant_user`
        FOREIGN KEY (`applicant_user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_lab_reservation_approver_user`
        FOREIGN KEY (`approver_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Lab reservation table';

CREATE TABLE `lab_reservation_slot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `reservation_id` BIGINT NOT NULL COMMENT 'Reservation id',
    `lab_id` BIGINT NOT NULL COMMENT 'Lab id',
    `reservation_date` DATE NOT NULL COMMENT 'Reservation date',
    `weekday` TINYINT NOT NULL COMMENT '1 Monday to 7 Sunday',
    `period_id` BIGINT NOT NULL COMMENT 'Period id',
    `slot_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 occupied, 2 canceled',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    KEY `idx_lab_reservation_slot_lab_date_period_status` (`lab_id`, `reservation_date`, `period_id`, `slot_status`),
    KEY `idx_lab_reservation_slot_reservation_id` (`reservation_id`),
    KEY `idx_lab_reservation_slot_lab_date` (`lab_id`, `reservation_date`),
    KEY `idx_lab_reservation_slot_period_id` (`period_id`),
    CONSTRAINT `fk_lab_reservation_slot_reservation`
        FOREIGN KEY (`reservation_id`) REFERENCES `lab_reservation` (`id`),
    CONSTRAINT `fk_lab_reservation_slot_lab`
        FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`),
    CONSTRAINT `fk_lab_reservation_slot_period`
        FOREIGN KEY (`period_id`) REFERENCES `class_period` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Lab reservation slot table';

CREATE TABLE `lab_maintenance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `lab_id` BIGINT NOT NULL COMMENT 'Lab id',
    `maintenance_date` DATE NOT NULL COMMENT 'Maintenance date',
    `period_id` BIGINT NOT NULL COMMENT 'Period id',
    `reason` VARCHAR(255) NOT NULL COMMENT 'Reason',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 active, 2 canceled',
    `operator_user_id` BIGINT NOT NULL COMMENT 'Operator user id',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lab_maintenance_lab_date_period` (`lab_id`, `maintenance_date`, `period_id`),
    KEY `idx_lab_maintenance_lab_date_status` (`lab_id`, `maintenance_date`, `status`),
    KEY `idx_lab_maintenance_period_id` (`period_id`),
    KEY `idx_lab_maintenance_operator_user_id` (`operator_user_id`),
    CONSTRAINT `fk_lab_maintenance_lab`
        FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`),
    CONSTRAINT `fk_lab_maintenance_period`
        FOREIGN KEY (`period_id`) REFERENCES `class_period` (`id`),
    CONSTRAINT `fk_lab_maintenance_operator_user`
        FOREIGN KEY (`operator_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Lab maintenance table';

CREATE TABLE `reservation_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `reservation_id` BIGINT NOT NULL COMMENT 'Reservation id',
    `audit_user_id` BIGINT NOT NULL COMMENT 'Audit user id',
    `audit_action` TINYINT NOT NULL COMMENT '1 submit, 2 approve, 3 reject, 4 cancel, 5 check in, 6 check out',
    `audit_comment` VARCHAR(255) DEFAULT NULL COMMENT 'Audit comment',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (`id`),
    KEY `idx_reservation_audit_log_reservation_id` (`reservation_id`),
    KEY `idx_reservation_audit_log_audit_user_id` (`audit_user_id`),
    CONSTRAINT `fk_reservation_audit_log_reservation`
        FOREIGN KEY (`reservation_id`) REFERENCES `lab_reservation` (`id`),
    CONSTRAINT `fk_reservation_audit_log_audit_user`
        FOREIGN KEY (`audit_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reservation audit log table';

CREATE TABLE `user_violation_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User id',
    `reservation_id` BIGINT DEFAULT NULL COMMENT 'Reservation id',
    `violation_type` TINYINT NOT NULL COMMENT '1 no show, 2 late, 3 misuse, 4 other',
    `score_change` INT NOT NULL DEFAULT 0 COMMENT 'Score change',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (`id`),
    KEY `idx_user_violation_record_user_id` (`user_id`),
    KEY `idx_user_violation_record_reservation_id` (`reservation_id`),
    CONSTRAINT `fk_user_violation_record_user`
        FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_user_violation_record_reservation`
        FOREIGN KEY (`reservation_id`) REFERENCES `lab_reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User violation record table';

SET FOREIGN_KEY_CHECKS = 1;
