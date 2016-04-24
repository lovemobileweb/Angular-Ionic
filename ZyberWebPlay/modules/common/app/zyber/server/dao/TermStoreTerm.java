package zyber.server.dao;

import java.util.UUID;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "zyber", name = TermStoreTerm.NAME)
public class TermStoreTerm extends InTenant {
	public static final String NAME = "term_store_term";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
	@Column(name = "term_store_id")
	@PartitionKey(1)
	private UUID termStoreId;

	@Column(name = "term_id")
	@ClusteringColumn
	private UUID termId;

	@Column(name = "parent_term_id")
	private UUID parentTermId;

	public UUID getTermId() {
		return termId;
	}

	public void setTermId(UUID termId) {
		this.termId = termId;
	}

	public UUID getParentTermId() {
		return parentTermId;
	}

	public void setParentTermId(UUID parentTermId) {
		this.parentTermId = parentTermId;
	}

	@Column(name = "name")
	private String name;

	public TermStoreTerm() {
	}

	public TermStoreTerm(String name) {
		super();
		this.name = name;
	}

	public TermStoreTerm(UUID termStoreId, UUID termId, String name) {
		super();
		this.termStoreId = termStoreId;
		this.termId = termId;
		this.name = name;
	}

	public UUID getTermStoreId() {
		return termStoreId;
	}

	public void setTermStoreId(UUID termStoreId) {
		this.termStoreId = termStoreId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

}
