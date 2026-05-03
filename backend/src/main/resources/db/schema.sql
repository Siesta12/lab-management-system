CREATE DATABASE IF NOT EXISTS `lab_management_system`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `lab_management_system`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `consumable_stock_log`;
DROP TABLE IF EXISTS `lab_experiment_report_consumable`;
DROP TABLE IF EXISTS `lab_experiment_report`;
DROP TABLE IF EXISTS `user_violation_record`;
DROP TABLE IF EXISTS `reservation_audit_log`;
DROP TABLE IF EXISTS `lab_maintenance`;
DROP TABLE IF EXISTS `lab_reservation_slot`;
DROP TABLE IF EXISTS `lab_reservation`;
DROP TABLE IF EXISTS `lab_open_slot`;
DROP TABLE IF EXISTS `class_period`;
DROP TABLE IF EXISTS `lab_consumable`;
DROP TABLE IF EXISTS `lab_device`;
DROP TABLE IF EXISTS `lab_device_repair`;
DROP TABLE IF EXISTS `lab`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `department`;

create table class_period
(
  id          bigint auto_increment comment 'Primary key'
        primary key,
  period_no   int                                not null comment 'Unique period number',
  period_name varchar(50)                        not null comment 'Period name',
  start_time  time                               not null comment 'Start time',
  end_time    time                               not null comment 'End time',
  sort_order  int      default 0                 not null comment 'Sort order',
  status      tinyint  default 1                 not null comment '1 enabled, 0 disabled',
  created_at  datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_class_period_period_no
    unique (period_no)
) comment 'Class period table';

create index idx_class_period_status_sort
  on class_period (status, sort_order);

create table department
(
  id              bigint auto_increment comment 'Primary key'
        primary key,
  department_name varchar(100)                       not null comment 'Department name',
  department_code varchar(50)                        not null comment 'Department code',
  leader_name     varchar(50)                        not null comment 'Leader name',
  phone           varchar(20)                        not null comment 'Phone',
  status          tinyint  default 1                 not null comment '1 enabled, 0 disabled',
  deleted         tinyint  default 0                 not null comment '0 active, 1 deleted',
  created_at      datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_department_code_deleted
    unique (department_code, deleted),
  constraint uk_department_name_deleted
    unique (department_name, deleted)
) comment 'Department table';

create table sys_role
(
  id          bigint auto_increment comment 'Primary key'
        primary key,
  role_name   varchar(50)                        not null comment 'Role name',
  role_code   varchar(50)                        not null comment 'Role code',
  description varchar(255) null comment 'Description',
  status      tinyint  default 1                 not null comment '1 enabled, 0 disabled',
  deleted     tinyint  default 0                 not null comment '0 active, 1 deleted',
  created_at  datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_sys_role_role_code_deleted
    unique (role_code, deleted),
  constraint uk_sys_role_role_name_deleted
    unique (role_name, deleted)
) comment 'System role table';

create table sys_user
(
  id              bigint auto_increment comment 'Primary key'
        primary key,
  department_id   bigint null comment 'Department id',
  user_no         varchar(50)                        not null comment 'Student or job number',
  password        varchar(255)                       not null comment 'Login password',
  real_name       varchar(50)                        not null comment 'Real name',
  gender          tinyint  default 0                 not null comment '0 unknown, 1 male, 2 female',
  phone           varchar(20)                        not null comment 'Phone',
  email           varchar(100)                       not null comment 'Email',
  credit_score    int      default 100               not null comment 'Credit score',
  violation_count int      default 0                 not null comment 'Violation count',
  normal_reservation_streak int default 0            not null comment 'Consecutive normal reservations',
  status          tinyint  default 1                 not null comment '1 enabled, 0 disabled',
  deleted         tinyint  default 0                 not null comment '0 active, 1 deleted',
  last_login_at   datetime null comment 'Last login time',
  created_at      datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_sys_user_email_deleted
    unique (email, deleted),
  constraint uk_sys_user_phone_deleted
    unique (phone, deleted),
  constraint uk_sys_user_user_no_deleted
    unique (user_no, deleted),
  constraint fk_sys_user_department
    foreign key (department_id) references department (id)
) comment 'System user table';

