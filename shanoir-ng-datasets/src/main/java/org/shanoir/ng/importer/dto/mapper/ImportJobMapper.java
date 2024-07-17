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

package org.shanoir.ng.importer.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.shanoir.ng.importer.dto.ImportJob;

/**
 * Mapper for ImportJobs.
 * Used by ShanoirUploader.
 * 
 * @author lvallet
 *
 */
@Mapper(componentModel = "spring", uses = { PatientMapper.class })
public interface ImportJobMapper {

	/**
	 * Map an @ImportJob model object from shanoir-ng-import to an ImportJob DTO from shanoir-ng-datasets.
	 * 
	 * @param importJob import job object from import ms.
	 * @return import job DTO from datasets ms.
	 */

	@Mapping(target = "converterId", ignore = true)
	@Mapping(target = "properties", ignore = true)
	ImportJob importJobToImportJobDTO(org.shanoir.ng.importer.model.ImportJob importJob);

}