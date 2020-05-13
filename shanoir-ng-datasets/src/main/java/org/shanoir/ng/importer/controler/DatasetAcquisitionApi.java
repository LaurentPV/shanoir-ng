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
package org.shanoir.ng.importer.controler;

import javax.validation.Valid;

import org.shanoir.ng.importer.dto.EegImportJob;
import org.shanoir.ng.importer.dto.ImportJob;
import org.shanoir.ng.shared.exception.ErrorModel;
import org.shanoir.ng.shared.exception.ShanoirException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-01-09T09:20:01.478Z")

@Api(value = "datasetacquisition", description = "the datasetacquisition API")
public interface DatasetAcquisitionApi {

    @ApiOperation(value = "", notes = "Creates new dataset acquisition", response = Void.class, tags={  })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "created Dataset Acquitistion", response = Void.class),
        @ApiResponse(code = 401, message = "unauthorized", response = Void.class),
        @ApiResponse(code = 403, message = "forbidden", response = Void.class),
        @ApiResponse(code = 422, message = "bad parameters", response = ErrorModel.class),
        @ApiResponse(code = 500, message = "unexpected error", response = ErrorModel.class) })
    @RequestMapping(value = "/datasetacquisition",
        produces = { "application/json" },
        consumes = { "application/json" },
        method = RequestMethod.POST)
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('EXPERT', 'USER') and @datasetSecurityService.hasRightOnStudy(#importJob.getFrontStudyId(), 'CAN_IMPORT'))")
    ResponseEntity<Void> createNewDatasetAcquisition(@ApiParam(value = "DatasetAcquisition to create" ,required=true )  @Valid @RequestBody ImportJob importJob) throws ShanoirException;

    @ApiOperation(value = "", notes = "Creates new EEG dataset acquisition", response = Void.class, tags={  })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "created EEG Dataset Acquitistion", response = Void.class),
        @ApiResponse(code = 401, message = "unauthorized", response = Void.class),
        @ApiResponse(code = 403, message = "forbidden", response = Void.class),
        @ApiResponse(code = 422, message = "bad parameters", response = ErrorModel.class),
        @ApiResponse(code = 500, message = "unexpected error", response = ErrorModel.class) })
    @RequestMapping(value = "/datasetacquisition_eeg",
        produces = { "application/json" },
        consumes = { "application/json" },
        method = RequestMethod.POST)
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('EXPERT', 'USER') and @datasetSecurityService.hasRightOnStudy(#importJob.getFrontStudyId(), 'CAN_IMPORT'))")
    ResponseEntity<Void> createNewEegDatasetAcquisition(@ApiParam(value = "DatasetAcquisition to create" ,required=true )  @Valid @RequestBody EegImportJob importJob);

}
