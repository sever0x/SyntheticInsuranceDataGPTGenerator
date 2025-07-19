package com.sever0x.datagenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sever0x.datagenerator.data.DocumentData;
import com.sever0x.datagenerator.data.InsuranceEntities;
import com.sever0x.datagenerator.types.DocumentType;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Setter
@Slf4j
@Service
public class DocumentFileService {

	@Value("${dataset.output-path}")
	private String basePath;

	@PostConstruct
	public void initDirectories() {
		createDirectoryStructure();
	}

	private void createDirectoryStructure() {
		try {
			Files.createDirectories(Paths.get(basePath, "raw_documents"));
			Files.createDirectories(Paths.get(basePath, "annotated_data"));
			Files.createDirectories(Paths.get(basePath, "training_data"));
			Files.createDirectories(Paths.get(basePath, "statistics"));
			Files.createDirectories(Paths.get(basePath, "by_type"));
			Files.createDirectories(Paths.get(basePath, "flair_ready"));

			log.info("Created directory structure at: {}", basePath);

		} catch (IOException e) {
			throw new RuntimeException("Failed to create directories", e);
		}
	}

	public String saveRawDocument(String content, int documentId, DocumentType docType) {
		String fileName = String.format("doc_%04d_%s.txt", documentId, docType.name().toLowerCase());
		Path filePath = Paths.get(basePath, "raw_documents", fileName);

		try {
			Files.writeString(filePath, content, StandardCharsets.UTF_8);
			log.debug("Saved raw document: {}", fileName);
			return filePath.toString();

		} catch (IOException e) {
			log.error("Failed to save raw document {}", fileName, e);
			throw new RuntimeException("File save failed", e);
		}
	}

	public String saveAnnotatedDocument(String content, InsuranceEntities entities, int documentId) {
		String conllContent = convertToCoNLLFormat(content, entities);
		String fileName = String.format("doc_%04d.conll", documentId);
		Path filePath = Paths.get(basePath, "annotated_data", fileName);

		try {
			Files.writeString(filePath, conllContent, StandardCharsets.UTF_8);
			log.debug("Saved annotated data: {}", fileName);
			return filePath.toString();
		} catch (IOException e) {
			log.error("Failed to save annotated document {}", fileName, e);
			throw new RuntimeException("Annotation save failed", e);
		}
	}

	private String convertToCoNLLFormat(String text, InsuranceEntities entities) {
		String[] sentences = text.split("\\n\\s*\\n|\\n(?=\\p{Upper})");
		StringBuilder conllOutput = new StringBuilder();

		for (String sentence : sentences) {
			if (sentence.trim().isEmpty()) continue;

			String[] tokens = tokenize(sentence);
			String[] labels = assignLabels(tokens, sentence, entities);

			for (int i = 0; i < tokens.length; i++) {
				conllOutput.append(tokens[i]).append("\t").append(labels[i]).append("\n");
			}
			conllOutput.append("\n");
		}

		return conllOutput.toString();
	}

	private String[] tokenize(String sentence) {
		// Simple German-aware tokenization
		return sentence.replaceAll("([.!?:;,])(?=\\s|$)", " $1").replaceAll("(\\d+)([.-])(\\d+)", "$1 $2 $3").split("\\s+");
	}

	private String[] assignLabels(String[] tokens, String originalSentence, InsuranceEntities entities) {
		String[] labels = new String[tokens.length];
		Arrays.fill(labels, "O");

		// Use original sentence for accurate position mapping
		assignEntityLabelsWithPositions(tokens, labels, originalSentence, entities.getContractNumbers(), "CONTRACT_NUMBER");
		assignEntityLabelsWithPositions(tokens, labels, originalSentence, entities.getCustomerIds(), "CUSTOMER_ID");
		assignEntityLabelsWithPositions(tokens, labels, originalSentence, entities.getCompanyNames(), "COMPANY_NAME");
		assignEntityLabelsWithPositions(tokens, labels, originalSentence, entities.getPersonNames(), "PERSON_NAME");

		return labels;
	}

	private void assignEntityLabelsWithPositions(String[] tokens, String[] labels, String originalSentence, List<String> entities, String entityType) {
		for (String entity : entities) {
			int entityStart = originalSentence.indexOf(entity);
			if (entityStart == -1) continue;

			// Find corresponding tokens for this entity position
			int[] tokenRange = findTokensForPosition(tokens, originalSentence, entityStart, entity.length());
			if (tokenRange[0] != -1 && tokenRange[1] != -1) {
				// Assign BIO labels
				if (tokenRange[0] == tokenRange[1]) {
					labels[tokenRange[0]] = "S-" + entityType;
				} else {
					labels[tokenRange[0]] = "B-" + entityType;
					for (int i = tokenRange[0] + 1; i <= tokenRange[1]; i++) {
						labels[i] = "I-" + entityType;
					}
				}
			}
		}
	}

