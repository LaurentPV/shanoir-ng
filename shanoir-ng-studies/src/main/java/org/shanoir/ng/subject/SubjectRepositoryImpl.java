package org.shanoir.ng.subject;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.shanoir.ng.shared.model.ItemRepositoryCustom;
import org.springframework.stereotype.Repository;

/**
 * Implementation of custom repository for templates.
 * 
 * @author msimon
 *
 */
@Repository
public class SubjectRepositoryImpl implements ItemRepositoryCustom<Subject> {

	@PersistenceContext
    private EntityManager em;

	@SuppressWarnings("unchecked")
	@Override
	public List<Subject> findBy(String fieldName, Object value) {
		return em.createQuery(
				"SELECT s FROM Subject s WHERE s." + fieldName + " LIKE :value")
				.setParameter("value", value)
				.getResultList();
	}

}
