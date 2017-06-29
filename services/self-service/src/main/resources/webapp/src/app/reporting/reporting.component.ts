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

import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';

import { BillingReportService }  from './../core/services';
import { ReportingConfigModel }  from './reporting-data.model';
import { ReportingGridComponent } from './reporting-grid/reporting-grid.component';
import { ToolbarComponent } from './toolbar/toolbar.component';

import * as moment from 'moment';

@Component({
  selector: 'dlab-reporting',
  template: `
  <dlab-navbar></dlab-navbar>
  <dlab-toolbar (rebuildReport)="rebuildBillingReport($event)" (setRangeOption)="setRangeOption($event)"></dlab-toolbar>
  <dlab-reporting-grid (filterReport)="filterReport($event)" (resetRangePicker)="resetRangePicker($event)"></dlab-reporting-grid>
  <footer>
    Total {{data?.cost_total}} {{data?.currency_code}}
  </footer>
  `,
  styles: [`
    footer {
      position: fixed;
      left: 0px;
      bottom: 0px;
      width: 100%;
      background: #a1b7d1;
      color: #ffffff;
      text-align: right;
      padding: 5px 15px;
      font-size: 18px;
      box-shadow: 0 9px 18px 15px #f5f5f5;
    }
  `]
})
export class ReportingComponent implements OnInit, OnDestroy {

  @ViewChild(ReportingGridComponent) reportingGrid: ReportingGridComponent;
  @ViewChild(ToolbarComponent) reportingToolbar: ToolbarComponent;

  reportData: ReportingConfigModel = ReportingConfigModel.getDefault();
  filterConfiguration: ReportingConfigModel = ReportingConfigModel.getDefault();
  data: any;

  constructor(private billingReportService: BillingReportService) { }

  ngOnInit() {
    this.rebuildBillingReport();
  }

  ngOnDestroy() {
    this.clearStorage();
  }

  getGeneralBillingData() {
    localStorage.removeItem('report_config');

    this.billingReportService.getGeneralBillingData(this.reportData)
      .subscribe(data => {
        this.data = data;
        this.reportingGrid.reportData = this.data.lines;
        this.reportingGrid.full_report = this.data.full_report;

        this.reportingToolbar.reportData = this.data;
        if (!localStorage.getItem('report_period')) {
          localStorage.setItem('report_period' , JSON.stringify({start_date: this.data.usage_date_start, end_date: this.data.usage_date_end}));
          this.reportingToolbar.setDateRange();
        }

        if (localStorage.getItem('report_config')) {
          this.filterConfiguration = JSON.parse(localStorage.getItem('report_config'));
          this.reportingGrid.setConfiguration(this.filterConfiguration);
        } else {
          this.getDefaultFilterConfiguration(this.data);
        }
     });
  }

  rebuildBillingReport($event?): void {
    this.clearStorage();
    this.resetRangePicker();
    this.reportData.defaultConfigurations();

    this.getGeneralBillingData();
  }

  getDefaultFilterConfiguration(data): void {
    const users = [], types = [], shapes = [], services = [];

    data.lines.forEach((item: any) => {
      if (item.user && users.indexOf(item.user) === -1)
        users.push(item.user);

      if (item.dlab_resource_type && types.indexOf(item.dlab_resource_type) === -1)
        types.push(item.dlab_resource_type);

      if (item.shape) {
        if (item.shape.indexOf('Master') > -1) {
          for (let shape of item.shape.split('\n')) {
              shape = shape.replace('Master: ', '');
              shape = shape.replace(/Slave:\s+\d+x/, '');
              shape = shape.replace(/\s+/g, '');

              shapes.indexOf(shape) === -1 && shapes.push(shape);
          }
        } else {
          shapes.indexOf(item.shape) === -1 && shapes.push(item.shape);
        }
      }

      if (item.product && services.indexOf(item.product) === -1)
        services.push(item.product);
    });

    this.filterConfiguration = new ReportingConfigModel(users, services, types, shapes, '', '', '');
    this.reportingGrid.setConfiguration(this.filterConfiguration);
    localStorage.setItem('report_config' , JSON.stringify(this.filterConfiguration));
  }

  filterReport(event: ReportingConfigModel): void {
    this.reportData = event;
    this.getGeneralBillingData();
  }

  resetRangePicker() {
    this.reportingToolbar.clearRangePicker();
  }

  clearStorage(): void {
    localStorage.removeItem('report_config');
    localStorage.removeItem('report_period');
  }

  setRangeOption(dateRangeOption: any): void {
    this.reportData.date_start = dateRangeOption.start_date;
    this.reportData.date_end = dateRangeOption.end_date;
    this.getGeneralBillingData();
  }
}
