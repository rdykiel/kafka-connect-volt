CREATE STREAM volt1
  PARTITION ON COLUMN a
  EXPORT TO TOPIC volt1
  (
    a INTEGER NOT NULL,
    b VARCHAR(128)
);
