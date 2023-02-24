package org.shanoir.ng.challenge;

import java.util.List;

import org.shanoir.ng.shared.core.model.IdName;
import org.shanoir.ng.shared.exception.ErrorModel;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-03-23T10:35:29.288Z")

@Tag(name = "challenges", description = "the challenge API")
@RequestMapping("/challenges")
public interface ChallengeApi {

	
	@ApiOperation(value = "", notes = "Returns id and name for all available challenges", response = IdName.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "found challenges", response = IdName.class, responseContainer = "List"),
			@ApiResponse(code = 204, message = "no challenges found", response = Void.class),
			@ApiResponse(code = 401, message = "unauthorized", response = Void.class),
			@ApiResponse(code = 403, message = "forbidden", response = Void.class),
			@ApiResponse(code = 500, message = "unexpected error", response = ErrorModel.class) })
	@RequestMapping(value = "", produces = { "application/json" }, method = RequestMethod.GET)
	ResponseEntity<List<IdName>> findChallenges() throws RestServiceException;
}
