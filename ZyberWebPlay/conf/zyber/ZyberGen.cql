// CQL Generated by zyber.GenerateCQL

CREATE TABLE zyber.access_subfolders (
  tenant_id uuid,
  parent_path_id uuid,
  principal_id uuid,
  path_id uuid,
    PRIMARY KEY ((tenant_id, parent_path_id), principal_id, path_id)
);

CREATE INDEX zyber_access_subfolders__principal_id ON zyber.access_subfolders(principal_id);
CREATE INDEX zyber_access_subfolders__path_id ON zyber.access_subfolders(path_id);

CREATE TABLE zyber.activity_timeline (
  tenant_id uuid,
  path_id uuid,
  user_id uuid,
  activity_timestamp timestamp,
  activity text,
  note text,
  icon text,
    PRIMARY KEY ((tenant_id, path_id), activity_timestamp)
);

CREATE INDEX zyber_activity_timeline__path_id ON zyber.activity_timeline(path_id);
CREATE INDEX zyber_activity_timeline__user_id ON zyber.activity_timeline(user_id);

CREATE TABLE zyber.application_settings (
  tenant_id uuid,
  key text,
  value text,
    PRIMARY KEY ((tenant_id, key))
);

CREATE TABLE zyber.file_data (
  tenant_id uuid,
  path_id uuid,
  version bigint,
  block_number bigint,
  bytes blob,
    PRIMARY KEY ((tenant_id, path_id, version), block_number)
);

CREATE TABLE zyber_secure.file_version (
  tenant_id uuid,
  path_id uuid,
  version timestamp,
  block_size int,
  size bigint,
  key_type text,
  key blob,
  data_encoding text,
  encoded_data_length bigint,
    PRIMARY KEY ((tenant_id, path_id), version)
);

CREATE TABLE zyber.group (
  tenant_id uuid,
  group_id uuid,
  name text,
  created_date timestamp,
  members int,
    PRIMARY KEY ((tenant_id), group_id)
);

CREATE INDEX zyber_group__group_id ON zyber.group(group_id);
CREATE INDEX zyber_group__name ON zyber.group(name);

CREATE TABLE zyber.group_member (
  tenant_id uuid,
  group_id uuid,
  member_principal_id uuid,
  joined_date timestamp,
    PRIMARY KEY ((tenant_id, group_id), member_principal_id)
);

CREATE INDEX zyber_group_member__group_id ON zyber.group_member(group_id);

CREATE MATERIALIZED VIEW group_member_by_principal AS
		  SELECT tenant_id, group_id, member_principal_id, joined_date 
		  FROM group_member
		  WHERE tenant_id IS NOT NULL AND group_id IS NOT NULL AND member_principal_id IS NOT NULL AND 
		  joined_date IS NOT NULL    
		  PRIMARY KEY ((tenant_id, member_principal_id), group_id) 
         WITH CLUSTERING ORDER BY (group_id desc);
;
CREATE TABLE zyber.group_member_flat (
  tenant_id uuid,
  group_id uuid,
  member_principal_id uuid,
  source_group_id uuid,
  member_principal_type text /* Principal.PrincipalType*/,
    PRIMARY KEY ((tenant_id, group_id), member_principal_id, source_group_id)
);

CREATE MATERIALIZED VIEW group_member_flat_user_groups AS
		  SELECT tenant_id, group_id, member_principal_id, member_principal_type, source_group_id 
		  FROM group_member_flat
		  WHERE tenant_id IS NOT NULL AND group_id IS NOT NULL AND member_principal_id IS NOT NULL AND 
		  member_principal_type IS NOT NULL AND source_group_id IS NOT NULL    
		  PRIMARY KEY ((tenant_id,member_principal_id),source_group_id, group_id) 
         WITH CLUSTERING ORDER BY (group_id desc);
;
CREATE MATERIALIZED VIEW group_member_flat_by_source AS
		  SELECT tenant_id, group_id, member_principal_id, member_principal_type, source_group_id 
		  FROM group_member_flat
		  WHERE tenant_id IS NOT NULL AND group_id IS NOT NULL AND member_principal_id IS NOT NULL AND 
		  member_principal_type IS NOT NULL AND source_group_id IS NOT NULL    
		  PRIMARY KEY ((tenant_id, source_group_id, member_principal_id), group_id) 
         WITH CLUSTERING ORDER BY (group_id desc);
;
CREATE TABLE zyber.metadata (
  tenant_id uuid,
  path_id uuid,
  key text,
  value set<text>,
    PRIMARY KEY ((tenant_id, path_id), key)
);

CREATE TABLE zyber_secure.password_history (
  tenant_id uuid,
  user_id uuid,
  password_hash text,
  password_hash_type text,
    PRIMARY KEY ((tenant_id, user_id), password_hash)
);

CREATE TABLE zyber.paths (
  tenant_id uuid,
  parent_path_id uuid,
  path_id uuid,
  name text,
  type text /* PathType*/,
  created_date timestamp,
  modified_date timestamp,
  share_id uuid,
  linked_id uuid,
  deleted boolean,
  mimeType text,
  size bigint,
  current_version timestamp,
    PRIMARY KEY ((tenant_id, parent_path_id), path_id)
);

