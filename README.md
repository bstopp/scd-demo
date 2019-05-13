# Sling Content Distribution Demo

This project provides an example of how to configure a Sling Content Distribution setup. You can use it as a template for implementation based on your own needs. 


## Setup

Almost everything that is needed is provided by the different modules. Only one configuration needs to be updated.

1. Log onto the [Author's Crypto Support](http://scd-demo.localhost:4502/system/console/crypto) and encrypt the *scd-transport-user*'s password (`password`)
1. Update Crypto Distribution Transport Secret Provider configuration with the encrypted password [here](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config.author/com.adobe.granite.distribution.core.impl.CryptoDistributionTransportSecretProvider-scd-demo.xml#L6).
1. Run the ACL Tool to apply the User/Permissions, curl commands are (assuming standard installations for ports):
  *  `curl -sS --retry 1 -u admin:admin -X POST http://localhost:4502/system/console/jmx/biz.netcentric.cq.tools:type=ACTool/op/apply/`
  *  `curl -sS --retry 1 -u admin:admin -X POST http://localhost:4503/system/console/jmx/biz.netcentric.cq.tools:type=ACTool/op/apply/`
  *  `curl -sS --retry 1 -u admin:admin -X POST http://localhost:4504/system/console/jmx/biz.netcentric.cq.tools:type=ACTool/op/apply/`

That should be all that's needed.

### How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

If you have a running AEM instance you can build and package the whole project and deploy into AEM with  

    mvn clean install -PautoInstallPackage
    
Or to deploy it to a single publish instance, run

    mvn clean install -PautoInstallPackagePublish1

Or to deploy it to the second publish instance, run

    mvn clean install -PautoInstallPackagePublish2
    
Or alternatively

    mvn clean install -PautoInstallPackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle


### Maven settings

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html

## Configuration Details

### OOTB Service Configurations

These configurations are available OOTB in AEM and should not be changed. They are listed here to provide information on their purpose.

#### Apache Sling Distribution Resources - Configuration Resource Provider Factory

This is used to expose agent configurations as resources.

#### Apache Sling Distribution Resources - Service Resource Provider Factory - Agent

This is a factory for managing all _Agent_ type distribution services. It specifies the resource endpoint for the API.

#### Apache Sling Distribution Resources - Service Resource Provider Factory - Importer

This is a factory for managing all _Importer_ type distribution services. It specifies the resource endpoint for the API.

#### Apache Sling Distribution Resources - Service Resource Provider Factory - Exporter

This is a factory for managing all _Exporter_ type distribution services. It specifies the resource endpoint for the API.


### Implementation Specified Service Configurations

These are configurations which must be provided by the implementation team. Some configurations are only needed on either the Author or Publish, the information is provided.
 
#### Service User Mapper Amendment

Required on: 
 * Author
 * Pubilsh

[This configuration](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config/org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-scd-demo.xml) maps the service user to the bundles/services for accessing the repository.

#### Apache Sling Distribution Request Authorization - Privilege Request Authorization Strategy

Required on: 
 * Author
 * Pubilsh

[This configuration](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config/org.apache.sling.distribution.agent.impl.PrivilegeDistributionRequestAuthorizationStrategyFactory-scd-demo.xml) maps a service name to a specific permission required by the service account in order to distribute the content.

* *Name*: Provide a unique name for looking up this service.
* *JCR Privilege*: The privilige that the user who wants to distribute content must have, or the distribution will be ignored/fail. 

#### Adobe Granite Distribution - Encrypted Password Transport Secret Provider

Required on: 
 * Author

[This configuration](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config.author/com.adobe.granite.distribution.core.impl.CryptoDistributionTransportSecretProvider-scd-demo.xml) provides a service for managing a user/password in an encrypted fashion, so that the Distribution services can authenticate to the Publish server.

The value in this file must be update for each specific AEM Author, as the password is encrypted using the Authors keys.
  
* *Property Name*: Provide a unique name for looking up this service.
* *Property Username*: Specify the username for the publish instance authentication.
* *Property encryptedPassword*: Specify the Crypto encrypted password for the publish instance authentication.


#### Apache Sling Distribution Packaging - Vault Package Builder Factory

Required on: 
 * Author
 * Pubilsh
 
[This configuration](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config/org.apache.sling.distribution.serialization.impl.vlt.VaultDistributionPackageBuilderFactory-scd-demo.xml) defines how the packages for the distribution are created.

* *Name*: Provide a unique name for looking up this service.
* *Type*: The type of package that is created by this packager.
* *Import Mode*: The [import mode](https://jackrabbit.apache.org/filevault/importmode.html) for the package.
* *ACL Handling*: The [ACL Handling](https://jackrabbit.apache.org/filevault/apidocs/org/apache/jackrabbit/vault/fs/io/AccessControlHandling.html) mode for this package.
* *Package Roots*: The root of the package filters when the packages are created. 
* *Package Node Filters*: Filters to apply to the package.
* *Package Property Filters*: Filters to apply to the paths within the package.
* *Temp Filesystem Folder*: The location on the host filesystem to use for building out the zip files.
* *Use Binary References*: Support for Binaryless replication?
* *Autosave Threshhold*: Batch size for autosaving.
* *Package Cleanup Delay*: How long to wait between cleanup of distributed packages.
* *File Size Threshold*: Size of file before using file buffering instead of memory.
* *Memory Unit for Threshold*: The type of unit to use for the memory threshold.
* *Flag to Enable/Disable Off Heap*: Use off heap memory?
* *Digest Algorithm Checksum*: What type of algorithm to use for when validating the created package.
* *Paths Mapping*: Mapping of packages from source to destination if moving content.

#### Apache Sling Distribution Trigger - Scheduled Triggers Factory

Required on: 
 * Author

[This configuration](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config.author/org.apache.sling.distribution.trigger.impl.ScheduledDistributionTriggerFactory-scd-demo.xml) triggers the sync agent to check for available packages on the Publish instances.

* *Name*: A unique name for looking up this service.
* *Distribution Type*: The type of request to trigger.
* *Distributed Path*: The path to be distributed regularly. (Not use in this demo)
* *Interval in Sections*: Frequency of the triggered check.
* *Service Name*: The Service User name mapping to use for repository access.

#### Apache Sling Distribution Importer - Local Package Importer Factory
 
Required on:
 * Publish

[This configuration](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config.publish/org.apache.sling.distribution.packaging.impl.importer.LocalDistributionPackageImporterFactory-scd-demo.xml) is used to unpack a package synchronized from the Author.

* *Name*: The name of the importer. This will be used and the service endpoint in the URL mapping.
* *Package Builder*: Service reference for the package builder to used to unpack the distributed packages. This takes the format `(name=<vlt-servicename>)`


#### Apache Sling Distribution Agent - Queue Agents Factory

Required on:
 * Publish

[This configuration](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config.publish/org.apache.sling.distribution.agent.impl.QueueDistributionAgentFactory-scd-demo.xml) is used to queue content on the Publish tier for synchronization. It is read by the Author sync agent.

* *Name*: A unique name for looking up the service.
* *Kind*: The type of Queue. (This property is not visible on the OSGi form, the name is `kind` and value is `agent`)
* *Type*: The type of Factory. (This property is not visible on the OSGi form, the name is `type` and value is `queue`)
* *Title*: A descriptive title for this Queue Agent.
* *Details*: A description for this Queue Agent.
* *Enabled*: Flag for whether or not this agent is enabled.
* *Service Name*: The Service User name mapping to use for repository access.
* *Log Level*: Logger level setting.
* *Allowed Roots*: Limits distribution to specific repository root paths.
* *Request Authorization Strategy*: Service reference for the privilege to use for checking queue support. This takes the format `(name=<privilege-servicename>)`
* *Package Builder*: Service reference for the VLT package builder to use for this queue. This takes the format `(name=<package-builder-servicename>)`
* *Triggers*: Any triggers this queue uses to process distributions. This takes the format `(name=<trigger-servicename>)`
* *Priority Queues* Name of any priority queues to use.


#### Apache Sling Distribution Exporter - Agent Based Package Exporter 

Required on:
 * Publish

[This configuration](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config.publish/org.apache.sling.distribution.packaging.impl.exporter.AgentDistributionPackageExporterFactory-scd-demo.xml) is used as the service endpoint on the Publish tier for synchronization. It is read by the Author sync agent.

* *Name*: The name of the exporter. This will be used and the service endpoint in the URL mapping.
* *Queue*: The name of the queue on the agent to read for distributing content. Specify either a named priority queue, or default.
* *Distribution Agent*: Service reference for the distribution agent to read. This takes the format `(name=<agent-queue-servicename>)`

#### Apache Sling Distribution Agent - Sync Agents Factory

Required on:
 * Author

[This configuration](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config.author/org.apache.sling.distribution.agent.impl.SyncDistributionAgentFactory-scd-demo.xml) is used to manage the actual synchronization between the publish tier.

* *Name*: Unique name for this service for references.
* *Kind*: The type of Factory. (This property is not visible on the OSGi form, the name is `kind` and value is `agent`)
* *Type*: The type of Agent. (This property is not visible on the OSGi form, the name is `type` and value is `sync`)
* *Title*: A descriptive title for this Sync Agent.
* *Details*: A description for this Sync Agent.
* *Enabled*: Flag for whether or not this agent is enabled.
* *Service Name*: The Service User name mapping to use for repository access.
* *Log Level*: Logger level setting.
* *Queue Processing Enabled*: Whether or not the endpoint queues are processed.
* *Passive Queues*: List of queues that should be disabled.These queues will gather all the packages until they are removed explicitly.
* *Exporter Endpoints*: URL Endpoints on the Publish instances which will respond to requests for packages to be distributed.
* *Importer Endpoints*: URL Endpoints on the Publish instances to which packages are sent.
* *Retry Strategy*: The strategy to apply after a certain number of failed retries.
* *Retry Attempts*: The number of times to retry a package.
* *Pull Items*: How many items in the queue to pull at a single request.
* *HTTP Connection Timeout*: How long to timeout for HTTP Connections.
* *Request Authorization Strategy*: Service reference for the privilege to use for checking queue support. This takes the format `(name=<privilege-servicename>)`
* *Transport Secret Provider*: Service reference for the transport secret containing the credentials for Publish instances. This takes the format `(name=<package-builder-servicename>)`
* *Package Builder*: Service reference for the VLT package builder to use for this queue. This takes the format `(name=<package-builder-servicename>)`
* *Triggers*: Any triggers this queue uses to process distributions. This takes the format `(name=<trigger-servicename>)`


### Implementation Specified Users

There are two users that this demo uses to perform its operations.

#### Sync Service User
This system user is for the services to access the repository to package and process updates to the content. 

The permission necessary for this user are found in the provided NetCentric ACL Tool yaml configuration [here](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config/acl-config.yaml#L13-L29).

#### Sync Transport User

This user is used to authenticate from the Author to the Publish to send the packages. This is not a system user, as it needs to support password authentication. However it only needs to be created on the _Publish_ systems.
 
As of the time of this writing, it also must be in the administrators group - until someone can determine the minimum permissions to support a package transport and install. The NetCentric ACL Tool configuration can be found [here](/ui.apps/src/main/content/jcr_root/apps/scd-demo/config.publish/acl-config.yaml#L3-L9).


### Implementation Provided Code

In order to trigger a sync event, a JCR Event listener is required. This listener will listen for events, and trigger a distribution event. [This is an example for this demo](/core/src/main/java/com/github/bstopp/demo/scd/core/listeners/PageModificationEventListener.java).

### Testing the Demo.

To test the demo, simply update any page property on one of the publish instances, and it will replicate to the other.   