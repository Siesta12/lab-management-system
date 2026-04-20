# Reservation Model Refactor (Time Range -> Class Period Schedule)

## Database

- Removed (deprecated) time-range model:
  - Dropped table `lab_open_rule`.
  - Refactored table `lab_reservation`: removed `reservation_date`, `start_time`, `end_time`.
- Added schedule/period model tables:
  - `class_period`: fixed class periods (5 default rows inserted in `schema.sql`).
  - `lab_open_slot`: weekly open configuration by `weekday + period_id`.
  - `lab_reservation_slot`: reservation occupancy by `lab_id + reservation_date + period_id` (unique).
  - `lab_maintenance`: maintenance blocks by `lab_id + maintenance_date + period_id` (unique; cancel via status update).

## Backend API Changes

- Added:
  - `GET /labs/{labId}/schedule` (future 21 days schedule; statuses: `FREE/RESERVED/PENDING/MAINTENANCE/CLOSED`)
  - `GET /labs/schedule/daily` (admin daily overview)
  - `POST /labs/{labId}/maintenance` (admin set maintenance; supports multiple periods)
  - `PUT /labs/{labId}/maintenance/{maintenanceId}/cancel`
  - `GET /labs/{labId}/maintenance`
- Reservations (updated to multi-slot):
  - `POST /reservations` payload now uses `slots: [{ reservationDate, periodId }]` (no `startTime/endTime`).
  - `GET /reservations/my` replaces old `/reservations/mine`.
  - `PUT /reservations/{id}/approve` and `PUT /reservations/{id}/reject` replace old `/reservations/{id}/audit`.
  - `PUT /reservations/{id}/cancel` replaces old POST cancel.
  - Extra helper: `POST /reservations/recommendations` returns slot/lab recommendations (used by the new schedule UI).
- Removed legacy endpoints:
  - `/lab-open-rules` (controller/service/mapper removed; replaced by schedule-based model).

## Frontend Changes

- Labs detail page now contains a “future 3-week schedule” grid:
  - Click `FREE` cells to select multiple periods.
  - Submit reservation with a single request containing multiple slots.
  - Admin can switch to “maintenance mode”, select slots and set maintenance; click maintenance cells to cancel.
- Added admin page: `/daily-schedule` (daily overview of all labs/periods).
- Removed legacy pages that depended on time-range rules:
  - Reservation apply page and open-rule page (routes removed).

## Time Window Rule (Important)

- All schedule/reservation/maintenance operations are limited to:
  - `today` to `today + 20` (inclusive), i.e. 21 days.
- The current implementation enforces `GET /labs/{labId}/schedule?startDate=...` to only accept `startDate=today`.

## Test Data

- `backend/src/main/resources/db/test_data.sql` was rewritten for the new model.
- Dataset dates are aligned around `2026-04-20` and cover the next 3 weeks with:
  - Pending / approved / rejected / canceled / completed reservations.
  - Active and canceled maintenance records.
  - Audit logs and violation records.

