###### Buiild status for branch:
 - Master:
   - [![build status](https://gitlab.prtech.mk/prtech/triglav_rest/badges/master/build.svg)](https://gitlab.prtech.mk/prtech/triglav_rest/commits/master)
 - Staging:
   - [![build status](https://gitlab.prtech.mk/prtech/triglav_rest/badges/staging/build.svg)](https://gitlab.prtech.mk/prtech/triglav_rest/commits/staging)
 - Dev:
   - [![build status](https://gitlab.prtech.mk/prtech/triglav_rest/badges/dev/build.svg)](https://gitlab.prtech.mk/prtech/triglav_rest/commits/dev)

### Git; Branch and Stability Info
Source control is `Git` exclusive:

* The `master` branch is updated only from the current state of the `staging` branch
* The `staging` branch must only be updated with commits from the `dev` branch
* The `dev` branch contains all the latest additions to the project
* All larger feature updates must be developed in their own branch and later merged into `dev`


# Project info:
Triglav is the core svarog webservice extension project ( and its current iteration is using the Jersey 2.X REST library https://jersey.java.net/ ) .
It publishes webservices for the default svarog functionalities as well as enabling dynamic extensibility by giving you the option of adding plugins which contain webservices for specific projects.

The default path for the location of the plugins, which need to be in the JAR file format, is /WebContent/plugins or defined in svarog.properties as triglav.plugin_path.

There are 3 options for creating dynamic webservices:
1. Annotated classes with the @Path annotation
2. Classes that implement the interface ITriglavPlugin.java from the svarog project.
3. Classes that have methods that return a value of the Response type.

All methods that are meant to be published as a webservice must return a value of type Response.

The dynamic webservices are loaded by the SvarogPluginExtender.