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

package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.handlers.ComputationalCallbackHandler;
import com.epam.dlab.backendapi.core.response.handlers.ComputationalConfigure;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.computational.ComputationalStartDTO;
import com.epam.dlab.dto.computational.ComputationalStopDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.process.model.ProcessType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.EnumMap;
import java.util.Map;

import static com.epam.dlab.backendapi.core.commands.DockerAction.*;

@Singleton
public class SparkClusterService extends DockerService implements DockerCommands {

	private static final DataEngineType SPARK_ENGINE = DataEngineType.SPARK_STANDALONE;
	private static Map<DockerAction, ProcessType> processTypeMap = new EnumMap<>(DockerAction.class);

	@Inject
	private ComputationalConfigure computationalConfigure;

	static {
		processTypeMap.put(DockerAction.CREATE, ProcessType.SPARK_CLUSTER_CREATE);
		processTypeMap.put(DockerAction.START, ProcessType.SPARK_CLUSTER_START);
		processTypeMap.put(DockerAction.STOP, ProcessType.SPARK_CLUSTER_STOP);
		processTypeMap.put(DockerAction.TERMINATE, ProcessType.SPARK_CLUSTER_TERMINATE);
	}

	public String create(UserInfo ui, ComputationalBase<?> dto) {
		return action(ui, dto, CREATE);
	}

	public String terminate(UserInfo ui, ComputationalTerminateDTO dto) {
		return action(ui, dto, TERMINATE);
	}

	public String stop(UserInfo ui, ComputationalStopDTO dto) {
		return action(ui, dto, STOP);
	}

	public String start(UserInfo ui, ComputationalStartDTO dto) {
		return action(ui, dto, START);
	}

	private String action(UserInfo ui, ComputationalBase<?> dto, DockerAction action) {
		String uuid = DockerCommands.generateUUID();
		folderListenerExecutor.start(configuration.getImagesDirectory(),
				configuration.getResourceStatusPollTimeout(),
				getFileHandlerCallback(action, uuid, dto));
		try {
			final RunDockerCommand dockerCommand = new RunDockerCommand()
					.withInteractive()
					.withName(nameContainer(dto.getEdgeUserName(), action, dto.getExploratoryName(),
							dto.getComputationalName()))
					.withVolumeForRootKeys(configuration.getKeyDirectory())
					.withVolumeForResponse(configuration.getImagesDirectory())
					.withVolumeForLog(configuration.getDockerLogDirectory(), SPARK_ENGINE.getName())
					.withResource(SPARK_ENGINE.getName())
					.withRequestId(uuid)
					.withConfKeyName(configuration.getAdminKey())
					.withImage(DataEngineType.getDockerImageName(SPARK_ENGINE))
					.withAction(action);

			final String processDescription = String.format("Cluster %s affiliated with exploratory %s",
					dto.getComputationalName(), dto.getExploratoryName());
			commandExecutor.startAsync(ui.getName(), uuid, processTypeMap.get(action), processDescription,
					commandBuilder.buildCommand(dockerCommand, dto));
		} catch (JsonProcessingException e) {
			throw new DlabException("Could not" + action.toString() + "computational resources cluster", e);
		}

		return uuid;
	}

	private FileHandlerCallback getFileHandlerCallback(DockerAction action, String uuid, ComputationalBase<?> dto) {
		return new ComputationalCallbackHandler(computationalConfigure, selfService, action, uuid, dto);
	}

	private String nameContainer(String user, DockerAction action, String exploratoryName, String name) {
		return nameContainer(user, action.toString(), "computational", exploratoryName, name);
	}

	@Override
	public String getResourceType() {
		return Directories.DATA_ENGINE_LOG_DIRECTORY;
	}

}
