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

import json
import time
from fabric.api import *
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import sys
import os
import uuid
import logging
from Crypto.PublicKey import RSA


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)
    try:
        os.environ['exploratory_name']
    except:
        os.environ['exploratory_name'] = ''
    if os.path.exists('/response/.dataproc_creating_{}'.format(os.environ['exploratory_name'])):
        time.sleep(30)

    print('Generating infrastructure names and tags')
    dataproc_conf = dict()
    try:
        dataproc_conf['exploratory_name'] = (os.environ['exploratory_name']).lower().replace('_', '-')
    except:
        dataproc_conf['exploratory_name'] = ''
    try:
        dataproc_conf['computational_name'] = (os.environ['computational_name']).lower().replace('_', '-')
    except:
        dataproc_conf['computational_name'] = ''
    dataproc_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    dataproc_conf['edge_user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
    dataproc_conf['key_name'] = os.environ['conf_key_name']
    dataproc_conf['key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    dataproc_conf['region'] = os.environ['gcp_region']
    dataproc_conf['zone'] = os.environ['gcp_zone']
    dataproc_conf['subnet'] = '{0}-{1}-subnet'.format(dataproc_conf['service_base_name'], dataproc_conf['edge_user_name'])
    dataproc_conf['cluster_name'] = '{0}-{1}-des-{2}-{3}'.format(dataproc_conf['service_base_name'], dataproc_conf['edge_user_name'],
                                                                 dataproc_conf['exploratory_name'], dataproc_conf['computational_name'])
    dataproc_conf['cluster_tag'] = '{0}-{1}-ps'.format(dataproc_conf['service_base_name'], dataproc_conf['edge_user_name'])
    dataproc_conf['bucket_name'] = '{}-{}-bucket'.format(dataproc_conf['service_base_name'], dataproc_conf['edge_user_name'])
    dataproc_conf['release_label'] = os.environ['dataproc_version']
    dataproc_conf['cluster_labels'] = {
        os.environ['notebook_instance_name']: "not-configured",
        "name": dataproc_conf['cluster_name'],
        "sbn": dataproc_conf['service_base_name'],
        "user": dataproc_conf['edge_user_name'],
        "notebook_name": os.environ['notebook_instance_name']
    }
    dataproc_conf['dataproc_service_account_name'] = '{0}-{1}-ps'.format(dataproc_conf['service_base_name'],
                                                                         dataproc_conf['edge_user_name'])
    service_account_email = "{}@{}.iam.gserviceaccount.com".format(dataproc_conf['dataproc_service_account_name'],
                                                                   os.environ['gcp_project_id'])
    dataproc_conf['edge_instance_hostname'] = '{0}-{1}-edge'.format(dataproc_conf['service_base_name'], dataproc_conf['edge_user_name'])
    dataproc_conf['dlab_ssh_user'] = os.environ['conf_os_user']

    edge_status = GCPMeta().get_instance_status(dataproc_conf['edge_instance_hostname'])
    if edge_status != 'RUNNING':
        logging.info('ERROR: Edge node is unavailable! Aborting...')
        print('ERROR: Edge node is unavailable! Aborting...')
        ssn_hostname = GCPMeta().get_private_ip_address(dataproc_conf['service_base_name'] + '-ssn')
        put_resource_status('edge', 'Unavailable', os.environ['ssn_dlab_path'], os.environ['conf_os_user'], ssn_hostname)
        append_result("Edge node is unavailable")
        sys.exit(1)

    print("Will create exploratory environment with edge node as access point as following: ".format(json.dumps(dataproc_conf, sort_keys=True, indent=4, separators=(',', ': '))))
    logging.info(json.dumps(dataproc_conf))

    local('touch /response/.dataproc_creating_{}'.format(os.environ['exploratory_name']))
    local("echo Waiting for changes to propagate; sleep 10")

    dataproc_cluster = json.loads(open('/root/templates/dataengine-service_cluster.json').read().decode('utf-8-sig'))
    dataproc_cluster['projectId'] = os.environ['gcp_project_id']
    dataproc_cluster['clusterName'] = dataproc_conf['cluster_name']
    dataproc_cluster['labels'] = dataproc_conf['cluster_labels']
    dataproc_cluster['config']['configBucket'] = dataproc_conf['bucket_name']
    dataproc_cluster['config']['gceClusterConfig']['serviceAccount'] = service_account_email
    dataproc_cluster['config']['gceClusterConfig']['zoneUri'] = dataproc_conf['zone']
    dataproc_cluster['config']['gceClusterConfig']['subnetworkUri'] = dataproc_conf['subnet']
    dataproc_cluster['config']['masterConfig']['machineTypeUri'] = os.environ['dataproc_master_instance_type']
    dataproc_cluster['config']['workerConfig']['machineTypeUri'] = os.environ['dataproc_slave_instance_type']
    dataproc_cluster['config']['masterConfig']['numInstances'] = int(os.environ['dataproc_master_count'])
    dataproc_cluster['config']['workerConfig']['numInstances'] = int(os.environ['dataproc_slave_count'])
    if int(os.environ['dataproc_preemptible_count']) != 0:
        dataproc_cluster['config']['secondaryWorkerConfig']['numInstances'] = int(os.environ['dataproc_preemptible_count'])
    else:
        del dataproc_cluster['config']['secondaryWorkerConfig']
    dataproc_cluster['config']['softwareConfig']['imageVersion'] = dataproc_conf['release_label']
    ssh_user_pubkey = open(os.environ['conf_key_dir'] + os.environ['edge_user_name'] + '.pub').read()
    key = RSA.importKey(open(dataproc_conf['key_path'], 'rb').read())
    ssh_admin_pubkey = key.publickey().exportKey("OpenSSH")
    dataproc_cluster['config']['gceClusterConfig']['metadata']['ssh-keys'] = '{0}:{1}\n{0}:{2}'.format(dataproc_conf['dlab_ssh_user'], ssh_user_pubkey, ssh_admin_pubkey)
    dataproc_cluster['config']['gceClusterConfig']['tags'][0] = dataproc_conf['cluster_tag']

    try:
        logging.info('[Creating Dataproc Cluster]')
        print('[Creating Dataproc Cluster]')
        params = "--region {0} --bucket {1} --params '{2}'".format(dataproc_conf['region'], dataproc_conf['bucket_name'], json.dumps(dataproc_cluster))

        try:
            local("~/scripts/{}.py {}".format('dataengine-service_create', params))
        except:
            traceback.print_exc()
            raise Exception

        keyfile_name = "/root/keys/{}.pem".format(dataproc_conf['key_name'])
        local('rm /response/.dataproc_creating_{}'.format(os.environ['exploratory_name']))
    except Exception as err:
        append_result("Failed to create Dataproc Cluster.", str(err))
        local('rm /response/.dataproc_creating_{}'.format(os.environ['exploratory_name']))
        sys.exit(1)

    sys.exit(0)