/***************************************************************************

 Copyright (c) 2016, EPAM SYSTEMS INC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 ****************************************************************************/

package com.epam.dlab.backendapi.core.commands;

import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.process.builder.ProcessInfoBuilder;
import com.epam.dlab.process.model.ProcessData;
import com.epam.dlab.process.model.ProcessInfo;
import com.epam.dlab.process.model.ProcessType;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CommandExecutorMock implements ICommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutorMock.class);
	private static final String DOCKER_DLAB_DATAENGINE = "docker.dlab-dataengine:latest";
	private static final String DOCKER_DLAB_DATAENGINE_SERVICE = "docker.dlab-dataengine-service:latest";

    private CommandExecutorMockAsync execAsync = null;
    private CompletableFuture<Boolean> future;

    private CloudProvider cloudProvider;

    public CommandExecutorMock(CloudProvider cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    /**
     * Return result of execution.
     *
	 * @throws ExecutionException can be thrown
	 * @throws InterruptedException can be thrown
     */
	boolean getResultSync() throws InterruptedException, ExecutionException {
        return (future == null ? true : future.get());
    }

    /**
     * Return variables for substitution into Json response file.
     */
	Map<String, String> getVariables() {
        return (execAsync == null ? null : execAsync.getParser().getVariables());
    }

    /**
     * Response file name.
     */
	String getResponseFileName() {
        return (execAsync == null ? null : execAsync.getResponseFileName());
    }

    @Override
	public ProcessInfo startSync(String user, String uuid, ProcessType processType, String processDescription,
								 String command) {
        LOGGER.debug("Run OS command for user {} with UUID {}: {}", user, uuid, command);
		ProcessInfoBuilder builder =
				new ProcessInfoBuilder(new ProcessData(user, uuid, processType, processDescription), 1000L);
        if (command.startsWith("docker images |")) {
            List<String> list = Lists.newArrayList(
                    "docker.dlab-deeplearning:latest",
                    "docker.dlab-jupyter:latest",
                    "docker.dlab-rstudio:latest",
                    "docker.dlab-tensor:latest",
                    "docker.dlab-zeppelin:latest",
                    "docker.dlab-tensor-rstudio:latest");

            list.addAll(getComputationalDockerImage());

            ProcessInfoBuilder.stdOut(builder, String.join("\n", list));
        }
        return builder.get();
    }

    @Override
	public void startAsync(String user, String uuid, ProcessType processType,
						   String processDescription, String command) {
		execAsync = new CommandExecutorMockAsync(user, uuid, processType, processDescription, command, cloudProvider);
        future = CompletableFuture.supplyAsync(execAsync);
    }

	@Override
	public Boolean cancelSync(String username, String uuid) {
		return false;
	}

	@Override
	public void cancelAsync(String username, String uuid) {
	}

	@Override
	public List<ProcessInfo> getProcessInfo(String username) {
		return Collections.emptyList();
	}

	private List<String> getComputationalDockerImage() {
        switch (cloudProvider) {
            case AWS:
                return Lists.newArrayList(DOCKER_DLAB_DATAENGINE_SERVICE, DOCKER_DLAB_DATAENGINE);
            case AZURE:
                return Lists.newArrayList(DOCKER_DLAB_DATAENGINE);
            case GCP:
                return Lists.newArrayList(DOCKER_DLAB_DATAENGINE_SERVICE, DOCKER_DLAB_DATAENGINE);
            default:
                throw new IllegalArgumentException("Unsupported cloud provider " + cloudProvider);
        }
    }

}
