# deployment-maven-plugin

[![License][License-Image]][License-Url]
[![Build][Build-Status-Image]][Build-Status-Url] 
[![Maintainable][Maintainable-image]][Maintainable-Url] 
[![Gitter][Gitter-image]][Gitter-Url] 

# CURRENTLY REFACTORING FROM BASH TO REAL MAVEN MOJO

### Description
Get rit of huge deployment definitions in your pom files and keep them small.
Auto handling semantic versioning, maven plugins, and much more while you can still use the original userProperties of the plugins   

### plugin
````xml
<plugin>
    <groupId>berlin.yuna</groupId>
    <artifactId>deployment-maven-plugin</artifactId>
    <version>0.0.1</version>
</plugin>
````

### Usage
````bash
mvn deployment:run -Djava.doc=true -Djava.source -Dupdate.minor
````
* Will create java doc, java sources, and updates dependencies

# Semantic- && versioning
### Parameters
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| project.version     | String  | ''                 | Sets project version in pom                                                |
| remove.snapshot     | Boolean | false              | Removes snapshot from version                                              |
| semantic.format     | String  | ''                 | Updates semantic version from regex pattern (overwrites project.version)   |

### Semantic version
* Sets the version coming from branch name (uses git refLog for that)
* The matching branch names has to be defined by using regex
* Syntax:
* ````"<separator>::<major>::<minor>::<patch>"```` 
* Example:
* ````semantic.format="[.-]::release.*::feature.*::bugfix\|hotfix::custom_1.*[A-Z]"````

# Tagging & Committing
### Parameters
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| tag                 | Boolean | false              | Tags the project (with project.version or semantic) if not already exists  |
| tag                 | String  | ${project.version} | Tags the project (with project.version or semantic) if not already exists  |
| tag.break           | Boolean | false              | Tags the project (with project.version or semantic) fails if already exists|
| message             | String  | ${auto}            | Commit msg for tag default = \[project.version] \[branchname], \[tag] ...  |
| scm.provider        | String  | scm:git            | needed for tagging & committing                                            |
| COMMIT              | String  | ''                 | Custom commit message on changes - "false" = deactivate commits            |
* Example tag with project.version (or semantic version if active)
* ````tag```` 
* ````tag=true```` 
* ````tag="my.own.version"```` 
* ````message="new release"```` 
* 'tag.break' parameter will stop tagging if the tag already exists 

# Update dependencies
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| update.minor        | Boolean | false              | Updates parent, properties, dependencies                                   |
| update.major        | Boolean | false              | Updates parent, properties, dependencies                                   |

# Testing
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| test.run            | Boolean | false              | runs test.unit and test.integration                                        |
| test.unit           | Boolean | false              | runs failsafe for unitTest                                                 |
| test.int            | Boolean | false              | alias for test.integration                                                 |
| test.integration    | Boolean | false              | runs surefire integration, component, contract, smoke                      |
| JACOCO              | Boolean | false              | runs failsafe integration test and surefire unitTest                       |

# UNDER CONSTRUCTION

