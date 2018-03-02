/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
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


package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.SchedulerJobService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Manages scheduler jobs for exploratory environment
 */
@Path("/infrastructure_provision/exploratory_environment/scheduler")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class SchedulerJobResource {

	@Inject
	private SchedulerJobService schedulerJobService;


	/**
	 * Updates exploratory <code>exploratoryName<code/> for user <code>userInfo<code/> with new scheduler job data
	 *
	 * @param userInfo        user info
	 * @param exploratoryName name of exploratory resource
	 * @param dto             scheduler job data
	 * @return response
	 */
	@POST
	@Path("/{exploratoryName}")
	public Response create(@Auth UserInfo userInfo, @PathParam("exploratoryName") String exploratoryName,
						   SchedulerJobDTO dto) {
		log.debug("Updating exploratory {} for user {} with new scheduler job data {}...",
				exploratoryName, userInfo.getName(), dto);
		schedulerJobService.updateSchedulerDataForUserAndExploratory(userInfo.getName(), exploratoryName, dto);
		return Response.ok().build();
	}

	/**
	 * Returns scheduler job for dlab resource <code>exploratoryName<code/>
	 *
	 * @param userInfo        user info
	 * @param exploratoryName name of exploratory resource
	 * @return scheduler job data
	 */
	@GET
	@Path("/{exploratoryName}")
	public Response fetchSchedulerJobForUserAndExploratory(@Auth UserInfo userInfo,
														   @PathParam("exploratoryName") String
																   exploratoryName) {
		log.debug("Loading scheduler job for user {} and exploratory {}...", userInfo.getName(), exploratoryName);
		final SchedulerJobDTO schedulerJob = schedulerJobService
				.fetchSchedulerJobForUserAndExploratory(userInfo.getName(), exploratoryName);
		return Response.ok(schedulerJob).build();
	}

}

