package com.sever0x.datagenerator.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InsuranceEntities {
	@JsonProperty("contract_numbers")
	private List<String> contractNumbers = new ArrayList<>();

	@JsonProperty("customer_ids")
	private List<String> customerIds = new ArrayList<>();

	@JsonProperty("company_names")
	private List<String> companyNames = new ArrayList<>();

	@JsonProperty("person_names")
	private List<String> personNames = new ArrayList<>();

	@JsonProperty("amounts")
	private List<String> amounts = new ArrayList<>();

	@JsonProperty("dates")
	private List<String> dates = new ArrayList<>();

	@JsonProperty("addresses")
	private List<String> addresses = new ArrayList<>();
}