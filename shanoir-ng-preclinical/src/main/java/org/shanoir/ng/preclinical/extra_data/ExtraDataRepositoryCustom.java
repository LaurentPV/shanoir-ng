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

package org.shanoir.ng.preclinical.extra_data;

import java.util.List;

/**
 * Custom repository for extra data.
 * 
 * @author sloury
 *
 */
public interface ExtraDataRepositoryCustom<T> {
	
	 public List<T> findAllByExaminationId(Long id);
	  
	 public List<T> findBy(String fieldName, Object value);
		

}