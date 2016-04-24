package zyber.server.dao;

import java.util.UUID;

import zyber.driver.mapping.annotations.Index;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "zyber", name = TermStore.NAME)
public class TermStore extends InTenant {
	public static final String NAME = "term_store";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
//	@PartitionKey(1)
	
	@Index
	@ClusteringColumn
	@Column(name = "term_store_id")
	private UUID TermStoreId;
	
	@Index
	@Column(name = "name")
	private String name;
	
	@Column(name = "description")
    private String description;
    
	@Column(name = "allow_custom_terms")
    private Boolean allowCustomTerms;

	public UUID getTermStoreId() {
		return TermStoreId;
	}
	
	public TermStore() {
	}

	public TermStore(String name, String description, Boolean allowCustomTerms) {
		this.name = name;
		this.description = description;
		this.allowCustomTerms = allowCustomTerms;
	}



	public void setTermStoreId(UUID termStoreId) {
		TermStoreId = termStoreId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getAllowCustomTerms() {
		return allowCustomTerms;
	}

	public void setAllowCustomTerms(Boolean allowCustomTerms) {
		this.allowCustomTerms = allowCustomTerms;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}
    
    
}
