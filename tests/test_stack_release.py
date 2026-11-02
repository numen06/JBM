import copy
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parents[1] / 'tools'))
from check_stack_release import validate


def test_missing_stack_service_and_wrong_image_cannot_pass():
    image='registry/iot:py-2.0-20261102.4@sha256:'+'a'*64
    target={'service':'iot_api','stack':'iot','image':image,'version':'py-2.0-20261102.4','sourceCommit':'b'*40,'mainline':'py-2.0'}
    manifest={'scopeStacks':['iot'],'services':[target]}
    service={'Spec':{'Name':'iot_api','Labels':{'com.docker.stack.namespace':'iot','com.docker.stack.image':image,
        'io.feige.release.version':target['version'],'io.feige.release.revision':target['sourceCommit'],'io.feige.release.branch':'py-2.0'},
        'TaskTemplate':{'ContainerSpec':{'Image':image}}},'UpdateStatus':{'State':'completed'}}
    assert validate([service],manifest)['passed']
    assert not validate([],manifest)['passed']
    changed=copy.deepcopy(service);changed['Spec']['Labels'].pop('com.docker.stack.namespace')
    assert not validate([changed],manifest)['passed']
    changed=copy.deepcopy(service);changed['Spec']['TaskTemplate']['ContainerSpec']['Image']='registry/iot:latest'
    assert not validate([changed],manifest)['passed']
    changed=copy.deepcopy(service);changed['Spec']['Name']='iot_unmanaged'
    assert not validate([service,changed],manifest)['passed']


def test_singleton_handover_and_worker_probe_cannot_regress():
    image = 'registry/job:py-7.3-20261102.4@sha256:' + 'a'*64
    target = {'service':'jbm7_job','stack':'jbm7','image':image,'version':'py-7.3-20261102.4',
              'sourceCommit':'b'*40,'mainline':'py-7.3','replicas':1,
              'deployment':{'singleActiveWorker':True},'healthcheck':{'Test':['NONE']}}
    manifest = {'scopeStacks':['jbm7'],'services':[target]}
    service = {'Spec':{'Name':target['service'],'Labels':{'com.docker.stack.namespace':'jbm7',
        'com.docker.stack.image':image,'io.feige.release.version':target['version'],
        'io.feige.release.revision':target['sourceCommit'],'io.feige.release.branch':'py-7.3'},
        'Mode':{'Replicated':{'Replicas':1}},'UpdateConfig':{'Order':'stop-first'},
        'RollbackConfig':{'Order':'stop-first'},'TaskTemplate':{'ContainerSpec':{'Image':image,'Healthcheck':{'Test':['NONE']}}}}}
    assert validate([service],manifest)['passed']
    changed=copy.deepcopy(service);changed['Spec']['UpdateConfig']['Order']='start-first'
    assert validate([changed],manifest)['passed']
    changed['Spec']['Mode']['Replicated']['Replicas']=2
    assert not validate([changed],manifest)['passed']
    changed=copy.deepcopy(service);changed['Spec']['TaskTemplate']['ContainerSpec']['Healthcheck']={'Test':['CMD','curl','localhost']}
    assert not validate([changed],manifest)['passed']
    assert not validate([service],{**manifest,'services':[target,target]})['passed']
