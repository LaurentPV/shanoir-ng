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
package org.shanoir.ng.shared.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.shanoir.ng.dataset.model.Dataset;
import org.shanoir.ng.shared.core.model.IdName;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author yyao
 *
 */
@Entity
@Table(name = "study")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Study extends IdName {

	@Id
	protected Long id;
	
	protected String name;
	
	@ManyToMany
	@JoinTable(name = "related_datasets", joinColumns = @JoinColumn(name = "study_id"), inverseJoinColumns = @JoinColumn(name = "dataset_id"))
	private List<Dataset> relatedDatasets;
	
	/**
	 * Linked tags.
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
	List<Tag> tags;

	/** Relations between the subjects and the studies. */
	@OneToMany(mappedBy = "study", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SubjectStudy> subjectStudyList;

	/**
	 * @return the tags
	 */
	public List<Tag> getTags() {
		return tags;
	}

	/**
	 * @param tags the tags to set
	 */
	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	/**
	 * @return the relatedDatasets
	 */
	public List<Dataset> getRelatedDatasets() {
		return relatedDatasets;
	}

	/**
	 * @param relatedDatasets the relatedDatasets to set
	 */
	public void setRelatedDatasets(List<Dataset> relatedDatasets) {
		this.relatedDatasets = relatedDatasets;
	}

	public Study() {}

	/**
	 * @param id
	 * @param name
	 */
	public Study(Long id, String name) {
		this.setId(id);
		this.setName(name);
	}

	/**
	 * @return the subjectStudyList
	 */
	public List<SubjectStudy> getSubjectStudyList() {
		return subjectStudyList;
	}

	/**
	 * @param subjectStudyList the subjectStudyList to set
	 */
	public void setSubjectStudyList(List<SubjectStudy> subjectStudyList) {
		this.subjectStudyList = subjectStudyList;
	}
	
	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
	
}