	private int[] findTokensForPosition(String[] tokens, String originalSentence, int start, int length) {
		// Map character positions to token indices
		int charPos = 0;
		int startToken = -1, endToken = -1;

		for (int i = 0; i < tokens.length; i++) {
			// Skip whitespace
			while (charPos < originalSentence.length() && Character.isWhitespace(originalSentence.charAt(charPos))) {
				charPos++;
			}

			int tokenStart = charPos;
			int tokenEnd = charPos + tokens[i].length();

			// Check if this token overlaps with entity
			if (startToken == -1 && tokenEnd > start) {
				startToken = i;
			}
			if (tokenStart < start + length) {
				endToken = i;
			}

			charPos = tokenEnd;
		}

		return new int[]{startToken, endToken};
	}

	public void saveByType(String content, DocumentType docType, int documentId) {
		String typeDir = docType.name().toLowerCase();
		Path typeDirectory = Paths.get(basePath, "by_type", typeDir);

		try {
			Files.createDirectories(typeDirectory);
			String fileName = String.format("%s_%04d.txt", typeDir, documentId);
			Path filePath = typeDirectory.resolve(fileName);
			Files.writeString(filePath, content, StandardCharsets.UTF_8);

		} catch (IOException e) {
			log.error("Failed to save document by type", e);
		}
	}

	public void createTrainingSplits(List<DocumentData> allDocuments) {
		Collections.shuffle(allDocuments);

		int totalSize = allDocuments.size();
		int trainSize = (int) (totalSize * 0.7);
		int devSize = (int) (totalSize * 0.15);

		List<DocumentData> trainDocs = allDocuments.subList(0, trainSize);
		List<DocumentData> devDocs = allDocuments.subList(trainSize, trainSize + devSize);
		List<DocumentData> testDocs = allDocuments.subList(trainSize + devSize, totalSize);

		saveTrainingFile("train.conll", trainDocs);
		saveTrainingFile("dev.conll", devDocs);
		saveTrainingFile("test.conll", testDocs);

		saveDatasetStatistics(trainDocs.size(), devDocs.size(), testDocs.size(), allDocuments);

		log.info("Created training splits: train={}, dev={}, test={}", trainDocs.size(), devDocs.size(), testDocs.size());
	}

	private void saveTrainingFile(String fileName, List<DocumentData> documents) {
		Path filePath = Paths.get(basePath, "training_data", fileName);

		try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
			for (DocumentData doc : documents) {
				writer.write(doc.getConllContent());
				writer.write("\n\n");
			}
		} catch (IOException e) {
			log.error("Failed to save training file {}", fileName, e);
		}
	}

	public void saveDatasetStatistics(int trainSize, int devSize, int testSize, List<DocumentData> allDocuments) {
		Map<String, Object> stats = new HashMap<>();
		stats.put("generation_date", LocalDateTime.now().toString());
		stats.put("total_documents", allDocuments.size());
		stats.put("train_size", trainSize);
		stats.put("dev_size", devSize);
		stats.put("test_size", testSize);

		Map<String, Integer> entityCounts = countEntities(allDocuments);
		stats.put("entity_counts", entityCounts);

		Map<String, Integer> docTypeCounts = allDocuments.stream().collect(Collectors.groupingBy(doc -> doc.getDocumentType().name(), Collectors.summingInt(doc -> 1)));
		stats.put("document_type_counts", docTypeCounts);

		Path statsPath = Paths.get(basePath, "statistics", "dataset_stats.json");
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(statsPath.toFile(), stats);
			log.info("Saved dataset statistics to {}", statsPath);
		} catch (IOException e) {
			log.error("Failed to save statistics", e);
		}
	}

	private Map<String, Integer> countEntities(List<DocumentData> documents) {
		Map<String, Integer> counts = new HashMap<>();

		for (DocumentData doc : documents) {
			InsuranceEntities entities = doc.getEntities();
			counts.merge("CONTRACT_NUMBER", entities.getContractNumbers().size(), Integer::sum);
			counts.merge("CUSTOMER_ID", entities.getCustomerIds().size(), Integer::sum);
			counts.merge("COMPANY_NAME", entities.getCompanyNames().size(), Integer::sum);
			counts.merge("PERSON_NAME", entities.getPersonNames().size(), Integer::sum);
		}

		return counts;
	}

	public String getDatasetPath() {
		return basePath;
	}

	public void exportForFlair() {
		Path flairPath = Paths.get(basePath, "flair_ready");
		try {
			Files.createDirectories(flairPath);
			Files.copy(Paths.get(basePath, "training_data", "train.conll"), flairPath.resolve("train.txt"));
			Files.copy(Paths.get(basePath, "training_data", "dev.conll"), flairPath.resolve("dev.txt"));
			Files.copy(Paths.get(basePath, "training_data", "test.conll"), flairPath.resolve("test.txt"));

			log.info("Exported Flair-ready files to {}", flairPath);

		} catch (IOException e) {
			log.error("Failed to export for Flair", e);
		}
	}

}
