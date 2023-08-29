package org.shanoir.ng.processing.carmin.output;

import org.shanoir.ng.processing.carmin.model.CarminDatasetProcessing;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

/**
 * This class defines the default class to be implemented for output processing.
 * @author jcome
 *
 */
public abstract class OutputProcessing {

	/**
	 * Return true if the implementation can process the result of the given processing
	 *
	 * @param processing CarminDatasetProcessing
	 * @return
	 */
	public abstract boolean canProcess(CarminDatasetProcessing processing);

	/**
	 * This methods manages the single result of a Carmin  dataset processing
	 *
	 * @param resultFiles  the result file as tar.gz of the processing
	 * @param parentFolder the temporary arent folder in which we are currently working
	 * @param processing   the corresponding dataset processing.
	 */

	@Transactional(isolation = Isolation.READ_COMMITTED,  propagation = Propagation.REQUIRES_NEW)
	public abstract void manageTarGzResult(List<File> resultFiles, File parentFolder, CarminDatasetProcessing processing) throws OutputProcessingException;

}
