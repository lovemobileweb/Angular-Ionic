--Author : Neha Dave
--Create KeySpace
DROP KEYSPACE IF EXISTS zyber;
CREATE KEYSPACE zyber WITH replication = { 'class':'SimpleStrategy', 'replication_factor':'1' };

DROP KEYSPACE IF EXISTS zyber_secure;
CREATE KEYSPACE zyber_secure WITH replication = { 'class':'SimpleStrategy', 'replication_factor':'1' };
