-- https://www.chicagopolice.org/statistics-data/public-arrest-data/
create table if not exists crime_data
(
    district int  not null,
    year     int  not null,
    month    int  not null,
    fbi_code  text not null
);