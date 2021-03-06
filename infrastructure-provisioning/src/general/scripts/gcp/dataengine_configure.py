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
import multiprocessing


def configure_slave(slave_number, data_engine):
    slave_name = data_engine['slave_node_name'] + '{}'.format(slave_number + 1)
    slave_hostname = GCPMeta().get_private_ip_address(slave_name)
    try:
        logging.info('[CREATING DLAB SSH USER ON SLAVE NODE]')
        print('[CREATING DLAB SSH USER ON SLAVE NODE]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format \
            (slave_hostname, os.environ['conf_key_dir'] + data_engine['key_name'] + ".pem", initial_user,
             data_engine['dlab_ssh_user'], sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        append_result("Failed to create ssh user on slave.", str(err))
        sys.exit(1)

    try:
        print('[INSTALLING USERs KEY ON SLAVE NODE]')
        logging.info('[INSTALLING USERs KEY ON SLAVE NODE]')
        additional_config = {"user_keyname": os.environ['edge_user_name'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            slave_hostname, os.environ['conf_key_dir'] + data_engine['key_name'] + ".pem", json.dumps(additional_config), data_engine['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            append_result("Failed installing users key")
            raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        append_result("Failed to install ssh user key on slave.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE PROXY ON SLAVE NODE]')
        print('[CONFIGURE PROXY ON ON SLAVE NODE]')
        additional_config = {"proxy_host": edge_instance_name, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}"\
            .format(slave_hostname, slave_name, keyfile_name, json.dumps(additional_config),
                    data_engine['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('common_configure_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        append_result("Failed to configure proxy on slave.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES ON SLAVE NODE]')
        print('[INSTALLING PREREQUISITES ON SLAVE NODE]')
        params = "--hostname {} --keyfile {} --user {} --region {}". \
            format(slave_hostname, keyfile_name, data_engine['dlab_ssh_user'], data_engine['region'])
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing apps: apt & pip.", str(err))
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        append_result("Failed to install prerequisites on slave.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SLAVE NODE {}]'.format(slave + 1))
        print('[CONFIGURE SLAVE NODE {}]'.format(slave + 1))
        params = "--hostname {} --keyfile {} --region {} --spark_version {} --hadoop_version {} --os_user {} --scala_version {} --r_mirror {} --master_ip {} --node_type {}". \
            format(slave_hostname, keyfile_name, data_engine['region'], os.environ['notebook_spark_version'],
                   os.environ['notebook_hadoop_version'], data_engine['dlab_ssh_user'],
                   os.environ['notebook_scala_version'], os.environ['notebook_r_mirror'], master_node_hostname,
                   'slave')
        try:
            local("~/scripts/{}.py {}".format('configure_dataengine', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed configuring slave node", str(err))
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        append_result("Failed to configure slave node.", str(err))
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)

    try:
        print('Generating infrastructure names and tags')
        data_engine = dict()
        data_engine['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
        data_engine['edge_user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
        data_engine['region'] = os.environ['gcp_region']
        data_engine['zone'] = os.environ['gcp_zone']
        try:
            if os.environ['gcp_vpc_name'] == '':
                raise KeyError
            else:
                data_engine['vpc_name'] = os.environ['gcp_vpc_name']
        except KeyError:
            data_engine['vpc_name'] = '{}-ssn-vpc'.format(data_engine['service_base_name'])
        try:
            data_engine['exploratory_name'] = (os.environ['exploratory_name']).lower().replace('_', '-')
        except:
            data_engine['exploratory_name'] = ''
        try:
            data_engine['computational_name'] = os.environ['computational_name'].lower().replace('_', '-')
        except:
            data_engine['computational_name'] = ''

        data_engine['subnet_name'] = '{0}-{1}-subnet'.format(data_engine['service_base_name'],
                                                             data_engine['edge_user_name'])
        data_engine['master_size'] = os.environ['gcp_dataengine_master_size']
        data_engine['slave_size'] = os.environ['gcp_dataengine_slave_size']
        data_engine['key_name'] = os.environ['conf_key_name']
        data_engine['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], data_engine['key_name'])
        data_engine['dataengine_service_account_name'] = '{}-{}-ps'.format(data_engine['service_base_name'],
                                                                           data_engine['edge_user_name'])

        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'
        data_engine['cluster_name'] = data_engine['service_base_name'] + '-' + data_engine['edge_user_name'] + \
                                      '-de-' + data_engine['exploratory_name'] + '-' + \
                                      data_engine['computational_name']
        data_engine['master_node_name'] = data_engine['cluster_name'] + '-m'
        data_engine['slave_node_name'] = data_engine['cluster_name'] + '-s'
        data_engine['instance_count'] = int(os.environ['dataengine_instance_count'])
        data_engine['notebook_name'] = os.environ['notebook_instance_name']
        data_engine['gpu_accelerator_type'] = 'None'
        if os.environ['application'] in ('tensor', 'deeplearning'):
            data_engine['gpu_accelerator_type'] = os.environ['gcp_gpu_accelerator_type']
        data_engine['network_tag'] = '{0}-{1}-ps'.format(data_engine['service_base_name'],
                                                         data_engine['edge_user_name'])
        master_node_hostname = GCPMeta().get_private_ip_address(data_engine['master_node_name'])
        edge_instance_name = '{0}-{1}-edge'.format(data_engine['service_base_name'],
                                                   data_engine['edge_user_name'])
        data_engine['dlab_ssh_user'] = os.environ['conf_os_user']
        keyfile_name = "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        print("Failed to generate variables dictionary.")
        append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATING DLAB SSH USER ON MASTER NODE]')
        print('[CREATING DLAB SSH USER ON MASTER NODE]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format\
            (master_node_hostname, os.environ['conf_key_dir'] + data_engine['key_name'] + ".pem", initial_user,
             data_engine['dlab_ssh_user'], sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        append_result("Failed to create ssh user on master.", str(err))
        sys.exit(1)

    try:
        print('[INSTALLING USERs KEY ON MASTER NODE]')
        logging.info('[INSTALLING USERs KEY ON MASTER NODE]')
        additional_config = {"user_keyname": os.environ['edge_user_name'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            master_node_hostname, os.environ['conf_key_dir'] + data_engine['key_name'] + ".pem", json.dumps(additional_config), data_engine['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            append_result("Failed installing users key")
            raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        append_result("Failed to install ssh user on master.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE PROXY ON MASTER NODE]')
        print('[CONFIGURE PROXY ON ON MASTER NODE]')
        additional_config = {"proxy_host": edge_instance_name, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}"\
            .format(master_node_hostname, data_engine['master_node_name'], keyfile_name, json.dumps(additional_config),
                    data_engine['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('common_configure_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        append_result("Failed to configure proxy on master.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES ON MASTER NODE]')
        print('[INSTALLING PREREQUISITES ON MASTER NODE]')
        params = "--hostname {} --keyfile {} --user {} --region {}".\
            format(master_node_hostname, keyfile_name, data_engine['dlab_ssh_user'], data_engine['region'])
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing apps: apt & pip.", str(err))
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        append_result("Failed to install prerequisites on master.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE MASTER NODE]')
        print('[CONFIGURE MASTER NODE]')
        params = "--hostname {} --keyfile {} --region {} --spark_version {} --hadoop_version {} --os_user {} --scala_version {} --r_mirror {} --master_ip {} --node_type {}".\
            format(master_node_hostname, keyfile_name, data_engine['region'], os.environ['notebook_spark_version'],
                   os.environ['notebook_hadoop_version'], data_engine['dlab_ssh_user'],
                   os.environ['notebook_scala_version'], os.environ['notebook_r_mirror'], master_node_hostname,
                   'master')
        try:
            local("~/scripts/{}.py {}".format('configure_dataengine', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to configure master node", str(err))
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        sys.exit(1)

    try:
        jobs = []
        for slave in range(data_engine['instance_count'] - 1):
            p = multiprocessing.Process(target=configure_slave, args=(slave, data_engine))
            jobs.append(p)
            p.start()
        for job in jobs:
            job.join()
        for job in jobs:
            if job.exitcode != 0:
                raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            GCPActions().remove_instance(slave_name, data_engine['zone'])
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        sys.exit(1)


    try:
        logging.info('[SUMMARY]')
        print('[SUMMARY]')
        print("Service base name: {}".format(data_engine['service_base_name']))
        print("Region: {}".format(data_engine['region']))
        print("Cluster name: {}".format(data_engine['cluster_name']))
        print("Master node shape: {}".format(data_engine['master_size']))
        print("Slave node shape: {}".format(data_engine['slave_size']))
        print("Instance count: {}".format(str(data_engine['instance_count'])))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": data_engine['cluster_name'],
                   "instance_id": data_engine['master_node_name'],
                   "key_name": data_engine['key_name'],
                   "Action": "Create new Data Engine"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)