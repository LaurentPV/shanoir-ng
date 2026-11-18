/**
 * Shanoir NG - Import, manage and share neuroimaging data
 * Copyright (C) 2009-2019 Inria - https://www.inria.fr/
 * Contact us on https://project.inria.fr/shanoir/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html
 */

package org.shanoir.ng.importer.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.media.DicomDirReader;
import org.dcm4che3.util.UIDUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DicomDirGeneratorServiceTest {

    private static final int NB_IMPORTS = 8;

    private static final int NB_INSTANCES_PER_IMPORT = 30;

    private DicomDirGeneratorService dicomDirGeneratorService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    public void setup() {
        dicomDirGeneratorService = new DicomDirGeneratorService();
    }

    @Test
    public void generateDicomDirReferencesAllInstancesOfDirectory() throws Exception {
        File importDir = tempDir.resolve("import").toFile();
        Set<String> expectedUIDs = createDicomFiles(importDir, "PATIENT", NB_INSTANCES_PER_IMPORT);
        File dicomDir = new File(importDir, "DICOMDIR");

        dicomDirGeneratorService.generateDicomDirFromDirectory(dicomDir, importDir);

        assertEquals(expectedUIDs, readInstanceUIDs(dicomDir));
    }

    /**
     * The service is a Spring singleton shared by all import requests:
     * concurrent generations must not write into each other's DICOMDIR.
     */
    @Test
    public void concurrentGenerationsDoNotMixDicomDirs() throws Exception {
        List<File> importDirs = new ArrayList<>();
        List<Set<String>> expectedUIDs = new ArrayList<>();
        for (int i = 0; i < NB_IMPORTS; i++) {
            File importDir = tempDir.resolve("import" + i).toFile();
            importDirs.add(importDir);
            expectedUIDs.add(createDicomFiles(importDir, "PATIENT" + i, NB_INSTANCES_PER_IMPORT));
        }

        CyclicBarrier start = new CyclicBarrier(NB_IMPORTS);
        ExecutorService executor = Executors.newFixedThreadPool(NB_IMPORTS);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (File importDir : importDirs) {
                Callable<Void> generation = () -> {
                    start.await();
                    dicomDirGeneratorService.generateDicomDirFromDirectory(new File(importDir, "DICOMDIR"), importDir);
                    return null;
                };
                futures.add(executor.submit(generation));
            }
            for (Future<Void> future : futures) {
                // rethrows any exception raised during a generation
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        for (int i = 0; i < NB_IMPORTS; i++) {
            File dicomDir = new File(importDirs.get(i), "DICOMDIR");
            assertTrue(dicomDir.exists(), "DICOMDIR missing for import " + i);
            assertEquals(expectedUIDs.get(i), readInstanceUIDs(dicomDir),
                    "DICOMDIR of import " + i + " must reference exactly its own instances");
        }
    }

    /**
     * Writes nbInstances DICOM Part 10 files of one patient/study/series into directory.
     *
     * @return the SOP Instance UIDs written
     */
    private Set<String> createDicomFiles(File directory, String patientID, int nbInstances) throws IOException {
        directory.mkdirs();
        String studyUID = UIDUtils.createUID();
        String seriesUID = UIDUtils.createUID();
        Set<String> sopInstanceUIDs = new HashSet<>();
        for (int i = 0; i < nbInstances; i++) {
            String sopInstanceUID = UIDUtils.createUID();
            Attributes dataset = new Attributes();
            dataset.setString(Tag.SOPClassUID, VR.UI, UID.MRImageStorage);
            dataset.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUID);
            dataset.setString(Tag.PatientID, VR.LO, patientID);
            dataset.setString(Tag.PatientName, VR.PN, patientID);
            dataset.setString(Tag.StudyInstanceUID, VR.UI, studyUID);
            dataset.setString(Tag.SeriesInstanceUID, VR.UI, seriesUID);
            dataset.setString(Tag.Modality, VR.CS, "MR");
            dataset.setInt(Tag.InstanceNumber, VR.IS, i + 1);
            Attributes fmi = dataset.createFileMetaInformation(UID.ExplicitVRLittleEndian);
            try (DicomOutputStream dos = new DicomOutputStream(new File(directory, String.format("IM%05d", i)))) {
                dos.writeDataset(fmi, dataset);
            }
            sopInstanceUIDs.add(sopInstanceUID);
        }
        return sopInstanceUIDs;
    }

    /**
     * Walks the DICOMDIR tree (patient / study / series / instance) and
     * returns the SOP Instance UIDs of all instance records.
     */
    private Set<String> readInstanceUIDs(File dicomDir) throws IOException {
        Set<String> uids = new HashSet<>();
        try (DicomDirReader reader = new DicomDirReader(dicomDir)) {
            for (Attributes patient = reader.readFirstRootDirectoryRecord(); patient != null;
                    patient = reader.readNextDirectoryRecord(patient)) {
                for (Attributes study = reader.readLowerDirectoryRecord(patient); study != null;
                        study = reader.readNextDirectoryRecord(study)) {
                    for (Attributes series = reader.readLowerDirectoryRecord(study); series != null;
                            series = reader.readNextDirectoryRecord(series)) {
                        for (Attributes instance = reader.readLowerDirectoryRecord(series); instance != null;
                                instance = reader.readNextDirectoryRecord(instance)) {
                            uids.add(instance.getString(Tag.ReferencedSOPInstanceUIDInFile));
                        }
                    }
                }
            }
        }
        return uids;
    }

}
