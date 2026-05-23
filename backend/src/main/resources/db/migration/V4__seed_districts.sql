-- ============================================================
-- V4: Seed Warsaw districts (18 official dzielnice)
-- ============================================================

INSERT INTO districts (name, slug) VALUES
    ('Bemowo',          'bemowo'),
    ('Białołęka',       'bialoleka'),
    ('Bielany',         'bielany'),
    ('Mokotów',         'mokotow'),
    ('Ochota',          'ochota'),
    ('Praga-Południe',  'praga-poludnie'),
    ('Praga-Północ',    'praga-polnoc'),
    ('Rembertów',       'rembertow'),
    ('Śródmieście',     'srodmiescie'),
    ('Targówek',        'targowek'),
    ('Ursus',           'ursus'),
    ('Ursynów',         'ursynow'),
    ('Wawer',           'wawer'),
    ('Wesoła',          'wesola'),
    ('Wilanów',         'wilanow'),
    ('Włochy',          'wlochy'),
    ('Wola',            'wola'),
    ('Żoliborz',        'zoliborz')
ON CONFLICT (slug) DO NOTHING;

-- Standart hizmet kategorileri
INSERT INTO services (name, category) VALUES
    ('Haircut',          'hair'),
    ('Hair Coloring',    'hair'),
    ('Hair Styling',     'hair'),
    ('Manicure',         'nails'),
    ('Pedicure',         'nails'),
    ('Nail Art',         'nails'),
    ('Facial',           'face'),
    ('Makeup',           'face'),
    ('Eyebrow Shaping',  'face'),
    ('Massage',          'body'),
    ('Waxing',           'body')
ON CONFLICT (name) DO NOTHING;
