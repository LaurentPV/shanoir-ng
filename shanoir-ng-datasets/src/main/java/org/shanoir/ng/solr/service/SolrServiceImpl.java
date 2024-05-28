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

/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.shanoir.ng.solr.service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrServerException;
import org.shanoir.ng.dataset.model.Dataset;
import org.shanoir.ng.dataset.repository.DatasetRepository;
import org.shanoir.ng.dataset.service.DatasetService;
import org.shanoir.ng.shared.dateTime.DateTimeUtils;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.shanoir.ng.shared.model.Center;
import org.shanoir.ng.shared.model.SubjectStudy;
import org.shanoir.ng.tag.model.StudyTag;
import org.shanoir.ng.tag.model.Tag;
import org.shanoir.ng.shared.paging.PageImpl;
import org.shanoir.ng.shared.repository.CenterRepository;
import org.shanoir.ng.shared.repository.SubjectStudyRepository;
import org.shanoir.ng.shared.subjectstudy.SubjectType;
import org.shanoir.ng.solr.model.ShanoirMetadata;
import org.shanoir.ng.solr.model.ShanoirSolrDocument;
import org.shanoir.ng.solr.model.ShanoirSolrQuery;
import org.shanoir.ng.solr.repository.ShanoirMetadataRepository;
import org.shanoir.ng.solr.solrj.SolrJWrapper;
import org.shanoir.ng.study.rights.StudyUser;
import org.shanoir.ng.study.rights.StudyUserRightsRepository;
import org.shanoir.ng.tag.repository.StudyTagRepository;
import org.shanoir.ng.utils.KeycloakUtil;
import org.shanoir.ng.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.result.SolrResultPage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * @author yyao
 *
 */
@Service
public class SolrServiceImpl implements SolrService {
	
	@Autowired
	private SolrJWrapper solrJWrapper;

	@Autowired
	private ShanoirMetadataRepository shanoirMetadataRepository;

	@Autowired
	private StudyUserRightsRepository rightsRepository;

	@Autowired
	private SubjectStudyRepository subjectStudyRepo;

	@Autowired
	private CenterRepository centerRepository;

	@Autowired
	private DatasetRepository dsRepository;

	public void addToIndex (final ShanoirSolrDocument document) throws SolrServerException, IOException {
		solrJWrapper.addToIndex(document);
	}

	public void addAllToIndex (final List<ShanoirSolrDocument> documents) throws SolrServerException, IOException {
		solrJWrapper.addAllToIndex(documents);
	}

	public void deleteFromIndex(Long datasetId) throws SolrServerException, IOException {
		solrJWrapper.deleteFromIndex(datasetId);
	}

	public void deleteFromIndex(List<Long> datasetIds) throws SolrServerException, IOException {
		solrJWrapper.deleteFromIndex(datasetIds);
	}

	public void deleteAll() throws SolrServerException, IOException {
		solrJWrapper.deleteAll();
	}
	
	@Transactional
	@Override
	@Scheduled(cron = "0 0 6 * * *", zone="Europe/Paris")
	public void indexAll() throws SolrServerException, IOException {
		// 1. delete all
		deleteAll();
		// 2. get all datasets
		List<ShanoirMetadata> documents = shanoirMetadataRepository.findAllAsSolrDoc();
		indexDocumentsInSolr(documents);
	}

	@Transactional
	@Override
	public void indexDatasets(List<Long> datasetIds) throws SolrServerException, IOException {
		// Get all associated datasets and index them to solr
		List<ShanoirMetadata> metadatas = shanoirMetadataRepository.findSolrDocs(datasetIds);
		this.indexDocumentsInSolr(metadatas);
	}

	@Override
	@Transactional(isolation = Isolation.READ_UNCOMMITTED,  propagation = Propagation.REQUIRES_NEW)
	public void indexDataset(Long datasetId) throws SolrServerException, IOException {
		ShanoirMetadata shanoirMetadata = shanoirMetadataRepository.findOneSolrDoc(datasetId);
		if (shanoirMetadata == null) throw new IllegalStateException("shanoir metadata with id " +  datasetId + " query failed to return any result");
		ShanoirSolrDocument doc = getShanoirSolrDocument(shanoirMetadata);
		doc.setTags(this.getTagsAsStrings(shanoirMetadata));
		solrJWrapper.addToIndex(doc);
	}

	private List<String> getTagsAsStrings(ShanoirMetadata metadata){

		List<String> tags = new ArrayList<>();

		// SubjectStudy tags
		List<SubjectStudy> subjectStudies = subjectStudyRepo.findByStudy_IdInAndSubjectIdIn(
				Collections.singletonList(metadata.getStudyId()),
				Collections.singletonList(metadata.getSubjectId())
		);

		for (SubjectStudy subStu : subjectStudies) {
			if (subStu.getTags() != null) {
				for (Tag tag : subStu.getTags()) {
					tags.add(tag.getName());
				}
			}
		}

		// Dataset tags
		Optional<Dataset> ds = dsRepository.findById(metadata.getDatasetId());
		if(ds.isEmpty()){
			return tags;
		}
		for(StudyTag tag : ds.get().getTags()){
			tags.add(tag.getName());
		}

		return tags;
	}

