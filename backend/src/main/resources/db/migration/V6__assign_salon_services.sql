-- Assign Hair services to hair/barber salons
INSERT INTO salon_services (salon_id, service_id)
SELECT DISTINCT s.id, sv.id
FROM salons s
CROSS JOIN services sv
WHERE sv.category = 'HAIR'
  AND s.is_active = TRUE
  AND (
    s.name ILIKE '%hair%'      OR
    s.name ILIKE '%fryzjer%'   OR
    s.name ILIKE '%barber%'    OR
    s.name ILIKE '%strzyż%'    OR
    s.name ILIKE '%włos%'      OR
    s.name ILIKE '%curls%'     OR
    s.name ILIKE '%haircut%'   OR
    s.name ILIKE '%hairmate%'  OR
    s.name ILIKE '%bloom%'     OR
    s.name ILIKE '%koloryzacja%'
  )
ON CONFLICT DO NOTHING;

-- Assign Nail services to nail salons
INSERT INTO salon_services (salon_id, service_id)
SELECT DISTINCT s.id, sv.id
FROM salons s
CROSS JOIN services sv
WHERE sv.category = 'NAILS'
  AND s.is_active = TRUE
  AND (
    s.name ILIKE '%nail%'         OR
    s.name ILIKE '%manicure%'     OR
    s.name ILIKE '%pedicure%'     OR
    s.name ILIKE '%paznokci%'     OR
    s.name ILIKE '%stylizacja%'   OR
    s.name ILIKE '%hybryd%'       OR
    s.name ILIKE '%beautyłapki%'  OR
    s.name ILIKE '%łapki%'        OR
    s.name ILIKE '%nailart%'
  )
ON CONFLICT DO NOTHING;

-- Assign Face services to beauty/cosmetic salons
INSERT INTO salon_services (salon_id, service_id)
SELECT DISTINCT s.id, sv.id
FROM salons s
CROSS JOIN services sv
WHERE sv.category = 'FACE'
  AND s.is_active = TRUE
  AND (
    s.name ILIKE '%kosmetyczn%'  OR
    s.name ILIKE '%kosmetolog%'  OR
    s.name ILIKE '%makijaż%'     OR
    s.name ILIKE '%makeup%'      OR
    s.name ILIKE '%make-up%'     OR
    s.name ILIKE '%brwi%'        OR
    s.name ILIKE '%rzęs%'        OR
    s.name ILIKE '%lashes%'      OR
    s.name ILIKE '%brows%'       OR
    s.name ILIKE '%facial%'      OR
    s.name ILIKE '%mezoterapia%' OR
    s.name ILIKE '%oczyszczanie%' OR
    s.name ILIKE '%laminacja%'
  )
ON CONFLICT DO NOTHING;

-- Assign Body services to spa/massage salons
INSERT INTO salon_services (salon_id, service_id)
SELECT DISTINCT s.id, sv.id
FROM salons s
CROSS JOIN services sv
WHERE sv.category = 'BODY'
  AND s.is_active = TRUE
  AND (
    s.name ILIKE '%spa%'        OR
    s.name ILIKE '%masaż%'      OR
    s.name ILIKE '%massage%'    OR
    s.name ILIKE '%wellness%'   OR
    s.name ILIKE '%relax%'      OR
    s.name ILIKE '%thai%'       OR
    s.name ILIKE '%hammam%'     OR
    s.name ILIKE '%depilacja%'  OR
    s.name ILIKE '%sauna%'      OR
    s.name ILIKE '%tantric%'    OR
    s.name ILIKE '%erotic%'     OR
    s.name ILIKE '%nuru%'
  )
ON CONFLICT DO NOTHING;

-- General beauty salons with no services yet → Hair + Face as default
INSERT INTO salon_services (salon_id, service_id)
SELECT DISTINCT s.id, sv.id
FROM salons s
CROSS JOIN services sv
WHERE sv.name IN ('Haircut', 'Facial')
  AND s.is_active = TRUE
  AND s.id NOT IN (SELECT DISTINCT salon_id FROM salon_services)
ON CONFLICT DO NOTHING;
