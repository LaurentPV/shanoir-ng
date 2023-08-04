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

package org.shanoir.ng.studycard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.shanoir.ng.studycard.model.StudyCard;
import org.shanoir.ng.studycard.repository.StudyCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for repository 'StudyCard'.
 * 
 * @author msimon
 *
 */

@DataJpaTest
@ActiveProfiles("test")
public class StudyCardRepositoryTest {

	private static final String STUDYCARD_TEST_1_DATA = "StudyCard1";
	private static final Long STUDYCARD_TEST_1_ID = 1L;
	private static final Long STUDY_TEST_1_ID = 1L;
	
	@Autowired
	private StudyCardRepository studyCardRepository;
	
	@Test
	public void findAllTest() throws Exception {
		Iterable<StudyCard> studyCardDb = studyCardRepository.findAll();
		assertThat(studyCardDb).isNotNull();
		int nbStudyCard = 0;
		Iterator<StudyCard> studyCardIt = studyCardDb.iterator();
		while (studyCardIt.hasNext()) {
			studyCardIt.next();
			nbStudyCard++;
		}
		assertThat(nbStudyCard).isEqualTo(5);
	}
	
	@Test
	public void findByStudyIdInTest() throws Exception {
		List<StudyCard> studyCards = studyCardRepository.findByStudyIdIn(Arrays.asList(STUDY_TEST_1_ID));
		assertNotNull(studyCards);
		assertTrue(studyCards.size() == 3);
	}
	
	@Test
	public void findOneTest() throws Exception {
		StudyCard studyCardDb = studyCardRepository.findById(STUDYCARD_TEST_1_ID).orElse(null);
		assertThat(studyCardDb.getName()).isEqualTo(STUDYCARD_TEST_1_DATA);
	}
	
}
