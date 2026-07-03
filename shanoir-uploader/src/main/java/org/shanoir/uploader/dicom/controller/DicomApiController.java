package org.shanoir.uploader.dicom.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

import org.shanoir.ng.importer.dicom.ImagesCreatorAndDicomFileAnalyzerService;
import org.shanoir.ng.importer.model.ImportJob;
import org.shanoir.ng.importer.model.ImportJobStatus;
import org.shanoir.ng.importer.model.Patient;
import org.shanoir.uploader.ShUpConfig;
import org.shanoir.uploader.ShUpOnloadConfig;
import org.shanoir.uploader.action.DownloadOrCopyRunnable;
import org.shanoir.uploader.action.FindDicomActionListener;
import org.shanoir.uploader.action.ImportProgressListener;
import org.shanoir.uploader.action.event.DicomClientReadyEvent;
import org.shanoir.uploader.dicom.DicomServerClient;
import org.shanoir.uploader.dicom.dto.ConfigDTO;
import org.shanoir.uploader.dicom.query.Media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/dicom")
public class DicomApiController implements ApplicationListener<DicomClientReadyEvent> {

    public ShUpOnloadConfig shUpOnloadConfig = ShUpOnloadConfig.getInstance();

    private volatile DicomServerClient dicomServerClient;

    private ImagesCreatorAndDicomFileAnalyzerService dicomFileAnalyzer;

    private static final Logger logger = LoggerFactory.getLogger(DicomApiController.class);

    private final Lock importLock;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ConcurrentHashMap<String, ImportJobStatus> statusStore = new ConcurrentHashMap<>();

    public DicomApiController(Lock importLock, ImagesCreatorAndDicomFileAnalyzerService dicomFileAnalyzer) {
        this.importLock = importLock;
        this.dicomFileAnalyzer = new ImagesCreatorAndDicomFileAnalyzerService();
    }

    @Override
    public void onApplicationEvent(DicomClientReadyEvent event) {
        logger.debug(">>> DicomClientReadyEvent received, client = {}", event.getDicomServerClient());
        this.dicomServerClient = event.getDicomServerClient();
        logger.debug("DicomApiController: DicomServerClient ready.");
    }

