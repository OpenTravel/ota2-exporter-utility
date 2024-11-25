# OTM Exporter Tool

## Overview

The OTM Exporter utility is intended to be a short-term tool to help facilitate the OpenTravel 2.0 workgroup process.  Prior
to the creation of this tool, workgroups found it very difficult to work with the OTM-DEx modeling application without the
help of expert users who were already familiar with the workings, not only of OTM-DEx, but of the OTM model repository.  The
Exporter utility helps to bridge that gap by creating an export of the main OpenTravel OTM Repository to a flat and easily-accessible
public GitHub repository.  Workgroups can then clone the exported repository and work with OTM models on their local file
systems.

Once the workgroup is complete, changes can be submitted and merged back to the original OpenTravel OTM Repository.  Note that
the merge-back functionality is targeted for a future release of this tool.

## Installation and Use

### System Requirements

The only system requirement for running the OTM Exporter utility is that Java 17 or later be installed on the workstation
where the tool will be launched.

**NOTE**: It must be noted that a current limitation of the OTM Exporter utility is that it relies on previously-entered
credentials to access the OpenTravel OTM Repository.  This means that the OTM-DEx editor must have been run on the same
workstation or system, and that credentials for the OpenTravel OTM Repository must have been entered.

### Download and Installation
The Exporter utility can be accessed by downloading the zip file from the latest release of this project (see
[here](https://github.com/orgs/OpenTravel/)).  Once downloaded, simply unzip the file and run the `otm_exporter.bat` file (for Windows)
or `otm_exporter.sh` (for Linux or Mac) to launch the tool.

For now, using the tool is fairly simple in that the only values that must be provided are the following:

- **GitHub Organization Name**: This is the GitHub organization where the repository export will be created.  By default, this should always be `OpenTravel` but other orgs can be specified if needed.
- **GitHub Repository Name**: This is the name of the GitHub repository that will be created for the export.
- **GitHub Access Token**: The personal access token that should be used when creating the repository.  Note that the token must have permission to create a repository in the organization.

## Build Instructions (Developers)

Local builds of the OTM Exporter utility, can be done by running the following command (Maven 3.x required):

```
$ mvn clean install
```
