-- https://www.chicagopolice.org/statistics-data/public-arrest-data/
create table if not exists crime_data
(
    district int  not null,
    year     int  not null,
    month    int  not null,
    fbi_code text not null
);

drop view if exists crime_breakdown;

create view crime_breakdown as
SELECT district,
       year,
       COUNT(*) AS total_crimes
FROM crime_data
GROUP BY district, year
ORDER BY district, year;