create table lab
(
  id              bigint auto_increment comment 'Primary key'
        primary key,
  department_id   bigint null comment 'Department id',
  lab_code        varchar(50)                        not null comment 'Lab code',
  lab_name        varchar(100)                       not null comment 'Lab name',
  lab_type        varchar(50)                        not null comment 'Lab type',
  building_name   varchar(100)                       not null comment 'Building name',
  room_no         varchar(50)                        not null comment 'Room number',
  capacity        int      default 0                 not null comment 'Capacity',
  manager_user_id bigint null comment 'Manager user id',
  open_status     tinyint  default 1                 not null comment '1 open, 0 closed',
  lab_status      tinyint  default 1                 not null comment '1 normal, 2 maintenance, 0 disabled',
  description     varchar(500) null comment 'Description',
  usage_rule      varchar(500) null comment 'Usage rule',
  latitude double null comment '实验室纬度',
  longitude double null comment '实验室经度',
  deleted         tinyint  default 0                 not null comment '0 active, 1 deleted',
  created_at      datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_lab_lab_code_deleted
    unique (lab_code, deleted),
  constraint uk_lab_name_room_deleted
    unique (lab_name, building_name, room_no, deleted),
  constraint fk_lab_department
    foreign key (department_id) references department (id),
  constraint fk_lab_manager_user
    foreign key (manager_user_id) references sys_user (id)
) comment 'Lab table';

create index idx_lab_department_id
  on lab (department_id);

create index idx_lab_manager_user_id
  on lab (manager_user_id);

create index idx_lab_open_status_deleted
  on lab (open_status, deleted);

create table lab_consumable
(
  id                bigint auto_increment comment 'Primary key'
        primary key,
  lab_id            bigint                             not null comment 'Lab id',
  consumable_name   varchar(100)                       not null comment 'Consumable name',
  consumable_code   varchar(50)                        not null comment 'Consumable code',
  specification     varchar(100) null comment 'Specification',
  unit              varchar(20)                        not null comment 'Unit',
  stock_quantity    int      default 0                 not null comment 'Stock quantity',
  warning_threshold int      default 0                 not null comment 'Warning threshold',
  status            tinyint  default 1                 not null comment '1 enabled, 0 disabled',
  remark            varchar(255) null comment 'Remark',
  deleted           tinyint  default 0                 not null comment '0 active, 1 deleted',
  created_at        datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_lab_consumable_code_deleted
    unique (consumable_code, deleted),
  constraint uk_lab_consumable_lab_name_deleted
    unique (lab_id, consumable_name, deleted),
  constraint fk_lab_consumable_lab
    foreign key (lab_id) references lab (id)
) comment 'Lab consumable table';

create table consumable_stock_log
(
  id               bigint auto_increment comment 'Primary key'
        primary key,
  consumable_id    bigint                             not null comment 'Consumable id',
  change_type      varchar(20)                        not null comment 'IN, OUT, ADJUST',
  change_amount    int                                not null comment 'Changed amount',
  before_stock     int                                not null comment 'Stock before change',
  after_stock      int                                not null comment 'Stock after change',
  operator_user_id bigint null comment 'Operator user id',
  source_type      varchar(30) null comment 'Source type',
  source_id        bigint null comment 'Source id',
  remark           varchar(255) null comment 'Remark',
  created_at       datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  constraint fk_consumable_stock_log_consumable
    foreign key (consumable_id) references lab_consumable (id),
  constraint fk_consumable_stock_log_operator_user
    foreign key (operator_user_id) references sys_user (id)
) comment 'Consumable stock log table';

create index idx_consumable_stock_log_consumable_id
  on consumable_stock_log (consumable_id);

create index idx_consumable_stock_log_created_at
  on consumable_stock_log (created_at);

create index idx_consumable_stock_log_operator_user_id
  on consumable_stock_log (operator_user_id);

create index idx_consumable_stock_log_source
  on consumable_stock_log (source_type, source_id);

create index idx_lab_consumable_lab_id
  on lab_consumable (lab_id);

create table lab_device
(
  id                 bigint auto_increment comment 'Primary key'
        primary key,
  lab_id             bigint                             not null comment 'Lab id',
  device_name        varchar(100)                       not null comment 'Device name',
  device_code        varchar(50)                        not null comment 'Device code',
  brand              varchar(100)                       not null comment 'Brand',
  model_no           varchar(100)                       not null comment 'Model',
  quantity           int      default 1                 not null comment 'Quantity',
  available_quantity int      default 1                 not null comment 'Available quantity',
  status             tinyint  default 1                 not null comment '1 normal, 2 maintenance, 3 disabled',
  purchase_date      date                               not null comment 'Purchase date',
  remark             varchar(255) null comment 'Remark',
  deleted            tinyint  default 0                 not null comment '0 active, 1 deleted',
  created_at         datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at         datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_lab_device_device_code_deleted
    unique (device_code, deleted),
  constraint uk_lab_device_lab_name_deleted
    unique (lab_id, device_name, model_no, deleted),
  constraint fk_lab_device_lab
    foreign key (lab_id) references lab (id)
) comment 'Lab device table';

  create index idx_lab_device_lab_id
    on lab_device (lab_id);

