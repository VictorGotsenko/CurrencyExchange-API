DROP TABLE IF EXISTS exchangerates;
DROP INDEX IF EXISTS idx_exchangerates_base_target;

CREATE TABLE IF NOT EXISTS exchangerates (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    basecurrencyid INTEGER NOT NULL,
    targetcurrencyid INTEGER NOT NULL,
    rate DECIMAL(6) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    table_constraints,
    FOREIGN KEY (basecurrencyid) REFERENCES currencies(id) ON DELETE CASCADE,
    FOREIGN KEY (targetcurrencyid) REFERENCES currencies(id) ON DELETE CASCADE
    );
CREATE UNIQUE INDEX idx_exchangerates_base_target
ON exchangerates(basecurrencyid, targetcurrencyid);