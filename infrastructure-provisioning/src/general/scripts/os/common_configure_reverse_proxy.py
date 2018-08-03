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

import argparse
import json
import sys
from fabric.api import *
from jinja2 import Environment, FileSystemLoader

parser = argparse.ArgumentParser()
parser.add_argument('--edge_hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--type', type=str, default='')
parser.add_argument('--exploratory_name', type=str, default='')
parser.add_argument('--additional_info', type=str, default='')
args = parser.parse_args()

def make_template():
    conf_file_name = args.exploratory_name
    additional_info = json.loads(args.additional_info)
    environment = Environment(loader=FileSystemLoader('/root/locations'), trim_blocks=True,
                              lstrip_blocks=True)
    template = environment.get_template('{}.conf'.format(args.type))
    ungit_template = environment.get_template('{}.conf'.format('ungit'))
    tf_template = environment.get_template('{}.conf'.format('tensor'))
    config = {}
    if args.type != 'emr' and args.type != 'spark':
        config['NAME'] = args.exploratory_name
        config['IP'] = additional_info['instance_hostname']
    elif args.type == 'spark' or args.type == 'emr':
        config['CLUSTER_NAME'] = '{}_{}'.format(
            args.exploratory_name, additional_info['computational_name'])
        config['MASTER_IP'] = additional_info['master_ip']
        config['MASTER_DNS'] = additional_info['master_dns']
        config['NOTEBOOK_IP'] = additional_info['notebook_instance_ip']
        config['slaves'] = additional_info['slaves']
        conf_file_name = config['CLUSTER_NAME']


    # Render the template with data and print the output
    f = open('/tmp/{}.conf'.format(conf_file_name), 'w')
    f.write(template.render(config))
    f.close()
    if args.type != 'emr' and args.type != 'spark':
        f = open('/tmp/{}.conf'.format(conf_file_name), 'a')
        f.write(ungit_template.render(config))
        f.close()
    if additional_info['tensor']:
        f = open('/tmp/{}.conf'.format(conf_file_name), 'a')
        f.write(tf_template.render(config))
        f.close()
    return conf_file_name

##############
# Run script #
##############
if __name__ == "__main__":
    print("Make template")

    try:
        conf_file_name = make_template()
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)

    print("Configure connections")
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.edge_hostname
    put('/tmp/{}.conf'.format(conf_file_name), '/etc/nginx/locations', use_sudo=True)
    sudo('service nginx restart')



