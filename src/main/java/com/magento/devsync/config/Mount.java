package com.magento.devsync.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Mount {

	@JsonProperty
	public String local;
	
	@JsonProperty
	public String remote;
	
	@JsonProperty
	public List<SyncRule> once;

	@JsonProperty
    public List<SyncRule> watch;

}
