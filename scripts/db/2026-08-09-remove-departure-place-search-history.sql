-- Apply only after deploying the application version that no longer reads or
-- writes departure-place search history. Back up the target database first.
-- The child table must be dropped before its referenced parent table.

DROP TABLE IF EXISTS departure_place_search_candidates;
DROP TABLE IF EXISTS departure_place_searches;
