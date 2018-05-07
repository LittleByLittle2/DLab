#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

import boto3
from botocore.client import Config
from fabric.api import *
import argparse
import os
import sys
import time
from fabric.api import lcd
from fabric.contrib.files import exists
from fabvenv import virtualenv
from dlab.notebook_lib import *
from dlab.actions_lib import *
from dlab.fab import *
from dlab.common_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--dataproc_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--pip_mirror', type=str, default='')
parser.add_argument('--application', type=str, default='')
args = parser.parse_args()

dataproc_dir = '/opt/' + args.dataproc_version + '/jars/'
spark_dir = '/opt/' + args.dataproc_version + '/' + args.cluster_name + '/spark/'
yarn_dir = '/opt/' + args.dataproc_version + '/' + args.cluster_name + '/conf/'


def configure_rstudio():
    if not os.path.exists('/home/' + args.os_user + '/.ensure_dir/rstudio_dataengine-service_ensured'):
        try:
            local('echo "export R_LIBS_USER=' + spark_dir + '/R/lib:" >> /home/' + args.os_user + '/.bashrc')
            local("sed -i 's/^SPARK_HOME/#SPARK_HOME/' /home/" + args.os_user + "/.Renviron")
            local('echo \'SPARK_HOME="' + spark_dir + '"\' >> /home/' + args.os_user + '/.Renviron')
            local('echo \'YARN_CONF_DIR="' + yarn_dir + '"\' >> /home/' + args.os_user + '/.Renviron')
            local('echo \'HADOOP_CONF_DIR="' + yarn_dir + '"\' >> /home/' + args.os_user + '/.Renviron')
            local('echo http_proxy=$http_proxy >> /home/{}/.Renviron'.format(args.os_user))
            local('echo https_proxy=$https_proxy >> /home/{}/.Renviron'.format(args.os_user))
            local("sed -i 's/^master/#master/' /home/" + args.os_user + "/.Rprofile")
            local('''R -e "source('/home/{}/.Rprofile')"'''.format(args.os_user))
            local('touch /home/' + args.os_user + '/.ensure_dir/rstudio_dataengine-service_ensured')
        except:
            sys.exit(1)
    else:
        try:
            local("sed -i '/R_LIBS_USER/ { s|=\(.*\)|=\\1" + spark_dir + "/R/lib:| }' /home/" + args.os_user + "/.bashrc")
            local("sed -i 's/^SPARK_HOME/#SPARK_HOME/' /home/" + args.os_user + "/.Renviron")
            local("sed -i 's/^YARN_CONF_DIR/#YARN_CONF_DIR/' /home/" + args.os_user + "/.Renviron")
            local("sed -i 's/^HADOOP_CONF_DIR/#HADOOP_CONF_DIR/' /home/" + args.os_user + "/.Renviron")
            local("sed -i 's/^master/#master/' /home/" + args.os_user + "/.Rprofile")
            local('echo \'SPARK_HOME="' + spark_dir + '"\' >> /home/' + args.os_user + '/.Renviron')
            local('echo \'YARN_CONF_DIR="' + yarn_dir + '"\' >> /home/' + args.os_user + '/.Renviron')
            local('echo \'HADOOP_CONF_DIR="' + yarn_dir + '"\' >> /home/' + args.os_user + '/.Renviron')
            local('echo http_proxy=$http_proxy >> /home/{}/.Renviron'.format(args.os_user))
            local('echo https_proxy=$https_proxy >> /home/{}/.Renviron'.format(args.os_user))
            local('''R -e "source('/home/{}/.Rprofile')"'''.format(args.os_user))
        except Exception as err:
            print('Error:', str(err))
            sys.exit(1)


if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        result = prepare(dataproc_dir, yarn_dir)
        if result == False :
            actions_lib.GCPActions().jars(args, dataproc_dir)
        actions_lib.GCPActions().yarn(args, yarn_dir)
        actions_lib.GCPActions().install_dataproc_spark(args)
        actions_lib.GCPActions().spark_defaults(args)
        configuring_notebook(args.dataproc_version)
        configure_rstudio()
