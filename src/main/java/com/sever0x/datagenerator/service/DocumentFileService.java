package com.sever0x.datagenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sever0x.datagenerator.data.DocumentData;
import com.sever0x.datagenerator.types.DocumentType;
import com.sever0x.datagenerator.data.InsuranceEntities;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

		// Find entities in tokens
		assignEntityLabels(tokens, labels, entities.getContractNumbers(), "CONTRACT_NUMBER");
		assignEntityLabels(tokens, labels, entities.getCustomerIds(), "CUSTOMER_ID");
		assignEntityLabels(tokens, labels, entities.getCompanyNames(), "COMPANY_NAME");
		assignEntityLabels(tokens, labels, entities.getPersonNames(), "PERSON_NAME");

		return labels;
	}

	private void assignEntityLabels(String[] tokens, String[] labels, List<String> entities, String entityType) {
		for (String entity : entities) {
			String[] entityTokens = entity.split("\\s+");

			// Find matching token sequences
			for (int i = 0; i <= tokens.length - entityTokens.length; i++) {
				if (matchesEntityTokens(tokens, i, entityTokens)) {
					if (entityTokens.length == 1) {
						labels[i] = "S-" + entityType;
					} else {
						labels[i] = "B-" + entityType;
						for (int j = 1; j < entityTokens.length; j++) {
							if (i + j < labels.length) {
								labels[i + j] = "I-" + entityType;
							}
						}
					}
					break;
				}
			}
		}
	}

	private boolean matchesEntityTokens(String[] tokens, int startIndex, String[] entityTokens) {
		for (int i = 0; i < entityTokens.length; i++) {
			if (startIndex + i >= tokens.length) return false;
			if (!tokens[startIndex + i].equalsIgnoreCase(entityTokens[i])) return false;
		}
		return true;
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

	public Path getTrainingDataPath() {
		return Paths.get(basePath, "training_data");
	}

	public List<String> getAllDocumentPaths() {
		Path documentsPath = Paths.get(basePath, "raw_documents");
		try (Stream<Path> paths = Files.list(documentsPath)) {
			return paths.map(Path::toString)
					.sorted()
					.collect(Collectors.toList());
		} catch (IOException e) {
			log.error("Failed to list document paths", e);
			return Collections.emptyList();
		}
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

	public void createZipArchive() {
		String zipFileName = String.format("insurance_ner_dataset_%s.zip", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
		Path zipPath = Paths.get(basePath).getParent().resolve(zipFileName);

		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
			addDirectoryToZip(Paths.get(basePath), basePath, zos);
			log.info("Created dataset archive: {}", zipPath);
		} catch (IOException e) {
			log.error("Failed to create zip archive", e);
		}
	}

	private void addDirectoryToZip(Path directory, String basePath, ZipOutputStream zos) throws IOException {
		try (Stream<Path> paths = Files.walk(directory)) {
			paths.forEach(path -> {
				try {
					String zipEntryName = Paths.get(basePath).relativize(path).toString().replace("\\", "/");
					if (Files.isDirectory(path)) {
						zipEntryName += "/";
					}
					ZipEntry zipEntry = new ZipEntry(zipEntryName);
					zos.putNextEntry(zipEntry);
					if (Files.isRegularFile(path)) {
						Files.copy(path, zos);
					}
					zos.closeEntry();
				} catch (IOException e) {
					log.error("Error adding file to zip: {}", path, e);
				}
			});
		}
	}

}
