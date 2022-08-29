package org.shanoir.ng.importer.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.shanoir.ng.dicom.web.StudyInstanceUIDHandler;
import org.shanoir.ng.dicom.web.service.DICOMWebService;
import org.shanoir.ng.examination.model.Examination;
import org.shanoir.ng.examination.repository.ExaminationRepository;
import org.shanoir.ng.shared.model.Subject;
import org.shanoir.ng.shared.repository.SubjectRepository;
import org.shanoir.ng.utils.KeycloakUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author mkain
 *
 */
@Service
public class DicomSRImporterService {
	
	private static final Logger LOG = LoggerFactory.getLogger(DicomSRImporterService.class);

	private static final String SR = "SR";

	@Autowired
	private StudyInstanceUIDHandler studyInstanceUIDHandler;
	
	@Autowired
	private ExaminationRepository examinationRepository;
	
	@Autowired
	private SubjectRepository subjectRepository;
	
	@Autowired
	private DICOMWebService dicomWebService;
	
	@Transactional
	public boolean importDicomSR(InputStream inputStream) {
		// DicomInputStream consumes the input stream to read the data
		try (DicomInputStream dIS = new DicomInputStream(inputStream)) {
			Attributes metaInformationAttributes = dIS.readFileMetaInformation();
			Attributes datasetAttributes = dIS.readDataset();
			// check for modality: DICOM SR
			if (SR.equals(datasetAttributes.getString(Tag.Modality))) {
				modifyDatasetAttributes(datasetAttributes);
				/**
				 * Create a new output stream to write the changes into and use its bytes
				 * to produce a new input stream to send later by http client to the DICOM server.
				 */
				ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
				DicomOutputStream dOS = new DicomOutputStream(bAOS, metaInformationAttributes.getString(Tag.TransferSyntaxUID));
				dOS.writeDataset(metaInformationAttributes, datasetAttributes);
				InputStream finalInputStream = new ByteArrayInputStream(bAOS.toByteArray());
				dicomWebService.sendDicomInputStreamToPacs(finalInputStream);
				finalInputStream.close();
				dOS.close();
			} else {
				LOG.error("Error: importDicomSR: other modality sent then SR.");
				return false;
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	/**
	 * This method replaces values of dicom tags within the DICOM SR file:
	 * - use user name as person name, who created the measurement
	 * - replace with correct study instance UID from pacs for correct storage
	 * - add subject name according to shanoir, as viewer sends a strange P-000001.
	 * 
	 * Note: the approach to replace the newly created SeriesInstanceUID
	 * with the referenced SeriesInstanceUID, available via CurrentRequested-
	 * ProcedureEvidenceSequence -> ReferencedSeriesSequence -> SeriesInstanceUID
	 * did not work to get it displayed correctly in the viewer, but lead even to
	 * an error in the viewer.
	 * 
	 * @param datasetAttributes
	 */
	private void modifyDatasetAttributes(Attributes datasetAttributes) {
		// set user name, as person, who created the measurement
		final String userName = KeycloakUtil.getTokenUserName();
		datasetAttributes.setString(Tag.PersonName, VR.PN, userName);
		// replace artificial examinationUID with real StudyInstanceUID in DICOM server
		String examinationUID = datasetAttributes.getString(Tag.StudyInstanceUID);
		String studyInstanceUID = studyInstanceUIDHandler.findStudyInstanceUIDFromCacheOrDatabase(examinationUID);
		datasetAttributes.setString(Tag.StudyInstanceUID, VR.UI, studyInstanceUID);
		// replace subject name, that is sent by the viewer wrongly with P-0000001 etc.
		Long examinationID = Long.valueOf(examinationUID.substring(StudyInstanceUIDHandler.PREFIX.length()));
		Examination examination = examinationRepository.findById(examinationID).get();
		Optional<Subject> subjectOpt = subjectRepository.findById(examination.getSubjectId());
		String subjectName = "error_subject_name_not_found_in_db";
		if (subjectOpt.isPresent()) {
			subjectName = subjectOpt.get().getName();
		}
		datasetAttributes.setString(Tag.PatientName, VR.PN, subjectName);
		datasetAttributes.setString(Tag.PatientID, VR.LO, subjectName);
	}

}
