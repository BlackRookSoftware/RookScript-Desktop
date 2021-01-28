# Black Rook RookScript Desktop

Copyright (c) 2021 Black Rook Software.  
[https://github.com/BlackRookSoftware/RookScript](https://github.com/BlackRookSoftware/RookScript)

[Latest Release](https://github.com/BlackRookSoftware/RookScriptDesktop/releases/latest)  
[Online Javadoc](https://blackrooksoftware.github.io/RookScriptDesktop/javadoc/)  


### Required Libraries

[RookScript](https://blackrooksoftware.github.io/RookScript/) 1.10.0+


### Required Java Modules

[java.desktop](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/module-summary.html)  
* [java.xml](https://docs.oracle.com/en/java/javase/11/docs/api/java.xml/module-summary.html)  
* [java.datatransfer](https://docs.oracle.com/en/java/javase/11/docs/api/java.datatransfer/module-summary.html)  
* [java.base](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/module-summary.html)  


### Introduction

This library contains more RookScript functions that revolve around the Java Desktop module.


### Why?

This is separate from the main RookScript library since it requires an additional Java module, in order to
keep the promise of having a scripting system that is modular.


### Library

Contained in this release is a series of classes that should be used for RookScript functions. 
The javadocs contain basic outlines of each package's contents.


### Compiling with Ant

To compile this library with Apache Ant, type:

	ant compile

To make Maven-compatible JARs of this library (placed in the *build/jar* directory), type:

	ant jar

To make Javadocs (placed in the *build/docs* directory):

	ant javadoc

To compile main and test code and run tests (if any):

	ant test

To make Zip archives of everything (main src/resources, bin, javadocs, placed in the *build/zip* directory):

	ant zip

To compile, JAR, test, and Zip up everything:

	ant release

To clean up everything:

	ant clean


### Javadocs

Online Javadocs can be found at: [https://blackrooksoftware.github.io/RookScript/javadoc/](https://blackrooksoftware.github.io/RookScript/javadoc/)


### Other

This program and the accompanying materials
are made available under the terms of the GNU Lesser Public License v2.1
which accompanies this distribution, and is available at
http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

A copy of the LGPL should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 

This contains code copied from Black Rook Base, under the terms of the MIT License (docs/LICENSE-BlackRookBase.txt).
