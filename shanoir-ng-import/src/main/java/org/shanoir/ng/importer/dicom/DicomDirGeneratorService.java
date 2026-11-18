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

import java.io.File;
import java.io.IOException;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.media.DicomDirWriter;
import org.dcm4che3.media.RecordFactory;
import org.dcm4che3.media.RecordType;
import org.dcm4che3.tool.common.FilesetInfo;
import org.dcm4che3.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * This bean generates a DICOMDIR file.
 *
 * This bean is a singleton shared by all import requests: it must stay
 * stateless. Everything belonging to one generation (DICOMDIR writer,
 * record factory) is created per call and passed as parameter, so that
 * concurrent imports never write into each other's DICOMDIR.
 *
 * @author mkain
 *
 */
@Service
public class DicomDirGeneratorService {

    private static final Logger LOG = LoggerFactory.getLogger(DicomDirGeneratorService.class);

    private static final boolean CHECK_DUPLICATE = false;

    public void generateDicomDirFromDirectory(File dicomDir, File directory) throws IOException {
        DicomDirWriter out = createDicomDir(dicomDir);
        try {
            addReferenceTo(out, new RecordFactory(), directory, dicomDir);
        } finally {
            close(out);
        }
    }

    private DicomDirWriter createDicomDir(File file) throws IOException {
        FilesetInfo fsInfo = new FilesetInfo();
        DicomDirWriter.createEmptyDirectory(file, UIDUtils.createUIDIfNull(fsInfo.getFilesetUID()),
                fsInfo.getFilesetID(), fsInfo.getDescriptorFile(), fsInfo.getDescriptorFileCharset());
        return DicomDirWriter.open(file);
    }

    private int addReferenceTo(DicomDirWriter out, RecordFactory recFact, File f, File dicomDir) throws IOException {
        int n = 0;
        if (f.isDirectory()) {
            // skip macOS Finder's zip artifacts: the __MACOSX sibling tree holds
            // twins of the real files, not DICOM data
            if ("__MACOSX".equals(f.getName())) {
                return 0;
            }
            for (String s : f.list()) {
                n += addReferenceTo(out, recFact, new File(f, s), dicomDir);
            }
            return n;
        }
        // do not add reference to DICOMDIR
        if (f.equals(dicomDir)) {
            return 0;
        }
        // skip macOS Finder metadata: .DS_Store and Apple double "._<name>" files
        if (f.getName().startsWith(".")) {
            return 0;
        }

        Attributes fmi;
        Attributes dataset;
        DicomInputStream din = null;
        try {
            din = new DicomInputStream(f);
            din.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
            fmi = din.readFileMetaInformation();
            dataset = din.readDatasetUntilPixelData();
        } catch (IOException e) {
            return 0;
        } finally {
            if (din != null) {
                try {
                    din.close();
                } catch (Exception ignore) {
                }
            }
        }
        char prompt = '.';
        if (fmi == null) {
            fmi = dataset.createFileMetaInformation(UID.ImplicitVRLittleEndian);
            prompt = 'F';
        }
        String iuid = fmi.getString(Tag.MediaStorageSOPInstanceUID, null);
        if (iuid == null) {
            LOG.error("skip DICOM file '" + f + "' without SOP Instance UID (0008, 0018)");
            return 0;
        }
        return addRecords(out, recFact, dataset, n, out.toFileIDs(f), prompt, iuid, fmi);
    }

    private int addRecords(DicomDirWriter out, RecordFactory recFact, Attributes dataset, int num, String[] fileIDs,
            char prompt, String iuid, Attributes fmi) throws IOException {
        String pid = dataset.getString(Tag.PatientID, null);
        String pBirthDate = dataset.getString(Tag.PatientBirthDate);
        String pSex = dataset.getString(Tag.PatientSex);
        String studyInstanceUID = dataset.getString(Tag.StudyInstanceUID, null);
        String seriesInstanceUID = dataset.getString(Tag.SeriesInstanceUID, null);
        String seriesDescription = dataset.getString(Tag.SeriesDescription, null);

        if (studyInstanceUID != null) {
            if (pid == null) {
                pid = studyInstanceUID;
                dataset.setString(Tag.PatientID, VR.LO, pid);
                prompt = prompt == 'F' ? 'P' : 'p';
            }
            Attributes patRec = out.findPatientRecord(pid);
            if (patRec == null) {
                patRec = recFact.createRecord(RecordType.PATIENT, null, dataset, null, null);
                patRec.setSpecificCharacterSet("ISO_IR 192"); // set to UTF-8
                patRec.setString(Tag.PatientBirthDate, VR.DA, pBirthDate);
                patRec.setString(Tag.PatientSex, VR.CS, pSex);
                out.addRootDirectoryRecord(patRec);
                num++;
            }
            Attributes studyRec = out.findStudyRecord(patRec, studyInstanceUID);
            if (studyRec == null) {
                studyRec = recFact.createRecord(RecordType.STUDY, null, dataset, null, null);
                studyRec.setSpecificCharacterSet("ISO_IR 192"); // set to UTF-8
                out.addLowerDirectoryRecord(patRec, studyRec);
                num++;
            }

            if (seriesInstanceUID != null) {
                Attributes seriesRec = out.findSeriesRecord(studyRec, seriesInstanceUID);
                if (seriesRec == null) {
                    seriesRec = recFact.createRecord(RecordType.SERIES, null, dataset, null, null);
                    seriesRec.setSpecificCharacterSet("ISO_IR 192"); // set to UTF-8
                    seriesRec.setString(Tag.SeriesDescription, VR.LO, seriesDescription);
                    out.addLowerDirectoryRecord(studyRec, seriesRec);
                    num++;
                }

                if (iuid != null) {
                    Attributes instRec;
                    if (CHECK_DUPLICATE) {
                        instRec = out.findLowerInstanceRecord(seriesRec, false, iuid);
                        if (instRec != null) {
                            System.out.print('-');
                            return 0;
                        }
                    }
                    instRec = recFact.createRecord(dataset, fmi, fileIDs);
                    out.addLowerDirectoryRecord(seriesRec, instRec);
                    num++;
                }
            }
        } else {
            if (iuid != null) {
                if (CHECK_DUPLICATE) {
                    if (out.findRootInstanceRecord(false, iuid) != null) {
                        System.out.print('-');
                        return 0;
                    }
                }
                Attributes instRec = recFact.createRecord(dataset, fmi, fileIDs);
                out.addRootDirectoryRecord(instRec);
                prompt = prompt == 'F' ? 'R' : 'r';
                num++;
            }
        }
        return num;
    }

    private void close(DicomDirWriter out) {
        try {
            // DicomDirWriter.close() commits pending records to the DICOMDIR file
            out.close();
        } catch (IOException e) {
            LOG.error("Error while closing DICOMDIR: {}", e.getMessage(), e);
        }
    }

}
