# GeoMapApp
Java application for exploring geoscience data.

# Overview
GeoMapApp is an earth science exploration and visualization application that is continually being expanded as part of the Marine Geoscience Data System (MGDS) at the Lamont-Doherty Earth Observatory of Columbia University. The application provides direct access to the Global Multi-Resolution Topography (GMRT) compilation that hosts high resolution (~100 m node spacing) bathymetry from multibeam data for ocean areas and ASTER (Advanced Spaceborne Thermal Emission and Reflection Radiometer) and NED (National Elevation Dataset) topography datasets for the global land masses.

If you are not interested in the code but somehow found yourself here, go to [our website](https://www.geomapapp.org/) for more information.

This code base also includes the code for Virtual Ocean. Virtual Ocean integrated the GeoMapApp tool suite with the NASA World Wind 3-D earth browser to create a powerful platform for interdisciplinary research and education. Virtual Ocean is no longer supported.

The code is implemented in Java.

# Requirements For Compilation
*  Maven for dependency management.
*  Java SE Development Kit 8 or higher.
*	Developers must have convenient access to the source code as well as modern IDE (Integrated Development Environment) tools to easily participate in the project’s life-cycle. Concurrent editing of the same source should be naturally supported.
*	The project leader should have robust, industry-standard versioning tools to manage the incoming code from contributors (accept, revert & modify changes). 
*	To ensure consistent build releases, the project leader should have tools to mark existing code (at any given time) as alpha, beta or release candidate. 
*	The project leader must have tools to define user permissions for source code access and/or modification. 
*	The project leader should guide the development team by publishing:
   	* Desired Features Specifications
    * UML, Use Case & Flow Control diagrams
    *	Known Application Bugs

# Release History
* 09/15/2025 v3.7.6 Public release of v3.7.6 of GeoMapApp.
* 04/22/2025 v3.7.5 Public release of v3.7.5 of GeoMapApp.
* 08/05/2024 v3.7.4 Public release of v3.7.4 of GeoMapApp.
* 04/15/2024 v3.7.3 Public release of v3.7.3 of GeoMapApp.
* 03/06/2024 v3.7.2 Public release of v3.7.2 of GeoMapApp.
* 08/10/2023 v3.7.1 Public release of v3.7.1 of GeoMapApp.
* 08/09/2023 v3.7.0 Public release of v3.7.0 of GeoMapApp.
* 09/07/2022 v3.6.15 Public release of v3.6.15 of GeoMapApp.
* 09/23/2021 v3.6.14 Public release of v3.6.14 of GeoMapApp.
* 03/09/2021 v3.6.12 Public release of v3.6.12 of GeoMapApp.
* 02/22/2021 v3.6.11 Public release of v3.6.11 of GeoMapApp.
* 04/24/2019 v3.6.10 Public release of v3.6.10 of GeoMapApp.
* 04/26/2018 v3.6.8 Public release of v3.6.8 of GeoMapApp.
* 07/20/2017 v3.6.6 Public release of v3.6.6 of GeoMapApp.



# What's here

This section provides a brief description of the source code files and folders in this repository:

## .gitignore
A file which contains a list of files and directories that Git has been explicitly told to ignore. 

## LICENSE
The Apache License file for this product.

## README.md
This file.

## pom.xml
The build file used to compile GMA and create the unsigned jar files. Since it's a Maven build, it also manages GMA's dependencies. You should be able to build GMA locally with `mvn clean compile` and make a jar with `mvn clean compile package`.

## src/main/java
Contains the Java source code for GeoMapApp.

## src/main/resources/
A directory that contains the various resources required by GeoMapApp, e.g. external libraries, images, icons, color look up tables, etc.
