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

package org.shanoir.ng.study.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.shanoir.ng.center.model.Center;
import org.shanoir.ng.center.repository.CenterRepository;
import org.shanoir.ng.manufacturermodel.service.ManufacturerModelServiceImpl;
import org.shanoir.ng.messaging.StudyUserUpdateBroadcastService;
import org.shanoir.ng.shared.configuration.RabbitMQConfiguration;
import org.shanoir.ng.shared.exception.EntityNotFoundException;
import org.shanoir.ng.shared.exception.MicroServiceCommunicationException;
import org.shanoir.ng.shared.security.rights.StudyUserRight;
import org.shanoir.ng.study.model.Study;
import org.shanoir.ng.study.model.StudyUser;
import org.shanoir.ng.study.repository.StudyRepository;
import org.shanoir.ng.study.repository.StudyUserRepository;
import org.shanoir.ng.study.security.StudySecurityService;
import org.shanoir.ng.studycenter.StudyCenter;
import org.shanoir.ng.subject.model.Subject;
import org.shanoir.ng.subject.repository.SubjectRepository;
import org.shanoir.ng.subject.service.SubjectService;
import org.shanoir.ng.subjectstudy.model.SubjectStudy;
import org.shanoir.ng.subjectstudy.model.SubjectStudyTag;
import org.shanoir.ng.subjectstudy.repository.SubjectStudyRepository;
import org.shanoir.ng.subjectstudy.service.SubjectStudyService;
import org.shanoir.ng.tag.model.Tag;
import org.shanoir.ng.utils.KeycloakUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of study service.
 * 
 * @author msimon
 *
 */
@Component
public class RelatedDatasetServiceImpl implements RelatedDatasetService {

	@Autowired
	private StudyUserRepository studyUserRepository;
	@Autowired
	private StudyService studyService;
	@Autowired
	private CenterRepository centerRepository;
	@Autowired
	private StudyRepository studyRepository;
	@Autowired
	private SubjectStudyRepository subjectStudyRepository;

	@Autowired
	private SubjectStudyService subjectStudyService;

	@Autowired
	private SubjectRepository subjectRepository;
	@Autowired
	private RabbitTemplate rabbitTemplate;
	private static final Logger LOG = LoggerFactory.getLogger(RelatedDatasetServiceImpl.class);

	@Transactional
	public void addSubjectStudyToNewStudy(String subjectIds, String id) {
		Study study = studyService.findById(Long.valueOf(id));
		Boolean toAdd = true;
		for (String subjectId : subjectIds.split(",")) {
			Subject subject = subjectRepository.findById(Long.valueOf(subjectId)).orElse(null);

			System.out.println("subject : " + subjectId + " / " + subject.getName());
			List<SubjectStudy> subjectStudyList = study.getSubjectStudyList();
			System.out.println("subjectStudyList size " + subjectStudyList.size());

			for (SubjectStudy subjectStudy : subjectStudyList) {
				if (subjectStudy.getSubject().equals(subject)) {
					toAdd = false;
					break;
				} else {
					toAdd = true;
				}
			}

			if(toAdd) {
				System.out.println("create new subjectStudy");
				SubjectStudy ssToAdd = new SubjectStudy();
				ssToAdd.setStudy(study);
				//ssToAdd.setSubjectType(subject.getSubjectType());
				ssToAdd.setSubject(subject);
				//ssToAdd.setPhysicallyInvolved(subject.isPhysicallyInvolved());

				System.out.println("ssToAdd : studyName = " + ssToAdd.getStudy().getName() + " / id = " + ssToAdd.getId() + " / subjectName = " + ssToAdd.getSubject().getName());
				System.out.println("before add subjectStudy to subjectStudyList : " + subjectStudyList.size());
				subjectStudyList.add(ssToAdd);
				study.setSubjectStudyList(subjectStudyList);
				System.out.println("after add subjectStudy to subjectStudyList : " + subjectStudyList.size());
				// subjectStudyRepository.save(ssToAdd);
				studyRepository.save(study);
			}
		}
	}

	public String addCenterAndCopyDatasetToStudy(String datasetSubjectIds, String id, String centerIds) {
		String result = "";
		Long userId = KeycloakUtil.getTokenUserId();
		Long studyId = Long.valueOf(id);

		Study study = studyService.findById(studyId);
		StudyUser studyUser = studyUserRepository.findByUserIdAndStudy_Id(userId, studyId);
		if (studyUser == null) {
			return "You must be part of both studies to copy datasets.";
		} else {
			List<StudyUserRight> rights = studyUser.getStudyUserRights();

			if (rights.contains(StudyUserRight.CAN_ADMINISTRATE) || rights.contains(StudyUserRight.CAN_IMPORT)) {
				result = addCenterToStudy(study, centerIds);

				try {
					copyDatasetToStudy(datasetSubjectIds, id);
					result = "Copy worked !";
				} catch (MicroServiceCommunicationException e) {
					throw new RuntimeException(e);
				}
			} else {
				LOG.error("Missing IMPORT or ADMIN rights on destination study " + study.getName());
				return "Missing IMPORT or ADMIN rights on destination study " + study.getName();
			}
			return result;
		}
	}

	private String addCenterToStudy(Study study, String centerIds) {
		Long studyId = study.getId();
		for (String centerId : centerIds.split(",")) {
			if (!centerRepository.findByStudy(studyId).contains(centerId)) {

				List<StudyCenter> studyCenterList = study.getStudyCenterList();
				Center center = centerRepository.findById(Long.valueOf(centerId)).orElse(null);
				List<Center> centersOfStudy = centerRepository.findByStudy(studyId);

				if (center != null && !centersOfStudy.contains(center)) {
					LOG.info("Add center " + center.getName() + " to study " + study.getName() + ".");
					StudyCenter centerToAdd = new StudyCenter();
					centerToAdd.setStudy(study);
					centerToAdd.setCenter(center);
					centerToAdd.setSubjectNamePrefix(null);
					studyCenterList.add(centerToAdd);
					study.setMonoCenter(false);
					study.setStudyCenterList(studyCenterList);
					studyRepository.save(study);
				} else {
					LOG.info("Center " + center.getName() + " already exists in study " + study.getName() + ".");
				}
			}
		}
		return "Center added to study";
	}

	private boolean copyDatasetToStudy(String datasetSubjectIds, String studyId) throws MicroServiceCommunicationException {
		System.out.println("copyDatasetToStudy datasetSubjectIds : " + datasetSubjectIds + " / studyId : " + studyId);
		try {
			rabbitTemplate.convertAndSend(RabbitMQConfiguration.COPY_DATASETS_TO_STUDY, datasetSubjectIds + ";" + studyId);
			return true;
		} catch (AmqpException e) {
			throw new MicroServiceCommunicationException(
					"Error while communicating with datasets MS to copy datasets to study.", e);
		}
	}
}
