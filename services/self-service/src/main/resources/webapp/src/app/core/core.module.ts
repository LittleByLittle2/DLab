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

import { NgModule, Optional, SkipSelf, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApplicationServiceFacade } from './services/applicationServiceFacade.service';
import { AppRoutingService } from './services/appRouting.service';
import { ApplicationSecurityService } from './services/applicationSecurity.service';
import { HealthStatusService } from './services/healthStatus.service';
import { UserAccessKeyService } from './services/userAccessKey.service';
import { UserResourceService } from './services/userResource.service';
import { AuthorizationGuard } from './services/authorization.guard';
import { CloudProviderGuard } from './services/cloudProvider.guard';
import { CheckParamsGuard } from './services/checkParams.guard';
import { LibrariesInstallationService } from './services/librariesInstallation.service';
import { ManageUngitService } from './services/manageUngit.service';
import { BillingReportService } from './services/billingReport.service';
import { BackupService } from './services/backup.service';

@NgModule({
  imports: [CommonModule],
  declarations: [],
  exports: [],
  providers: []
})

export class CoreModule {

  static forRoot(): ModuleWithProviders {
    return {
      ngModule: CoreModule,
      providers: [
        ApplicationSecurityService,
        AuthorizationGuard,
        CloudProviderGuard,
        CheckParamsGuard,
        UserAccessKeyService,
        AppRoutingService,
        UserResourceService,
        HealthStatusService,
        LibrariesInstallationService,
        ManageUngitService,
        BillingReportService,
        BackupService,
        ApplicationServiceFacade
      ]
    };
  }

  constructor (@Optional() @SkipSelf() parentModule: CoreModule) {
    if (parentModule)
      throw new Error('CoreModule is already loaded. Import it in the AppModule only');
  }
}
