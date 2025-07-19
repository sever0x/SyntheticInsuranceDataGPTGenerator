package com.sever0x.datagenerator.data;

import com.sever0x.datagenerator.types.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentData {
	private int documentId;
	private String rawContent;
	private String conllContent;
	private InsuranceEntities entities;
	private DocumentType documentType;
	private String filePath;
}
