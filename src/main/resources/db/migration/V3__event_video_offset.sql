-- Cuando el análisis sale de un video, el "cuándo" que importa no es el reloj sino el momento
-- dentro del video, medido desde la marca de inicio del partido. Queda NULL para todo lo que se
-- carga en vivo desde el celular.
ALTER TABLE match_events
    ADD COLUMN video_offset_ms BIGINT;

ALTER TABLE match_events
    ADD CONSTRAINT ck_events_video_offset CHECK (video_offset_ms IS NULL OR video_offset_ms >= 0);