create table lab_device_repair
(
  id                 bigint auto_increment comment 'Primary key'
        primary key,
  device_id          bigint                             not null comment 'Device id',
  lab_id             bigint                             not null comment 'Lab id',
  applicant_user_id   bigint                             not null comment 'Applicant user id',
  issue_description  varchar(500)                       not null comment 'Issue description',
  urgency_level      tinyint  default 2                 not null comment '1 low, 2 medium, 3 high',
  status             tinyint  default 1                 not null comment '1 pending, 2 processing, 3 completed, 4 rejected',
  handler_user_id    bigint null comment 'Handler user id',
  handling_result    varchar(500) null comment 'Handling result',
  handled_at         datetime null comment 'Handled time',
  created_at         datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at         datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint fk_lab_device_repair_device
    foreign key (device_id) references lab_device (id),
  constraint fk_lab_device_repair_lab
    foreign key (lab_id) references lab (id),
  constraint fk_lab_device_repair_applicant_user
    foreign key (applicant_user_id) references sys_user (id),
  constraint fk_lab_device_repair_handler_user
    foreign key (handler_user_id) references sys_user (id)
) comment 'Lab device repair table';

create index idx_lab_device_repair_lab_id_status
  on lab_device_repair (lab_id, status);

create index idx_lab_device_repair_device_id
  on lab_device_repair (device_id);

create index idx_lab_device_repair_applicant_user_id
  on lab_device_repair (applicant_user_id);

create table lab_maintenance
(
  id               bigint auto_increment comment 'Primary key'
        primary key,
  lab_id           bigint                             not null comment 'Lab id',
  maintenance_date date                               not null comment 'Maintenance date',
  period_id        bigint                             not null comment 'Period id',
  reason           varchar(255)                       not null comment 'Reason',
  status           tinyint  default 1                 not null comment '1 active, 2 canceled',
  operator_user_id bigint                             not null comment 'Operator user id',
  created_at       datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_lab_maintenance_lab_date_period
    unique (lab_id, maintenance_date, period_id),
  constraint fk_lab_maintenance_lab
    foreign key (lab_id) references lab (id),
  constraint fk_lab_maintenance_operator_user
    foreign key (operator_user_id) references sys_user (id),
  constraint fk_lab_maintenance_period
    foreign key (period_id) references class_period (id)
) comment 'Lab maintenance table';

create index idx_lab_maintenance_lab_date_status
  on lab_maintenance (lab_id, maintenance_date, status);

create index idx_lab_maintenance_operator_user_id
  on lab_maintenance (operator_user_id);

create index idx_lab_maintenance_period_id
  on lab_maintenance (period_id);

create table lab_open_slot
(
  id            bigint auto_increment comment 'Primary key'
        primary key,
  lab_id        bigint                             not null comment 'Lab id',
  weekday       tinyint                            not null comment '1 Monday to 7 Sunday',
  period_id     bigint                             not null comment 'Period id',
  allow_student tinyint  default 1                 not null comment 'Student can reserve',
  allow_teacher tinyint  default 1                 not null comment 'Teacher can reserve',
  status        tinyint  default 1                 not null comment '1 enabled, 0 disabled',
  created_at    datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_lab_open_slot_lab_weekday_period
    unique (lab_id, weekday, period_id),
  constraint fk_lab_open_slot_lab
    foreign key (lab_id) references lab (id),
  constraint fk_lab_open_slot_period
    foreign key (period_id) references class_period (id)
) comment 'Lab open slot table';

create index idx_lab_open_slot_lab_weekday
  on lab_open_slot (lab_id, weekday);

create index idx_lab_open_slot_period_id
  on lab_open_slot (period_id);

