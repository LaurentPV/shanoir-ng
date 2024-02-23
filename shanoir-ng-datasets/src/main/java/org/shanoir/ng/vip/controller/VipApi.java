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

package org.shanoir.ng.vip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.shanoir.ng.shared.core.model.IdName;
import org.shanoir.ng.shared.exception.EntityNotFoundException;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.shanoir.ng.shared.exception.SecurityException;
import org.shanoir.ng.vip.dto.VipExecutionDTO;
import org.shanoir.ng.vip.monitoring.model.ExecutionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * @author Alae Es-saki
 */
@Tag(name = "toto")
@RequestMapping("/toto")
public interface VipApi {

    @Operation(summary = "Creates an execution inside VIP and return the monitoring id and name", description = "Creates the ressources and path control in shanoir before creating an execution in VIP, then return the running execution monitoring", tags={  })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Execution successfully created response."),
            @ApiResponse(responseCode = "403", description = "forbidden"),
            @ApiResponse(responseCode = "500", description = "unexpected error"),
            @ApiResponse(responseCode = "503", description = "error from VIP API")})
    @RequestMapping(value = "/execution/",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.POST)
    ResponseEntity<IdName> createExecution(
            @Parameter(name = "execution", required = true) @RequestBody final String execution) throws EntityNotFoundException, SecurityException;

    @Operation(summary = "Get VIP execution for the given identifier", description = "Returns the VIP execution that has the given identifier in parameter.", tags={  })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful response, returns the status"),
            @ApiResponse(responseCode = "403", description = "forbidden"),
            @ApiResponse(responseCode = "500", description = "unexpected error"),
            @ApiResponse(responseCode = "503", description = "error from VIP API")})
    @RequestMapping(value = "/execution/{identifier}",
            produces = { "application/json", "application/octet-stream" },
            method = RequestMethod.GET)
    Mono<VipExecutionDTO> getExecution(@Parameter(name = "The execution identifier", required=true) @PathVariable("identifier") String identifier) throws IOException, RestServiceException, EntityNotFoundException, SecurityException;

    @Operation(summary = "Get stderr logs for the given VIP execution identifier", description = "Returns the stderr logs of the VIP execution that has the given identifier in parameter.", tags={  })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful response, returns the status"),
            @ApiResponse(responseCode = "403", description = "forbidden"),
            @ApiResponse(responseCode = "500", description = "unexpected error"),
            @ApiResponse(responseCode = "503", description = "error from VIP API")})
    @RequestMapping(value = "/execution/{identifier}/stderr",
            produces = { "application/json", "application/octet-stream" },
            method = RequestMethod.GET)
    Mono<String> getExecutionStderr(@Parameter(name = "The execution identifier", required=true) @PathVariable("identifier") String identifier) throws IOException, RestServiceException, EntityNotFoundException, SecurityException;

    @Operation(summary = "Get stdout logs for the given VIP execution identifier", description = "Returns the stdout logs of the VIP execution that has the given identifier in parameter.", tags={  })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful response, returns the status"),
            @ApiResponse(responseCode = "403", description = "forbidden"),
            @ApiResponse(responseCode = "500", description = "unexpected error"),
            @ApiResponse(responseCode = "503", description = "error from VIP API")})
    @RequestMapping(value = "/execution/{identifier}/stdout",
            produces = { "application/json", "application/octet-stream" },
            method = RequestMethod.GET)
    Mono<String> getExecutionStdout(@Parameter(name = "The execution identifier", required=true) @PathVariable("identifier") String identifier) throws IOException, RestServiceException, EntityNotFoundException, SecurityException;


    @Operation(summary = "Get status for the given VIP execution identifier", description = "Returns the status of the VIP execution that has the given identifier in parameter.", tags={  })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful response, returns the status"),
            @ApiResponse(responseCode = "403", description = "forbidden"),
            @ApiResponse(responseCode = "500", description = "unexpected error"),
            @ApiResponse(responseCode = "503", description = "error from VIP API")})
    @RequestMapping(value = "/execution/{identifier}/status",
            produces = { "application/json", "application/octet-stream" },
            method = RequestMethod.GET)
    Mono<ExecutionStatus> getExecutionStatus(@Parameter(name = "The execution identifier", required=true) @PathVariable("identifier") String identifier) throws IOException, RestServiceException, EntityNotFoundException, SecurityException;

    @Operation(summary = "Get all available pipelines in VIP", description = "Returns all the pipelines available to the authenticated user in VIP", tags={  })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful response, returns the status"),
            @ApiResponse(responseCode = "403", description = "forbidden"),
            @ApiResponse(responseCode = "500", description = "unexpected error"),
            @ApiResponse(responseCode = "503", description = "error from VIP API")})
    @RequestMapping(value = "/pipelines",
            produces = { "application/json", "application/octet-stream" },
            method = RequestMethod.GET)
    Mono<String> getPipelineAll() throws SecurityException;

    @Operation(summary = "Get the description of pipeline [name] in the version [version]", description = "Returns the VIp description of the pipeline [name] in the version [version].", tags={  })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful response, returns the status"),
            @ApiResponse(responseCode = "403", description = "forbidden"),
            @ApiResponse(responseCode = "500", description = "unexpected error"),
            @ApiResponse(responseCode = "503", description = "error from VIP API")})
    @RequestMapping(value = "/pipeline/{identifier}",
            produces = { "application/json", "application/octet-stream" },
            method = RequestMethod.GET)
    Mono<String> getPipeline(@Parameter(name = "The pipeline identifier", required=true) @PathVariable("identifier") String identifier) throws SecurityException;


}
