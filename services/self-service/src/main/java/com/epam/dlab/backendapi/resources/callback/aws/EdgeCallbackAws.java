/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.resources.callback.aws;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.callback.base.EdgeCallback;
import com.epam.dlab.dto.aws.edge.EdgeInfoAws;
import com.epam.dlab.dto.base.keyload.UploadFileResult;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/infrastructure/edge")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class EdgeCallbackAws extends EdgeCallback {

	@Inject
	private RequestId requestId;

    public EdgeCallbackAws() {
        log.info("{} is initialized", getClass().getSimpleName());
    }

    /**
     * Stores the result of the upload the user key.
     *
     * @param dto result of the upload the user key.
     * @return 200 OK
     */
    @POST
    @Path(ApiCallbacks.STATUS_URI)
    public Response status(UploadFileResult<EdgeInfoAws> dto) {
		requestId.checkAndRemove(dto.getRequestId());
        handleEdgeCallback(dto.getUser(), dto.getStatus());
        return Response.ok().build();
    }
}
