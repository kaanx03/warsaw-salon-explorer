package com.kaandev.salonexplorer.ingestion.runner;

import com.kaandev.salonexplorer.ingestion.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("ingest")
@RequiredArgsConstructor
public class IngestionRunner implements ApplicationRunner {

    private final IngestionService ingestionService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("########################################");
        log.info("# Ingestion profile activated          #");
        log.info("########################################");

        var result = ingestionService.ingestAll();

        log.info("########################################");
        log.info("# Final report:                        #");
        log.info("# Total fetched : {}", result.totalFetched());
        log.info("# Inserted      : {}", result.inserted());
        log.info("# Updated       : {}", result.updated());
        log.info("# Skipped       : {}", result.skipped());
        log.info("# Failed        : {}", result.failed());
        log.info("# Duration      : {}ms", result.durationMs());
        log.info("########################################");
    }
}