    // Securize endpoints if called before initialization of DicomServerClient
    private DicomServerClient getClient() {
        if (dicomServerClient == null) {
            try {
                dicomServerClient = shUpOnloadConfig.getDicomServerClient();
                if (dicomServerClient == null) {
                    throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "DICOM client not yet initialized"
                    );
                }
            } catch (Exception e) {
                logger.error("Error retrieving DicomServerClient: " + e.getMessage(), e);
            }
        }
        return dicomServerClient;
    }

    @GetMapping("/echo")
    public HashMap<String, Boolean> echoDicomServer() {
        return new HashMap<String, Boolean>() {
            {
                put("success", getClient().echoDicomServer());
            }
        };
    }

    @GetMapping("/configuration")
    public ConfigDTO getDicomConfiguration() {
        Integer pacsDicomPort = null;
        Integer localDicomPort = null;

        if (ShUpConfig.dicomServerProperties.getProperty("dicom.server.port") != null && ShUpConfig.dicomServerProperties.getProperty("local.dicom.server.port") != null) {
            try {
                pacsDicomPort = Integer.valueOf(ShUpConfig.dicomServerProperties.getProperty("dicom.server.port"));
                localDicomPort = Integer.valueOf(ShUpConfig.dicomServerProperties.getProperty("local.dicom.server.port"));
            } catch (NumberFormatException e) {
                logger.error("Error parsing Dicom port numbers", e);
                return null;
            }
        }
        return new ConfigDTO(
            ShUpConfig.dicomServerProperties.getProperty("dicom.server.host"),
            pacsDicomPort,
            ShUpConfig.dicomServerProperties.getProperty("dicom.server.aet.called"),
            ShUpConfig.dicomServerProperties.getProperty("local.dicom.server.host"),
            localDicomPort,
            ShUpConfig.dicomServerProperties.getProperty("local.dicom.server.aet.calling")
            );
    }

    @PutMapping("/configuration")
    public void updateDicomConfiguration(@RequestBody ConfigDTO config) {
        setDicomProperties(ShUpConfig.dicomServerProperties, config);
        try (FileOutputStream fos = new FileOutputStream(ShUpConfig.shanoirUploaderFolder + File.separator + ShUpConfig.DICOM_SERVER_PROPERTIES)) {
            ShUpConfig.dicomServerProperties.store(fos, "Updated by user");
            logger.info("Dicom server properties updated by user");
        } catch (Exception e) {
            logger.error("Error updating Dicom configuration", e);
        }
    }

    @PostMapping("/query")
    public Object queryDicomServer(@RequestBody HashMap<String, String> queryParameters) throws Exception {
        logger.info("Querying Dicom server with parameters: {}", queryParameters);

        List<Patient> patients = getClient().queryDicomServer(Objects.equals(queryParameters.get("studyRootQuery"), "true"), queryParameters.get("modality"), queryParameters.get("patientName"), queryParameters.get("patientID"), queryParameters.get("studyDescription"), queryParameters.get("patientBirthDate"), queryParameters.get("studyDate"));
        Media media = new Media();
        logger.info(patients.toString());

        FindDicomActionListener.fillMediaWithPatients(media, patients);
        logger.info("Patients read from DICOM server: " + media.getTreeNodes().toString());
        logger.info("Media : " + media.getData().toString());

        return media.getData();
    }

    @PostMapping("/retrieve")
    public ResponseEntity<?> retrieveDicomSeries(@RequestBody ImportJob importJob) throws Exception {
        // Lock to avoid concurrent queries in parallel
        if (!importLock.tryLock()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "A previous import is still running. Please wait until it is finished."));
        }

        String jobId = UUID.randomUUID().toString();
        statusStore.put(jobId, new ImportJobStatus(0, "STARTED", null, false, false));

        ImportProgressListener restListener = new ImportProgressListener() {
            @Override
            public void onProgress(int percentage, String currentStep) {
                statusStore.put(jobId, new ImportJobStatus(percentage, currentStep, null, false, false));
            }

            @Override
            public void onComplete(String reportSummary, boolean success) {
                statusStore.put(jobId, new ImportJobStatus(100, "DONE", reportSummary, true, success));
                importLock.unlock();
            }
        };

        try {
            Map<String, ImportJob> importJobs = Map.of(importJob.getStudy().getStudyInstanceUID(), importJob);
            DownloadOrCopyRunnable runner = new DownloadOrCopyRunnable(
                    importJob.isFromPacs(), dicomServerClient, dicomFileAnalyzer,
                    null, importJobs, restListener);
            executor.submit(runner);
        } catch (Exception e) {
            importLock.unlock();
            logger.error("An error occured while running the thread.", e);
            return ResponseEntity.internalServerError().build();
        }
    return ResponseEntity.accepted().body(Map.of("importJobId", jobId));
    }

    @GetMapping("/importJobs/{jobId}/progress")
    public ResponseEntity<ImportJobStatus> getProgress(@PathVariable String jobId) {
        ImportJobStatus status = statusStore.get(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    private void setDicomProperties(Properties dicomServerProperties, ConfigDTO config) {
        dicomServerProperties.setProperty("dicom.server.host", config.getDistantDicomServer().getHost());
        dicomServerProperties.setProperty("dicom.server.port", String.valueOf(config.getDistantDicomServer().getPort()));
        dicomServerProperties.setProperty("dicom.server.aet.called", config.getDistantDicomServer().getAet());
        dicomServerProperties.setProperty("local.dicom.server.host", config.getLocalDicomServer().getHost());
        dicomServerProperties.setProperty("local.dicom.server.port", String.valueOf(config.getLocalDicomServer().getPort()));
        dicomServerProperties.setProperty("local.dicom.server.aet.calling", config.getLocalDicomServer().getAet());
    }

}
