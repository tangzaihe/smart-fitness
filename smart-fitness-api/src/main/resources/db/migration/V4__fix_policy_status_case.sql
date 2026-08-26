-- V3 seeded status as lowercase 'active'; runtime queries ACTIVE.

UPDATE coach_policy_version
SET status = 'ACTIVE'
WHERE status = 'active';
