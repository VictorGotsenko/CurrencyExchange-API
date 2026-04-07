/*DROP TABLE IF EXISTS currencies;*/
/*DROP INDEX IF EXISTS idx_currencies_code;*/

CREATE TABLE IF NOT EXISTS currencies (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    fullname VARCHAR(255) NOT NULL UNIQUE,
    sign VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    table_constraints
);

CREATE UNIQUE INDEX idx_currencies_id_code
    ON currencies(id, code);