package com.sever0x.datagenerator.types;

import lombok.Getter;

@Getter
public enum DocumentType {
	POLICY_CONFIRMATION("Versicherungspolice-Bestätigung"),
	CLAIM_REPORT("Schadensmeldung"),
	PREMIUM_ADJUSTMENT("Beitragsanpassung"),
	CANCELLATION("Kündigungsschreiben"),
	PAYMENT_REMINDER("Zahlungserinnerung"),
	INSURANCE_QUOTE("Versicherungsangebot");

	private final String germanName;
	DocumentType(String germanName) { this.germanName = germanName; }
}
