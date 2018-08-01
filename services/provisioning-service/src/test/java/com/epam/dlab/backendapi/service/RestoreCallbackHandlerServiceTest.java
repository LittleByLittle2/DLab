/*
 * **************************************************************************
 *
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
 *
 * ***************************************************************************
 */

package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.core.response.handlers.PersistentFileHandler;
import com.epam.dlab.backendapi.core.response.handlers.dao.CallbackHandlerDao;
import io.dropwizard.util.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RestoreCallbackHandlerServiceTest {

	@Mock
	private CallbackHandlerDao dao;
	@Mock
	private FolderListenerExecutor folderListenerExecutor;
	@InjectMocks
	private RestoreCallbackHandlerService restoreCallbackHandlerService;

	@Test
	public void start() throws Exception {

		final PersistentFileHandler handler1 = new PersistentFileHandler(null, 1L, "test");
		final PersistentFileHandler handler2 = new PersistentFileHandler(null, 2L, "test1");
		when(dao.findAll()).thenReturn(Arrays.asList(handler1, handler2));

		restoreCallbackHandlerService.start();

		verify(dao).findAll();
		verify(folderListenerExecutor).start(handler1.getDirectory(),
				Duration.milliseconds(handler1.getTimeout()), handler1.getHandler());
		verify(folderListenerExecutor).start(handler2.getDirectory(),
				Duration.milliseconds(handler2.getTimeout()), handler2.getHandler());
		verifyNoMoreInteractions(dao, folderListenerExecutor);
	}
}