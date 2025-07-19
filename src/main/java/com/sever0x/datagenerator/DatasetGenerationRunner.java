package com.sever0x.datagenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sever0x.datagenerator.service.DatasetGenerationService;
import com.sever0x.datagenerator.service.DocumentFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class DatasetGenerationRunner implements CommandLineRunner {

	private final DatasetGenerationService generationService;
	private final DocumentFileService fileService;

	public DatasetGenerationRunner(DatasetGenerationService generationService, DocumentFileService fileService) {
		this.generationService = generationService;
		this.fileService = fileService;
	}

	@Override
	public void run(String... args) {
		if (args.length == 0) {
			System.out.println("Usage: java -jar app.jar --generate [--size=300] [--output=./dataset]");
			return;
		}

		Map<String, String> params = parseArgs(args);

		if (params.containsKey("generate")) {
			if (params.containsKey("size")) {
				int size = Integer.parseInt(params.get("size"));
				generationService.setDatasetSize(size);
			}

			if (params.containsKey("output")) {
				fileService.setBasePath(params.get("output"));
			}

			System.out.println("Starting dataset generation...");
			long start = System.currentTimeMillis();

			generationService.generateFullDataset();

			long duration = (System.currentTimeMillis() - start) / 1000;
			System.out.println("Generation completed in " + duration + "s");
			System.out.println("Files saved to: " + fileService.getDatasetPath());

			showStats();
		}
	}

	private Map<String, String> parseArgs(String[] args) {
		Map<String, String> params = new HashMap<>();
		for (String arg : args) {
			if (arg.startsWith("--")) {
				String[] parts = arg.substring(2).split("=", 2);
				params.put(parts[0], parts.length > 1 ? parts[1] : "true");
			}
		}
		return params;
	}

	private void showStats() {
		try {
			String statsPath = fileService.getDatasetPath() + "/statistics/dataset_stats.json";
			if (Files.exists(Paths.get(statsPath))) {
				ObjectMapper mapper = new ObjectMapper();
				Map<String, Object> stats = mapper.readValue(new File(statsPath), Map.class);

				System.out.println("\nDataset stats:");
				System.out.println("Total documents: " + stats.get("total_documents"));
				System.out.println("Train/dev/test: " + stats.get("train_size") + "/" + stats.get("dev_size") + "/" + stats.get("test_size"));

				Map<String, Integer> entities = (Map<String, Integer>) stats.get("entity_counts");
				System.out.println("Entities: " + entities);
			}
		} catch (Exception e) {
			log.warn("Could not show stats", e);
		}
	}
}