create table lab_reservation
(
  id                     bigint auto_increment comment 'Primary key'
        primary key,
  reservation_no         varchar(50)                        not null comment 'Reservation number',
  lab_id                 bigint                             not null comment 'Lab id',
  applicant_user_id      bigint                             not null comment 'Applicant user id',
  approver_user_id       bigint null comment 'Approver user id',
  reservation_type       tinyint  default 3                 not null comment '1 course, 2 research, 3 personal',
  priority_level         tinyint  default 3                 not null comment '1 high, 2 medium, 3 low',
  usage_purpose          varchar(255)                       not null comment 'Usage purpose',
  course_or_project_name varchar(100)                       not null comment 'Course or project name',
  participant_count      int      default 1                 not null comment 'Participant count',
  contact_phone          varchar(20)                        not null comment 'Contact phone',
  status                 tinyint  default 1                 not null comment '1 pending, 2 approved, 3 rejected, 4 canceled, 5 completed',
  reject_reason          varchar(255) null comment 'Reject reason',
  check_in_time          datetime null comment 'Check in time',
  check_out_time         datetime null comment 'Check out time',
  created_at             datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at             datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_lab_reservation_reservation_no
    unique (reservation_no),
  constraint fk_lab_reservation_applicant_user
    foreign key (applicant_user_id) references sys_user (id),
  constraint fk_lab_reservation_approver_user
    foreign key (approver_user_id) references sys_user (id),
  constraint fk_lab_reservation_lab
    foreign key (lab_id) references lab (id)
) comment 'Lab reservation table';

create index idx_lab_reservation_applicant_user_id
  on lab_reservation (applicant_user_id);

create index idx_lab_reservation_approver_user_id
  on lab_reservation (approver_user_id);

create index idx_lab_reservation_lab_id
  on lab_reservation (lab_id);

create index idx_lab_reservation_status_created_at
  on lab_reservation (status, created_at);

create table lab_reservation_slot
(
  id               bigint auto_increment comment 'Primary key'
        primary key,
  reservation_id   bigint                             not null comment 'Reservation id',
  lab_id           bigint                             not null comment 'Lab id',
  reservation_date date                               not null comment 'Reservation date',
  weekday          tinyint                            not null comment '1 Monday to 7 Sunday',
  period_id        bigint                             not null comment 'Period id',
  slot_status      tinyint  default 1                 not null comment '1 occupied, 2 canceled',
  created_at       datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint fk_lab_reservation_slot_lab
    foreign key (lab_id) references lab (id),
  constraint fk_lab_reservation_slot_period
    foreign key (period_id) references class_period (id),
  constraint fk_lab_reservation_slot_reservation
    foreign key (reservation_id) references lab_reservation (id)
) comment 'Lab reservation slot table';

create index idx_lab_reservation_slot_lab_date
  on lab_reservation_slot (lab_id, reservation_date);

create index idx_lab_reservation_slot_lab_date_period_status
  on lab_reservation_slot (lab_id, reservation_date, period_id, slot_status);

create index idx_lab_reservation_slot_period_id
  on lab_reservation_slot (period_id);

create index idx_lab_reservation_slot_reservation_id
  on lab_reservation_slot (reservation_id);

create table reservation_audit_log
(
  id             bigint auto_increment comment 'Primary key'
        primary key,
  reservation_id bigint                             not null comment 'Reservation id',
  audit_user_id  bigint                             not null comment 'Audit user id',
  audit_action   tinyint                            not null comment '1 submit, 2 approve, 3 reject, 4 cancel, 5 check in, 6 check out',
  audit_comment  varchar(255) null comment 'Audit comment',
  created_at     datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  constraint fk_reservation_audit_log_audit_user
    foreign key (audit_user_id) references sys_user (id),
  constraint fk_reservation_audit_log_reservation
    foreign key (reservation_id) references lab_reservation (id)
) comment 'Reservation audit log table';

create index idx_reservation_audit_log_audit_user_id
  on reservation_audit_log (audit_user_id);

create index idx_reservation_audit_log_reservation_id
  on reservation_audit_log (reservation_id);

create index idx_sys_user_department_id
  on sys_user (department_id);

create index idx_sys_user_status_deleted
  on sys_user (status, deleted);

create table sys_user_role
(
  id         bigint auto_increment comment 'Primary key'
        primary key,
  user_id    bigint                             not null comment 'User id',
  role_id    bigint                             not null comment 'Role id',
  created_at datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  constraint uk_sys_user_role_user_id_role_id
    unique (user_id, role_id),
  constraint fk_sys_user_role_role
    foreign key (role_id) references sys_role (id),
  constraint fk_sys_user_role_user
    foreign key (user_id) references sys_user (id)
) comment 'User role mapping table';

create index idx_sys_user_role_role_id
  on sys_user_role (role_id);

create table user_violation_record
(
  id             bigint auto_increment comment 'Primary key'
        primary key,
  user_id        bigint                             not null comment 'User id',
  reservation_id bigint null comment 'Reservation id',
  violation_type tinyint                            not null comment '1 no show, 2 late, 3 misuse, 4 other',
  score_change   int      default 0                 not null comment 'Score change',
  remark         varchar(255) null comment 'Remark',
  created_at     datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  constraint fk_user_violation_record_reservation
    foreign key (reservation_id) references lab_reservation (id),
  constraint fk_user_violation_record_user
    foreign key (user_id) references sys_user (id)
) comment 'User violation record table';

