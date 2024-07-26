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
import org.shanoir.ng.importer.dto.Patient;

/**
 * Mapper for Patients.
 * Used by ShanoirUploader.
 * 
 * @author lvallet
 *
 */
@Mapper(componentModel = "spring")
public interface PatientMapper {
	
	/**
	 * Map a @Patient from import ms to a @Patient DTO object from datasets ms.	
	 * @param patient
	 * @return
	**/
	Patient patientToPatientDTO(org.shanoir.ng.importer.model.Patient patient);

}