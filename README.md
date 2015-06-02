<a id="top"></a>
# Baragon [![Build Status](https://travis-ci.org/HubSpot/Baragon.svg?branch=master)](https://travis-ci.org/HubSpot/Baragon)

![Behold the mighty Baragon's roar](http://i.imgur.com/mCbkbcZ.jpg)

Baragon is a system for automating load balancer configuration updates. It pairs well with the [Singularity](https://github.com/HubSpot/Singularity) Mesos framework.

## Contents

- [Baragon Basics](#basics)
- [Getting Started](#start)
  - [Quick Start](#start)
  - [Detailed Setup Guide](docs/managing_baragon.md)
  - [Example Baragon Service Configuration](docs/baragon_service_config.yaml)
  - [Example Baragon Agent Configuration](docs/baragon_agent_config.yaml)
- [Quick Start With Docker Compose](#docker)
- [Quick Start With Vagrant](#vagrant)
- [BaragonUI](#ui)
- [API Docs](docs/baragon_api_docs.md)

<a id="basics"></a>
## Baragon Basics

Baragon is made up of two services:

- BaragonService -- coordination service

- BaragonAgentService -- applies changes on the actual load balancer

When a web service changes (i.e. upstreams added / removed), POST a [BaragonRequest](docs/baragon_api_docs.md#requests) JSON object to BaragonService's `/[contextPath]/request` endpoint like this one:

```json
{
  "loadBalancerRequestId": "4",
  "loadBalancerService": {
    "serviceId": "1",
    "owners": ["foo"],
    "serviceBasePath": "/basepath",
    "loadBalancerGroups": ["loadBalancerGroupName"]
  },
  "addUpstreams": ["1.1.1.1:80"],
  "removeUpstreams": []
}
```

- `BaragonService` will fan out the update to all `BaragonAgent`s in the specified `loadBalancerGroups`
- `BaragonAgent`s will apply the changes on the load balancer using templates provided in its configuration and report back a Success or Failure to `BaragonService`
- Polling the `BaragonService` request status url (`/[contextPath]/request/{loadBalancerRequestId}`) will indicate the current status of the request

Check out the [API Docs](docs/baragon_api_docs.md) for additional `BaragonRequest` fields and returned values.

<a id="start"></a>
## Getting Started

For more details on configuring and using Baragon, check out the [detailed setup and management guide](docs/managing_baragon.md)

** Prerequisite: A working ZooKeeper cluster **

1. Build JARs via `mvn clean package`.

2. Create a configuration file for Baragon Service and Baragon Agent. These are an extended version of a Dropwizard configuration file. Details on configurable fields can be found in the example configs below and in the [detailed setup and management guide](docs/managing_baragon.md) 
  - [Example Baragon Service Configuration](docs/baragon_service_config.yaml). This will be referenced as `$SERVICE_CONFIG_YAML`.
  - [Example Baragon Agent Configuration](docs/baragon_agent_config.yaml). This will be referenced as `$AGENT_CONFIG_YAML`.

3. Copy `BaragonService-*-SNAPSHOT.jar` and `$SERVICE_CONFIG_YAML` onto one or more hosts, and start the service via `java -jar BaragonService-*-SNAPSHOT.jar server $SERVICE_CONFIG_YAML`.

4. Copy `BaragonAgentService-*-SNAPSHOT.jar` and `$AGENT_CONFIG_YAML` onto each of your load balancer hosts. Start the BaragonAgent service via `java -jar BaragonAgentService-*-SNAPSHOT.jar server $AGENT_CONFIG_YAML`.

<a id="docker"></a>
## Quickstart with Docker Compose

To get an example cluster up and running, you can install [docker](https://docs.docker.com/installation/) and [docker-compose](https://docs.docker.com/compose/#installation-and-set-up).

Simply run `docker-compose up` to bring up:
- zookeper container
- Baragon Service container
- Baragon Agent + Nginx container

The Baragon UI will be available at [localhost:8080](http://localhost:8080) and nginx at [localhost:80](http://localhost:80).

*If using boot2docker replace localhost with the `boot2docker ip`

Nginx's config directories that BaragonAgent writes to will also be mounted as volumes in the `docker/configs` folder on your local machine.

<a id="vagrant"></a>
## Quickstart with Vagrant

Baragon comes with Vagrant boxes for easy local development and integration testing. Ensure Virtualbox and Vagrant are installed on your machine, then run `vagrant up` inside the `vagrant` folder of the git repo to spin up a Baragon cluster for testing. This will spin up the following:
- A BaragonService instance located at `192.168.33.20:8080/baragon/v2`
  - Zookeeper will be running on this instance
- A BaragonAgent instance located at `192.168.33.21:8882/baragon-agent/v2`
  - The BaragonAgent will be assigned to the `vagrant` loadBalancerGroup
  - Preconfigured nginx will be running on the BaragonAgent vagrant box, accessible at `192.168.33.21:80`

Additional vagrant configurations are available and details can be found [here](docs/vagrant.md)

<a id="ui"></a>
## BaragonUI
 
Baragon comes with a UI for visualization and easier management of load balancer paths and upstreams. By default it will be available in a read-only mode at `/[contextPath]/ui` see the [Example Baragon Service Configuration](docs/baragon_service_config.yaml) or [detailed setup and management guide](docs/managing_baragon.md) for more details on configuring BaragonUI behavior.

## Baragon API Docs

Full documentation on the Baragon Service API can be found [here](docs/baragon_api_docs.md)
