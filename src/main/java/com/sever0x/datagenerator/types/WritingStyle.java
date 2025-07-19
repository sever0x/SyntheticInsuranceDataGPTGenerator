package com.sever0x.datagenerator.types;

import lombok.Getter;

@Getter
public enum WritingStyle {
	FORMAL("Sehr formal und geschäftlich"),
	FRIENDLY("Freundlich und persönlich"),
	TECHNICAL("Technisch und präzise"),
	LEGAL("Rechtlich und exakt"),
	SALES("Verkaufsorientiert und überzeugend");

	private final String description;
	WritingStyle(String description) { this.description = description; }
}
