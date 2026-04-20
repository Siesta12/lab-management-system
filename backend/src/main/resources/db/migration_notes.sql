-- Reservation conflict handling migration
-- Why: multiple pending applications for the same lab/date/period must be allowed,
-- while approved/conflict handling is now enforced in service logic.

ALTER TABLE lab_reservation_slot
  DROP INDEX uk_lab_reservation_slot_lab_date_period_active;

ALTER TABLE lab_reservation_slot
  DROP COLUMN active_flag;

ALTER TABLE lab_reservation_slot
  ADD INDEX idx_lab_reservation_slot_lab_date_period_status (lab_id, reservation_date, period_id, slot_status);
