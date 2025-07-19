package com.sever0x.datagenerator.service;

import com.sever0x.datagenerator.data.DocumentData;
import com.sever0x.datagenerator.data.InsuranceEntities;
import com.sever0x.datagenerator.types.DocumentType;
import com.sever0x.datagenerator.types.InsuranceCompanyType;
import com.sever0x.datagenerator.types.WritingStyle;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class DatasetGenerationService {

	@Setter
	@Value("${dataset.size}")
	private int datasetSize;

	private final InsuranceDocumentGenerationService documentService;
	private final DocumentFileService fileService;

	public DatasetGenerationService(InsuranceDocumentGenerationService documentService, DocumentFileService fileService) {
		this.documentService = documentService;
		this.fileService = fileService;
	}

	public void generateFullDataset() {
		log.info("Starting generation of {} insurance documents", datasetSize);

		List<DocumentData> allDocuments = new ArrayList<>();

		for (int i = 1; i <= datasetSize; i++) {
			try {
				DocumentType docType = randomDocumentType();
				String document = generateVariedDocument(docType);
				InsuranceEntities entities = documentService.extractEntities(document);

				String rawFilePath = fileService.saveRawDocument(document, i, docType);
				String conllFilePath = fileService.saveAnnotatedDocument(document, entities, i);
				fileService.saveByType(document, docType, i);

				DocumentData docData = new DocumentData(i, document, Files.readString(Paths.get(conllFilePath)), entities, docType, rawFilePath);
				allDocuments.add(docData);

				if (i % 10 == 0) {
					System.out.println("Generated " + i + "/" + datasetSize + " documents");
				}

				Thread.sleep(200); // API rate limiting

			} catch (Exception e) {
				log.error("Failed to generate document {}", i, e);
			}
		}

		fileService.createTrainingSplits(allDocuments);
		fileService.exportForFlair();
	}

	private String generateVariedDocument(DocumentType docType) {
		// Add variety: 70% standard, 15% complex, 10% edge cases, 5% multilingual
		double rand = Math.random();

		if (rand < 0.15) {
			// Generate complex documents with multiple contracts
			return documentService.generateComplexDocument();
		} else if (rand < 0.25) {
			// Generate edge case documents for challenging NER
			return documentService.generateEdgeCaseDocument();
		} else if (rand < 0.30) {
			// Generate documents with international elements
			return documentService.generateMultiLanguageDocument();
		} else if (rand < 0.60) {
			// Generate with varied personality/style
			return documentService.generateWithPersonality(docType, randomWritingStyle(), randomCompanyType());
		} else {
			// Standard generation
			return generateDocumentByType(docType);
		}
	}

	private WritingStyle randomWritingStyle() {
		WritingStyle[] styles = WritingStyle.values();
		return styles[new Random().nextInt(styles.length)];
	}

	private InsuranceCompanyType randomCompanyType() {
		InsuranceCompanyType[] types = InsuranceCompanyType.values();
		return types[new Random().nextInt(types.length)];
	}

	private String generateDocumentByType(DocumentType docType) {
		return switch (docType) {
			case POLICY_CONFIRMATION -> documentService.generatePolicyConfirmation();
			case CLAIM_REPORT -> documentService.generateClaimReport();
			case PREMIUM_ADJUSTMENT -> documentService.generatePremiumAdjustment();
			case CANCELLATION -> documentService.generateCancellationLetter();
			case PAYMENT_REMINDER -> documentService.generatePaymentReminder();
			case INSURANCE_QUOTE -> documentService.generateInsuranceQuote();
		};
	}

	private DocumentType randomDocumentType() {
		DocumentType[] types = DocumentType.values();
		return types[new Random().nextInt(types.length)];
	}
}
