package org.shanoir.uploader.model.rest;

public class QualityCard {

	private Long id;

	/** The name of the quality card. */
	private String name;

	/** The study for which is defined the quality card. */
	private Long studyId;
	
	/** Is the quality card to be applied at import */
	private Boolean toCheckAtImport;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getStudyId() {
		return studyId;
	}

	public void setStudyId(Long studyId) {
		this.studyId = studyId;
	}

	public Boolean getToCheckAtImport() {
		return toCheckAtImport;
	}

	public void setToCheckAtImport(Boolean toCheckAtImport) {
		this.toCheckAtImport = toCheckAtImport;
	}

	public String toString() {
		return this.getName();
	}


}
