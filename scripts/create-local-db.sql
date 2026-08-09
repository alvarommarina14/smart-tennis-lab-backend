-- Crea la base y el usuario de desarrollo local de Smart Tennis Lab.
-- Correr como superusuario (postgres):
--
--   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -f scripts\create-local-db.sql
--
-- Solo para desarrollo local: en producción las credenciales van por variable de entorno.

CREATE USER stl WITH PASSWORD 'stl';

CREATE DATABASE smarttennislab OWNER stl;

GRANT ALL PRIVILEGES ON DATABASE smarttennislab TO stl;

-- Flyway crea las tablas dentro del schema public, así que stl tiene que poder escribir ahí.
\connect smarttennislab

GRANT ALL ON SCHEMA public TO stl;
ALTER SCHEMA public OWNER TO stl;
