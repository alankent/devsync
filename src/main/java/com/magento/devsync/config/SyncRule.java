package com.magento.devsync.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SyncRule {

	@JsonProperty
	public String mode; // push,pull,sync
	
	@JsonProperty
	public String path;
	
	@JsonProperty
	public List<String> exclude;
	
}
