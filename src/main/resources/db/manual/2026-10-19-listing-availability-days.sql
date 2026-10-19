-- Keep availability in its existing single column; no new table is needed.
ALTER TABLE listings DROP CHECK chk_listing_availability;
ALTER TABLE listings MODIFY COLUMN availability VARCHAR(100) NOT NULL;

-- Preserve the meaning of the existing grouped availability values.
UPDATE listings
SET availability = CASE availability
    WHEN 'WEEKDAYS' THEN 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY'
    WHEN 'WEEKENDS' THEN 'SATURDAY,SUNDAY'
    WHEN 'BOTH' THEN 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY'
    ELSE availability
END
WHERE availability IN ('WEEKDAYS', 'WEEKENDS', 'BOTH');

-- Replace the old three-value constraint with valid comma-separated day names.
ALTER TABLE listings ADD CONSTRAINT chk_listing_availability CHECK (
    REGEXP_LIKE(availability,
      '^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)(,(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY))*$', 'c')
);
