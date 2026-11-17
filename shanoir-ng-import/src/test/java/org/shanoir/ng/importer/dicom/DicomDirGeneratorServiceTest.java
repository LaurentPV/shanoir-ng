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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.media.DicomDirReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DicomDirGeneratorServiceTest {

    private static final String STUDY_UID = "1.2.3.4.5.1";
    private static final String SERIES_UID = "1.2.3.4.5.1.1";
    private static final String INSTANCE_UID = "1.2.3.4.5.1.1.1";

    @TempDir
    private Path tempDir;

    private DicomDirGeneratorService dicomDirGeneratorService;

    @BeforeEach
    public void setup() {
        dicomDirGeneratorService = new DicomDirGeneratorService();
    }

    @Test
    public void generateDicomDirReferencesDicomInstances() throws IOException {
        writeDicomInstance(tempDir.resolve("IM0001").toFile());
        File dicomDir = tempDir.resolve("DICOMDIR").toFile();

        dicomDirGeneratorService.generateDicomDirFromDirectory(dicomDir, tempDir.toFile());

        assertInstanceReferenced(dicomDir);
    }

    @Test
    public void generateDicomDirSkipsNonDicomFiles() throws IOException {
        writeDicomInstance(tempDir.resolve("IM0001").toFile());
        Files.writeString(tempDir.resolve("report.txt"), "Exam report, not DICOM data", StandardCharsets.UTF_8);
        // a file without file meta information, parsed as implicit VR without any DICOM instance attribute
        Attributes notAnInstance = new Attributes();
        notAnInstance.setString(Tag.PatientName, VR.PN, "Not^AnInstance");
        writeWithoutFileMetaInformation(tempDir.resolve("notes.bin").toFile(), notAnInstance);
        File dicomDir = tempDir.resolve("DICOMDIR").toFile();

        dicomDirGeneratorService.generateDicomDirFromDirectory(dicomDir, tempDir.toFile());

        assertInstanceReferenced(dicomDir);
    }

    @Test
    public void generateDicomDirRejectsDicomInstanceWithoutSopInstanceUid() throws IOException {
        Attributes instanceWithoutUid = instanceAttributes();
        instanceWithoutUid.remove(Tag.SOPInstanceUID);
        writeWithoutFileMetaInformation(tempDir.resolve("IM0002").toFile(), instanceWithoutUid);
        File dicomDir = tempDir.resolve("DICOMDIR").toFile();

        InvalidDicomFileException exception = assertThrows(InvalidDicomFileException.class,
                () -> dicomDirGeneratorService.generateDicomDirFromDirectory(dicomDir, tempDir.toFile()));

        assertTrue(exception.getMessage().contains("IM0002"));
    }

    private static Attributes instanceAttributes() {
        Attributes attributes = new Attributes();
        attributes.setString(Tag.SOPClassUID, VR.UI, UID.MRImageStorage);
        attributes.setString(Tag.SOPInstanceUID, VR.UI, INSTANCE_UID);
        attributes.setString(Tag.PatientID, VR.LO, "PATIENT-1");
        attributes.setString(Tag.PatientName, VR.PN, "Test^Patient");
        attributes.setString(Tag.StudyInstanceUID, VR.UI, STUDY_UID);
        attributes.setString(Tag.SeriesInstanceUID, VR.UI, SERIES_UID);
        attributes.setString(Tag.Modality, VR.CS, "MR");
        attributes.setInt(Tag.SeriesNumber, VR.IS, 1);
        attributes.setInt(Tag.InstanceNumber, VR.IS, 1);
        return attributes;
    }

    private static void writeDicomInstance(File file) throws IOException {
        Attributes dataset = instanceAttributes();
        Attributes fmi = dataset.createFileMetaInformation(UID.ExplicitVRLittleEndian);
        try (DicomOutputStream dos = new DicomOutputStream(file)) {
            dos.writeDataset(fmi, dataset);
        }
    }

    private static void writeWithoutFileMetaInformation(File file, Attributes dataset) throws IOException {
        try (DicomOutputStream dos = new DicomOutputStream(new FileOutputStream(file), UID.ImplicitVRLittleEndian)) {
            dos.writeDataset(null, dataset);
        }
    }

    private static void assertInstanceReferenced(File dicomDir) throws IOException {
        assertTrue(dicomDir.exists());
        try (DicomDirReader reader = new DicomDirReader(dicomDir)) {
            Attributes patient = reader.findPatientRecord("PATIENT-1");
            assertNotNull(patient);
            Attributes study = reader.findStudyRecord(patient, STUDY_UID);
            assertNotNull(study);
            Attributes series = reader.findSeriesRecord(study, SERIES_UID);
            assertNotNull(series);
            Attributes instance = reader.findLowerInstanceRecord(series, false, INSTANCE_UID);
            assertNotNull(instance);
            assertEquals("IM0001", instance.getStrings(Tag.ReferencedFileID)[0]);
            assertNull(reader.readNextDirectoryRecord(instance));
        }
    }

}
