CREATE STREAM volt2
  PARTITION ON COLUMN a
  (
    a INTEGER NOT NULL,
    b VARCHAR(128)
);
CREATE TOPIC USING STREAM volt2 EXECUTE PROCEDURE VOLT2.insert PROPERTIES(topic.format=avro);
