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

package org.shanoir.ng.preclinical.anesthetics.ingredients;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;


@Component
public class AnestheticIngredientRepositoryImpl implements AnestheticIngredientRepositoryCustom{

	@PersistenceContext
    private EntityManager em;
		
	@SuppressWarnings("unchecked")
	@Override
	public List<AnestheticIngredient> findBy(String fieldName, Object value) {
		return em.createQuery(
				"SELECT i FROM AnestheticIngredient i WHERE i." + fieldName + " LIKE :value")
				.setParameter("value", value)
				.getResultList();
	} 
	

}