	private void indexDocumentsInSolr(List<ShanoirMetadata> metadatas) throws SolrServerException, IOException {
		Iterator<ShanoirMetadata> docIt = metadatas.iterator();

		List<ShanoirSolrDocument> solrDocuments = new ArrayList<>();

		while (docIt.hasNext()) {
			ShanoirMetadata shanoirMetadata = docIt.next();
			ShanoirSolrDocument doc = this.getShanoirSolrDocument(shanoirMetadata);
			doc.setTags(this.getTagsAsStrings(shanoirMetadata));
			solrDocuments.add(doc);
		}
		solrJWrapper.addAllToIndex(solrDocuments);
	}

	private ShanoirSolrDocument getShanoirSolrDocument(ShanoirMetadata shanoirMetadata) {
		return new ShanoirSolrDocument(String.valueOf(shanoirMetadata.getDatasetId()), shanoirMetadata.getDatasetId(), shanoirMetadata.getDatasetName(),
				shanoirMetadata.getDatasetType(), shanoirMetadata.getDatasetNature(), DateTimeUtils.localDateToDate(shanoirMetadata.getDatasetCreationDate()),
				shanoirMetadata.getExaminationId(), shanoirMetadata.getExaminationComment(), DateTimeUtils.localDateToDate(shanoirMetadata.getExaminationDate()), shanoirMetadata.getAcquisitionEquipmentName(),
				shanoirMetadata.getSubjectName(), SubjectType.getType(shanoirMetadata.getSubjectType()) != null ? SubjectType.getType(shanoirMetadata.getSubjectType()).name() : null, shanoirMetadata.getSubjectId(), shanoirMetadata.getStudyName(), shanoirMetadata.getStudyId(), shanoirMetadata.getCenterName(),
				shanoirMetadata.getCenterId(), shanoirMetadata.getSliceThickness(), shanoirMetadata.getPixelBandwidth(), shanoirMetadata.getMagneticFieldStrength(),
				shanoirMetadata.isProcessed());
	}

	@Transactional
	@Override
	public SolrResultPage<ShanoirSolrDocument> facetSearch(ShanoirSolrQuery query, Pageable pageable) throws RestServiceException {
		SolrResultPage<ShanoirSolrDocument> result;
		pageable = prepareTextFields(pageable);
		if (KeycloakUtil.getTokenRoles().contains("ROLE_ADMIN")) {
			result = solrJWrapper.findByFacetCriteriaForAdmin(query, pageable);
		} else {
			Map<Long, List<String>> studiesCenter = getStudiesCenter();
			result = solrJWrapper.findByStudyIdInAndFacetCriteria(studiesCenter, query, pageable);
		}
		return result;
	}

	private Map<Long, List<String>> getStudiesCenter() {
		List<StudyUser> studyUsers = Utils.toList(rightsRepository.findByUserId(KeycloakUtil.getTokenUserId()));
		Map<Long, List<String>> studiesCenter = new HashMap<>();
		List<Center> centers = Utils.toList(centerRepository.findAll());
		for(StudyUser su : studyUsers) {
			if (su.isConfirmed()) {
				studiesCenter.put(su.getStudyId(), su.getCenterIds().stream().map(centerId -> findCenterName(centers, centerId)).collect(Collectors.toList()));
			}
		}
		return studiesCenter;
	}
	
	private String findCenterName(List<Center> centers, Long id) {
		List<Center> filteredCenters = centers.stream().filter(centerToFilter -> centerToFilter.getId().equals(id)).collect(Collectors.toList());
		return filteredCenters.size() > 0 ? filteredCenters.get(0).getName() : null;
	}

	private Pageable prepareTextFields(Pageable pageable) {
		for (Sort.Order order : pageable.getSort()) {
			if (order.getProperty().equals("studyName") || order.getProperty().equals("subjectName")
					|| order.getProperty().equals("datasetName") || order.getProperty().equals("datasetNature")
					|| order.getProperty().equals("datasetType") || order.getProperty().equals("examinationComment")
					|| order.getProperty().equals("tags") || order.getProperty().equals("subjectType") || order.getProperty().equals("acquisitionEquipmentName")
					|| order.getProperty().equals("processed")
			) {
				pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
						order.getDirection(), order.getProperty());
			} else if (order.getProperty().equals("id")) {
				pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
						order.getDirection(), "datasetId");
			}
		}
		return pageable;
	}

	@Override
	public Page<ShanoirSolrDocument> getByIdIn(List<Long> datasetIds, Pageable pageable) throws RestServiceException {
		if (datasetIds.isEmpty()) {
			return new PageImpl<>();
		}
		Page<ShanoirSolrDocument> result;
		pageable = prepareTextFields(pageable);
		if (KeycloakUtil.getTokenRoles().contains("ROLE_ADMIN")) {
			result = solrJWrapper.findByDatasetIdIn(datasetIds, pageable);
		} else {
			Map<Long, List<String>> studiesCenter = getStudiesCenter();
			result = solrJWrapper.findByStudyIdInAndDatasetIdIn(studiesCenter, datasetIds, pageable);
		}
		return result;
	}

	/**
	 * Updates a list of datasets in Solr.
	 * @param datasetIds the list of dataset IDs to update
	 */
	@Override
	public void updateDatasets(List<Long> datasetIds) throws SolrServerException, IOException {
		if (CollectionUtils.isEmpty(datasetIds)) {
			return;
		}
		this.deleteFromIndex(datasetIds);
		this.indexDatasets(datasetIds);		
	}

}
