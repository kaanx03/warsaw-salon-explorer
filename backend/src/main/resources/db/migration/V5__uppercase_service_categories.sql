ALTER TABLE services DROP CONSTRAINT IF EXISTS services_category_check;
UPDATE services SET category = UPPER(category);
ALTER TABLE services ADD CONSTRAINT services_category_check
    CHECK (category IN ('HAIR', 'NAILS', 'FACE', 'BODY', 'OTHER'));