### Building
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| clean               | Boolean | false              | cleans target and resolves dependencies                                    |
| clean.cache         | Boolean | false              | Purges local maven repository cache                                        |
| java.doc            | Boolean | false              | Creates java doc (javadoc.jar) if its not a pom artifact                   |
| java.source         | Boolean | false              | Creates java sources (sources.jar) if its not a pom artifact               |
| gpg.pass            | String  | ''                 | Signs artifacts (.asc) with GPG 2.1                                        |
### Deployment
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| DEPLOY_ID           | String  | ''                 | (Nexus) Deploys artifacts (server id = Settings.xml)                       |
| RELEASE             | Boolean | false              | (Nexus) Releases the deployment                                            |
| NEXUS_BASE_URL      | String  | ''                 | (Nexus) The nexus base url (e.g https://my.nexus.com)                      |
| NEXUS_DEPLOY_URL    | String  | ''                 | (Nexus) Staging url (e.g https://my.nexus.com/service/local/staging/deploy)|
### Add to Settings.xml session
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| Server              | String | ''                  | server id (multiple possible && caseInsensitive)                           |
| Username            | String | ''                  | username (multiple possible && caseInsensitive)                            |
| Password            | String | ''                  | password (multiple possible && caseInsensitive)                            |
| PrivateKey          | String | ''                  | e.g. ${user.home}/.ssh/id_dsa) (multiple possible && caseInsensitive)      |
| Passphrase          | String | ''                  | privateKey, passphrase (multiple possible && caseInsensitive)              |
| FilePermissions     | String | ''                  | permissions, e.g. 664, or 775  (multiple possible && caseInsensitive)      |
| DirectoryPermissions| String | ''                  | permissions, e.g. 664, or 775  (multiple possible && caseInsensitive)      |
### Misc
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| REPORT              | Boolean | false              | Generates report about version updates                                     |                                            |                                                  |
| test.skip           | Boolean | false              | same as "maven.test.skip"                                                  |
| project.encoding    | Boolean | false              | sets default encoding to every encoding parameter definition               |
| java.version        | Boolean | false              | sets default java version to every java version parameter definition       |

### Requirements
* \[JAVA\] for maven 
* \[MAVEN\] to run maven commands
* \[GIT\] for tagging

### Technical links
* [maven-javadoc-plugin](https://maven.apache.org/plugins/maven-javadoc-plugin/)
* [maven-source-plugin](https://maven.apache.org/plugins/maven-source-plugin/)
* [maven-surefire-plugin](http://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html)
* [versions-maven-plugin](https://www.mojohaus.org/versions-maven-plugin/set-mojo.html)
* [maven-gpg-plugin](http://maven.apache.org/plugins/maven-gpg-plugin/usage.html)
* [maven-scm-plugin](http://maven.apache.org/scm/maven-scm-plugin/plugin-info.html)
* [upload-an-artifact-into-Nexus](https://support.sonatype.com/hc/en-us/articles/213465818-How-can-I-programmatically-upload-an-artifact-into-Nexus-2-)

### TODO
* [ ] tag message can contain environment properties
* [ ] not tag when last commit was tag commit
* [ ] set always autoReleaseAfterClose=false and add "mvn nexus-staging:release" to release process
* [ ] reset readme urls, description and title
* [ ] Deploy dynamic to nexus
* [ ] Deploy dynamic to artifactory
* [ ] try to use JGit for git service
* [ ] org.sonatype.plugins
* [ ] own or buy logo https://www.designevo.com/apps/logo/?name=blue-hexagon-and-3d-container

![maven-deployment](src/main/resources/banner.png "maven-deployment")

[License-Url]: https://www.apache.org/licenses/LICENSE-2.0
[License-Image]: https://img.shields.io/badge/License-Apache2-blue.svg
[github-release]: https://github.com/YunaBraska/maven-deployment
[Build-Status-Url]: https://travis-ci.org/YunaBraska/maven-deployment
[Build-Status-Image]: https://travis-ci.org/YunaBraska/maven-deployment.svg?branch=master
[Coverage-Url]: https://codecov.io/gh/YunaBraska/maven-deployment?branch=master
[Coverage-image]: https://codecov.io/gh/YunaBraska/maven-deployment/branch/master/graphs/badge.svg
[Version-url]: https://github.com/YunaBraska/maven-deployment
[Version-image]: https://badge.fury.io/gh/YunaBraska%2Fmaven-deployment.svg
[Central-url]: https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22maven-deployment%22
[Central-image]: https://maven-badges.herokuapp.com/maven-central/berlin.yuna/maven-deployment/badge.svg
[Maintainable-Url]: https://codeclimate.com/github/YunaBraska/maven-deployment
[Maintainable-image]: https://codeclimate.com/github/YunaBraska/maven-deployment.svg
[Gitter-Url]: https://gitter.im/nats-streaming-server-embedded/Lobby
[Gitter-image]: https://img.shields.io/badge/gitter-join%20chat%20%E2%86%92-brightgreen.svg
[Javadoc-url]: http://javadoc.io/doc/berlin.yuna/maven-deployment
[Javadoc-image]: http://javadoc.io/badge/berlin.yuna/maven-deployment.svg