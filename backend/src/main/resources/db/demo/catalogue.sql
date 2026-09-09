-- Deterministic, fictional catalogue. Never replace prices, reservations or existing inventory.
WITH cities AS (
 SELECT city, ordinal FROM unnest(ARRAY['Goa','Delhi','Mumbai','Bengaluru','Chennai','Hyderabad','Jaipur','Udaipur','Kochi','Shimla','Manali','Rishikesh','Agra','Varanasi','Lucknow','Bhopal','Indore','Pune','Kolkata','Ahmedabad','Chandigarh','Amritsar','Jodhpur','Jaisalmer','Mysuru','Ooty','Coorg','Darjeeling','Gangtok','Puducherry']) WITH ORDINALITY AS c(city,ordinal)
)
INSERT INTO properties(demo_key,name,property_type,city,address,description)
SELECT 'hotelvista-demo-v1-'||ordinal||'-'||n,
 (ARRAY['Amber','Cedar','Coral','Juniper','Lotus'])[((n-1)%5)+1]||' '||
 (ARRAY['House','Retreat','Gardens','Residence','Haven'])[((n-1)/5)+1]||' · '||city||' (Demo)',
 (ARRAY['HOTEL','RESORT','APARTMENT','HOMESTAY','VILLA'])[((n-1)%5)+1],city,
 'Fictional address: '||n||' Demo Lane, '||city,
 'Fictional accommodation for project testing, not a real business. Illustrative photos do not depict this property. Includes varied room capacities and pay-at-hotel booking.'
FROM cities CROSS JOIN generate_series(1,25) AS n
ON CONFLICT(demo_key) DO NOTHING;

INSERT INTO room_types(property_id,name,capacity,total_quantity,base_nightly_price)
SELECT p.id, CASE WHEN p.property_type='VILLA' THEN 'Entire villa' ELSE r.name END,
 CASE WHEN p.property_type='VILLA' THEN 6 ELSE r.capacity END,
 CASE WHEN p.property_type='VILLA' THEN 1 ELSE 4 + (split_part(p.demo_key,'-',5)::int % 9) END,
 (1200 + split_part(p.demo_key,'-',4)::int * 80 + split_part(p.demo_key,'-',5)::int * 55 + r.extra)::numeric
FROM properties p CROSS JOIN (VALUES ('Standard',2,0),('Deluxe',3,900),('Family suite',5,2200)) AS r(name,capacity,extra)
WHERE p.demo_key LIKE 'hotelvista-demo-v1-%' AND (p.property_type<>'VILLA' OR r.name='Standard')
ON CONFLICT(property_id,name) DO NOTHING;

INSERT INTO room_inventory(room_type_id,stay_date,sellable_quantity,reserved_quantity)
SELECT r.id,(CURRENT_TIMESTAMP AT TIME ZONE p.timezone)::date + d,r.total_quantity,0
FROM room_types r JOIN properties p ON p.id=r.property_id CROSS JOIN generate_series(0,89) AS d
WHERE p.demo_key LIKE 'hotelvista-demo-v1-%' AND r.active AND p.active
ON CONFLICT(room_type_id,stay_date) DO NOTHING;
