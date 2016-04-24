use zyber;

CREATE TABLE zyber.application_settings (
    tenant_id uuid,
    key text,
    value text, 
    PRIMARY KEY ((tenant_id,key))
 );

CREATE TABLE zyber.paths  (
    tenant_id uuid,
    parent_path_id uuid,
    path_id uuid,
    name text,
    type text,
    deleted Boolean,

    created_date timestamp,
    modified_date timestamp,
    share_id uuid,
    linked_id uuid,

    -- Applies to files with data.
    size bigint,
    current_version timestamp,
    PRIMARY KEY ((tenant_id,parent_path_id),path_id)
);

CREATE TABLE zyber.trash  (
    tenant_id uuid,
    user_id uuid,
    path_id uuid,
    parent_id uuid,
    name text,
    directory Boolean,
    PRIMARY KEY ((tenant_id,user_id),path_id)
);


CREATE INDEX zyber_path_id ON zyber.paths(path_id);
CREATE INDEX zyber_linked_id ON zyber.paths(linked_id);

CREATE MATERIALIZED VIEW path_orderby_created_date AS        
	SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size
	FROM paths
	WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL AND created_date IS NOT NULL    
	PRIMARY KEY ((tenant_id,parent_path_id),created_date,path_id) 
	WITH CLUSTERING ORDER BY (created_date desc); 
	
CREATE MATERIALIZED VIEW path_orderby_modified_date AS        
	SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size
	FROM paths
	WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL AND modified_date IS NOT NULL
	PRIMARY KEY ((tenant_id,parent_path_id),modified_date,path_id)
	WITH CLUSTERING ORDER BY (modified_date desc);

CREATE MATERIALIZED VIEW path_orderby_size AS        
	SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size
	FROM paths
	WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL AND size IS NOT NULL
	PRIMARY KEY ((tenant_id,parent_path_id),size,path_id)
	WITH CLUSTERING ORDER BY (size desc);

CREATE MATERIALIZED VIEW path_orderby_name AS
	SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size
	FROM paths
	WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL
	PRIMARY KEY ((tenant_id,parent_path_id),name,path_id)
	WITH CLUSTERING ORDER BY (name desc);


CREATE TABLE zyber_secure.file_version  (
    tenant_id uuid,
    path_id uuid,
    version timestamp,
    block_size int,
    size bigint,

    -- Key Type should look like 'AES'
    key_type text,
    key blob,

    data_encoding text,
	encoded_data_length bigint,
    PRIMARY KEY ((tenant_id,path_id), version)
);

CREATE TABLE zyber_secure.password_history (
  tenant_id uuid,
  user_id uuid,
  password_hash text,
  password_hash_type text,
  PRIMARY KEY ((tenant_id, user_id),password_hash)
);

CREATE TABLE zyber.metadata (
    tenant_id uuid,
    key text,
    value set<text>,
    path_id uuid,
    PRIMARY KEY ((tenant_id,path_id),key)
  );
CREATE INDEX zyber_value on metadata (value) ;


CREATE TABLE zyber.file_data (
    tenant_id uuid,
    path_id uuid,
    version bigint,
    block_number bigint,
    bytes blob,
    PRIMARY KEY ((tenant_id, path_id, version), block_number)
);

CREATE TABLE zyber.users (
    tenant_id uuid,
  	user_id uuid,
	email text,
	associated_principal_id text,
	home_folder uuid,
	created_date timestamp,
	name text,
	active Boolean,
	language text,
	phone_number text,
	country_code text,
	two_factor Boolean,
	number_confirmed Boolean,
	nonce uuid,
	PRIMARY KEY ((tenant_id),user_id)
);

CREATE INDEX emailIdx ON users(email);
CREATE INDEX activeUserIdx ON users(active);
CREATE INDEX user_by_id ON users(user_id);

CREATE TABLE zyber_secure.user_keys (
    tenant_id uuid,
	user_id uuid,
	password_hash text,
	password_hash_type text, -- MD5, SHA256
	public_key text,
	private_key text,
	key_type text, -- DSA256
        PRIMARY KEY ((tenant_id, user_id))
) WITH comment = 'Store user passwords and keys in a seperate table.';


CREATE TABLE principal (
    tenant_id uuid,
	principal_id uuid,
	type text,
	root_folder uuid,
	created_date timestamp,
	active Boolean, -- principal_name text, -- The name of the user or group.
    PRIMARY KEY ((tenant_id),principal_id)
) WITH comment = 'Either a user or a group. 1 user has 1 principal. But some principals have no user (a group). Related to user through AssociatedPrincipalID';

CREATE INDEX principal_by_id ON principal(principal_id);

CREATE TABLE group (
    tenant_id uuid,
	group_id uuid,
	owner_principal_id uuid,
	name text,
	members int,
	created_date timestamp,
	PRIMARY KEY ((tenant_id),group_id)
);
CREATE INDEX groupIdIdx ON group(group_id);
CREATE INDEX groupNameIdx ON group(name);
CREATE INDEX groupOwnerIdx ON group(owner_principal_id);

CREATE TABLE group_member (
    tenant_id uuid,
	group_id uuid,
	member_principal_id uuid,
	member_principal_type text,
	joined_date timestamp,
	PRIMARY KEY ((tenant_id),member_principal_id, group_id)
);
CREATE INDEX group_member_by_member ON group_member(member_principal_type);
CREATE INDEX group_member_by_group ON group_member(group_id);

CREATE TABLE group_member_flat (
    tenant_id uuid,
	member_principal_id uuid,
	member_principal_type text, 
	group_id uuid,
	PRIMARY KEY ((tenant_id, member_principal_id), group_id)
);

CREATE TABLE path_security (
    tenant_id uuid,
	path_id uuid,
	principal_id uuid,
	principal_type text, // One of User/Group.
	permission int,
        PRIMARY KEY ((tenant_id,path_id),principal_id, principal_type)
);

CREATE TABLE shares (
   tenant_id uuid,
   share_id uuid,
   share_type text,
   user_id uuid,
   path_id uuid,
   expiry timestamp,
   password text,
   PRIMARY KEY((tenant_id),share_id, share_type,user_id)
);

CREATE TABLE activity_timeline (
    tenant_id uuid,
	path_id uuid,
	user_id uuid,
    activity_timestamp timestamp,
    activity text, -- One of create, view, edit, change permissions
    note text, -- Description of the action such as added user permissions: a,b,c for change permissions or others. No note necessary for create/view/edit. For an edit we write in the version number.
    PRIMARY KEY ((tenant_id), path_id, user_id ,activity_timestamp)
);
CREATE INDEX activity_timeline_by_user ON activity_timeline(user_id);
CREATE INDEX activity_timeline_by_path ON activity_timeline(path_id);

CREATE TABLE term_store (
    tenant_id uuid,
	term_store_id uuid,
	name text,
    description text,
    allow_custom_terms Boolean,
    PRIMARY KEY ((tenant_id),term_store_id)
);
CREATE INDEX term_store_by_id ON term_store(term_store_id);
CREATE INDEX term_store_by_name ON term_store(name);

CREATE TABLE term_store_term (
    tenant_id uuid,
	term_id uuid,
	term_store_id uuid,
	parent_term_id uuid,
	name text,
    PRIMARY KEY ((tenant_id, term_store_id), term_id)
);

CREATE INDEX term_store_term_by_id ON term_store_term(term_id);