CREATE INDEX zyber_paths__path_id ON zyber.paths(path_id);
CREATE INDEX zyber_paths__type ON zyber.paths(type);
CREATE INDEX zyber_paths__linked_id ON zyber.paths(linked_id);

CREATE MATERIALIZED VIEW path_orderby_created_date AS
  SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size,mimeType
  FROM paths
  WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL AND created_date IS NOT NULL
  PRIMARY KEY ((tenant_id,parent_path_id),created_date,path_id)
  WITH CLUSTERING ORDER BY (created_date desc); 

CREATE MATERIALIZED VIEW path_orderby_modified_date AS
  SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size,mimeType
  FROM paths
  WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL AND modified_date IS NOT NULL
  PRIMARY KEY ((tenant_id,parent_path_id),modified_date,path_id)
  WITH CLUSTERING ORDER BY (modified_date desc);

CREATE MATERIALIZED VIEW path_orderby_size AS
  SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size,mimeType
  FROM paths
  WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL AND size IS NOT NULL
  PRIMARY KEY ((tenant_id,parent_path_id),size,path_id)
  WITH CLUSTERING ORDER BY (size desc);

CREATE MATERIALIZED VIEW path_orderby_name AS
  SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size,mimeType
  FROM paths
  WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL
  PRIMARY KEY ((tenant_id,parent_path_id),name,path_id)
  WITH CLUSTERING ORDER BY (name desc);

CREATE TABLE zyber.path_security (
  tenant_id uuid,
  parent_path_id uuid,
  path_id uuid,
  principal_id uuid,
  principal_type text /* Principal.PrincipalType*/,
  security_type uuid,
    PRIMARY KEY ((tenant_id, parent_path_id), path_id, principal_id, principal_type)
);

CREATE TABLE zyber.shares (
  tenant_id uuid,
  share_id uuid,
  share_type text,
  user_id uuid,
  path_id uuid,
  expiry timestamp,
  password text,
    PRIMARY KEY ((tenant_id), share_id, share_type, user_id)
);

CREATE TABLE zyber.principal (
  tenant_id uuid,
  principal_id uuid,
  type text /* PrincipalType*/,
  root_folder uuid,
  created_date timestamp,
  active boolean,
  displayName text,
    PRIMARY KEY ((tenant_id), principal_id)
);

CREATE TABLE zyber.resumable_transfer (
  tenant_id uuid,
  resumable_transfer_id uuid,
  name text,
  total_chunks int,
  total_size bigint,
    PRIMARY KEY ((tenant_id, resumable_transfer_id))
);

CREATE TABLE zyber.resumable_transfer_blocks (
  tenant_id uuid,
  resumable_transfer_id uuid,
  chunk_number bigint,
  chunk_data blob,
  chunk_size bigint,
    PRIMARY KEY ((tenant_id, resumable_transfer_id), chunk_number)
);

CREATE TABLE zyber.security_type (
  tenant_id uuid,
  name text,
  security_id uuid,
  permission int,
    PRIMARY KEY ((tenant_id, security_id))
);

CREATE TABLE zyber.term_store (
  tenant_id uuid,
  term_store_id uuid,
  name text,
  description text,
  allow_custom_terms boolean,
    PRIMARY KEY ((tenant_id), term_store_id)
);

CREATE INDEX zyber_term_store__term_store_id ON zyber.term_store(term_store_id);
CREATE INDEX zyber_term_store__name ON zyber.term_store(name);

CREATE TABLE zyber.term_store_term (
  tenant_id uuid,
  term_store_id uuid,
  term_id uuid,
  parent_term_id uuid,
  name text,
    PRIMARY KEY ((tenant_id, term_store_id), term_id)
);

CREATE TABLE zyber.trash (
  tenant_id uuid,
  user_id uuid,
  path_id uuid,
  parent_id uuid,
  name text,
  directory boolean,
    PRIMARY KEY ((tenant_id, user_id), path_id)
);

CREATE TABLE zyber.users (
  tenant_id uuid,
  user_id uuid,
  email text,
  associated_principal_id text,
  home_folder uuid,
  created_date timestamp,
  name text,
  language text,
  user_role uuid,
  phone_number text,
  country_code text,
  two_factor boolean,
  number_confirmed boolean,
  nonce uuid,
  authy_id int,
  active boolean,
  passwordExpiry timestamp,
    PRIMARY KEY ((tenant_id), user_id)
);

CREATE INDEX zyber_users__user_id ON zyber.users(user_id);
CREATE INDEX zyber_users__email ON zyber.users(email);
CREATE INDEX zyber_users__active ON zyber.users(active);

CREATE TABLE zyber_secure.user_keys (
  tenant_id uuid,
  user_id uuid,
  password_hash text,
  password_hash_type text,
  public_key text,
  private_key text,
  key_type text,
    PRIMARY KEY ((tenant_id, user_id))
);

CREATE TABLE zyber.user_role (
  tenant_id uuid,
  name text,
  role_id uuid,
  abilities int,
    PRIMARY KEY ((tenant_id, role_id))
);

CREATE TABLE zyber.viewer_info (
  token_id uuid,
  tenant_id uuid,
  user_id uuid,
  file_id uuid,
  created_time timestamp,
    PRIMARY KEY ((token_id))
);

