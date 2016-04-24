-- Tables for tenants
use zyber_tenants;

CREATE TABLE zyber_tenants.tenants (
	  tenant_id uuid,
	  tenant_name text, 
	  contact_name text, 
	  contact_phone text, 
	  contact_email text, 
	  --CustomerId,
	  created_date timestamp,
	  subdomain text,
      PRIMARY KEY (tenant_id)
 );
 
 CREATE INDEX subdomainIdx ON zyber_tenants.tenants(subdomain);
 
 
 CREATE TABLE zyber_tenants.administrators (
  	user_id uuid,
	username text,
	password_hash text,
	created_date timestamp,
	name text,
	active Boolean,
	language text,
	PRIMARY KEY (user_id)
);

CREATE INDEX usernameIdx ON zyber_tenants.administrators(username);

CREATE TABLE zyber_tenants.application_settings (
      key text,
      value text,
      PRIMARY KEY (key)
 );