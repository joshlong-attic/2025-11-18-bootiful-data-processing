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
SELECT
    district,
    total_crimes,
    CASE
        WHEN crime_tertile = 1 THEN 'green'
        WHEN crime_tertile = 2 THEN 'yellow'
        WHEN crime_tertile = 3 THEN 'red'
        END AS category
FROM (
         SELECT
             district,
             COUNT(*) AS total_crimes,
             NTILE(3) OVER (ORDER BY COUNT(*)) AS crime_tertile
         FROM
            crime_data
         GROUP BY
             district
     ) AS crime_counts
ORDER BY
    total_crimes;