create index idx_user_violation_record_reservation_id
  on user_violation_record (reservation_id);

create index idx_user_violation_record_user_id
  on user_violation_record (user_id);

create table lab_experiment_report
(
  id              bigint auto_increment comment 'Primary key'
        primary key,
  report_no       varchar(50)                        not null comment 'Report number',
  student_id      bigint                             not null comment 'Student user id',
  teacher_id      bigint                             not null comment 'Teacher user id',
  department_id   bigint                             not null comment 'Department id',
  lab_id          bigint                             not null comment 'Lab id',
  reservation_id  bigint null comment 'Reservation id',
  experiment_name varchar(100)                       not null comment 'Experiment name',
  experiment_date date                               not null comment 'Experiment date',
  purpose         text null comment 'Purpose',
  principle       text null comment 'Principle',
  steps           text null comment 'Steps',
  result_data     text null comment 'Result data',
  analysis        text null comment 'Analysis',
  conclusion      text null comment 'Conclusion',
  status          tinyint  default 1                 not null comment '1 draft, 2 pending, 3 approved, 4 returned',
  teacher_comment varchar(500) null comment 'Teacher comment',
  submitted_at    datetime null comment 'Submitted time',
  reviewed_at     datetime null comment 'Reviewed time',
  deleted         tinyint  default 0                 not null comment '0 active, 1 deleted',
  created_at      datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  updated_at      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated time',
  constraint uk_lab_experiment_report_no
    unique (report_no),
  constraint fk_lab_experiment_report_student
    foreign key (student_id) references sys_user (id),
  constraint fk_lab_experiment_report_teacher
    foreign key (teacher_id) references sys_user (id),
  constraint fk_lab_experiment_report_department
    foreign key (department_id) references department (id),
  constraint fk_lab_experiment_report_lab
    foreign key (lab_id) references lab (id),
  constraint fk_lab_experiment_report_reservation
    foreign key (reservation_id) references lab_reservation (id)
) comment 'Experiment report table';

create index idx_lab_experiment_report_student_status
  on lab_experiment_report (student_id, status);

create index idx_lab_experiment_report_teacher_status
  on lab_experiment_report (teacher_id, status);

create index idx_lab_experiment_report_department_status
  on lab_experiment_report (department_id, status);

create table lab_experiment_report_consumable
(
  id              bigint auto_increment comment 'Primary key'
        primary key,
  report_id       bigint                             not null comment 'Report id',
  consumable_id   bigint                             not null comment 'Consumable id',
  lab_id          bigint                             not null comment 'Lab id',
  consumable_name varchar(100)                       not null comment 'Consumable name',
  specification   varchar(100) null comment 'Specification',
  quantity        int      default 0                 not null comment 'Quantity',
  unit            varchar(20) null comment 'Unit',
  status          tinyint  default 1                 not null comment '1 pending, 2 confirmed, 3 rejected',
  confirm_user_id bigint null comment 'Confirm admin user id',
  confirmed_at    datetime null comment 'Confirmed time',
  reject_reason   varchar(255) null comment 'Reject reason',
  stock_log_id    bigint null comment 'Stock log id',
  remark          varchar(255) null comment 'Remark',
  created_at      datetime default CURRENT_TIMESTAMP not null comment 'Created time',
  constraint fk_lab_experiment_report_consumable_report
    foreign key (report_id) references lab_experiment_report (id),
  constraint fk_lab_experiment_report_consumable_consumable
    foreign key (consumable_id) references lab_consumable (id),
  constraint fk_lab_experiment_report_consumable_lab
    foreign key (lab_id) references lab (id),
  constraint fk_lab_experiment_report_consumable_confirm_user
    foreign key (confirm_user_id) references sys_user (id),
  constraint fk_lab_experiment_report_consumable_stock_log
    foreign key (stock_log_id) references consumable_stock_log (id)
) comment 'Experiment report consumable record table';

create index idx_lab_experiment_report_consumable_report_id
  on lab_experiment_report_consumable (report_id);

create index idx_lab_experiment_report_consumable_consumable_id
  on lab_experiment_report_consumable (consumable_id);

create index idx_lab_experiment_report_consumable_lab_status
  on lab_experiment_report_consumable (lab_id, status);

create index idx_lab_experiment_report_consumable_status_created_at
  on lab_experiment_report_consumable (status, created_at);
