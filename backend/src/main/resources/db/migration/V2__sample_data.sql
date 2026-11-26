INSERT INTO hotels (id, name, location, description, price_per_night, total_rooms) VALUES
(1, 'Sea Breeze Resort',        'Goa',       'Beachfront resort a short walk from Calangute with a pool, shacks and sunset views.',            6500.00, 6),
(2, 'Pink City Haveli',         'Jaipur',    'Restored 19th-century haveli with courtyard dining, rooftop views of Amer Fort and folk evenings.', 4800.00, 4),
(3, 'Marine Drive Grand',       'Mumbai',    'Business-friendly hotel on the Queen''s Necklace with sea-facing rooms and a rooftop bar.',          9200.00, 8),
(4, 'Backwater Bliss',          'Kochi',     'Quiet lakeside stay in Fort Kochi, with Ayurvedic spa and houseboat excursions.',                 5400.00, 3),
(5, 'Lakeview Palace Retreat',  'Udaipur',   'Heritage property on Lake Pichola; every room faces the water and the City Palace.',             12500.00, 2),
(6, 'Pine Ridge Lodge',         'Manali',    'Cosy wooden lodge with fireplaces, mountain views and easy access to Solang Valley.',             3900.00, 5),
(7, 'Candolim Palm Suites',     'Goa',       'Budget-friendly suites near Candolim beach with a pool and free breakfast.',                      3200.00, 5);

INSERT INTO hotel_amenities (hotel_id, amenity) VALUES
(1, 'Pool'), (1, 'Beach access'), (1, 'Free Wi-Fi'), (1, 'Breakfast included'),
(2, 'Courtyard dining'), (2, 'Free Wi-Fi'), (2, 'Cultural evenings'),
(3, 'Free Wi-Fi'), (3, 'Gym'), (3, 'Rooftop bar'), (3, 'Airport shuttle'),
(4, 'Ayurvedic spa'), (4, 'Free Wi-Fi'), (4, 'Boat tours'),
(5, 'Lake view'), (5, 'Pool'), (5, 'Spa'), (5, 'Fine dining'),
(6, 'Fireplace'), (6, 'Mountain view'), (6, 'Free parking'),
(7, 'Pool'), (7, 'Free Wi-Fi'), (7, 'Breakfast included');

ALTER TABLE hotels AUTO_INCREMENT = 100;
