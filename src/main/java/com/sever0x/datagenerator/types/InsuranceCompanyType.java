package com.sever0x.datagenerator.types;

import lombok.Getter;

@Getter
public enum InsuranceCompanyType {
	LARGE_INSURER("Große Versicherungskonzern (Allianz, AXA-Stil)"),
	REGIONAL_INSURER("Regionale Versicherung (persönlicher)"),
	INSURANCE_BROKER("Versicherungsmakler (beratend)"),
	SPECIALIST_INSURER("Spezialversicherer (technisch)");

	private final String description;
	InsuranceCompanyType(String description) { this.description = description; }
}
