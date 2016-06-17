DyCLINK: A Dynamic Detector for Relative Code with Link Analysis
========


DyCLINK is a system for detecting methods having similar runtime behavior at instruction level. DyCLINK constructs dynamic instruction graphs based on execution traces for methods and then conducts inexact (sub)graph matching between each execution of each method. The methods having similar (sub)graphs are called "code relatives", because they have relevant runtime behavior. The information about how DyCLINK works can be found in our [FSE 2016 paper](http://jonbell.net/fse_16_dyclink.pdf).

Running
-------
DyCLINK will rewrite the bytecode of your application for recording executed instructions and constructing graphs. The steps to install and use DyCLINK are as follows.

### Step 0
DyCLINK needs a database to store the detected code relatives. We use MySQL. For downloading and installing MySQL, please refer to [MySQL](https://www.mysql.com/). For setting up the database, there are 3 steps. You can find more details in scripts/db\_setup.


DyCLINK is a maven project. For installing maven, please refer to [maven](https://maven.apache.org/install.html). After installing maven, please change your directory to "dyclink" and use the following command to compile DyCLINK:

mvn clean package

Before running DyCLINK, please use the following script to create required directories:

./scripts/dyclink\_setup.sh

### Step 1 Configuration
DyCLINK has multiple parameters to specify. The configuration file can be found under "$dyclink\_base/config/mib\_config.json", where "$dyclink\_base" represents the base directory of DyCLINK. You can use the default values for these parameters that we set up for you. Here is the introduction for some parameters you might want to tune: <br />
  -instThreshold: The programs have instruction number smaller than this value will be ignored <br />
  -callThreshold: How many unique graphs from a single callee should be recorded <br />
  -simThreshold: The method pair that have (sub)graph similarity higher than this value is considered as a code relative <br />
  -controlWeight: The weight of the control dependency(edge) <br />
  -instDataWeigth: The weight of the data dependency derived from Java specification <br />
  -writeDataWeigth: The weight of the data dependency between writer/reader instructions <br />
  -dburl: The database address
  -dbusername: The user name of the database <br />
  -domInstDiff: The minimum instruction difference between the caller graph and its largest callee. For such caller graph will be filtered out, since most of its behavior is from its largest callee. The default value is 20, but this value should decrease with the method size that you want to detect. <br />
  -graphDensity: #edge/#vertex. If a graph's density is lower than this value, such graph will be filtered out. This default value is 0.8, but again this value should decrease with the method size that you want to detect.

### Step 2 Dynamic instruction graph
DyCLINK instrument your applications on the fly to construct dynamic instruction graphs. For executing your application, please use this command:

./scripts/dyclink\_con.sh /path/to/your/codebase application.mainclass args 

The constructed graphs of each method will be stored in a zip file under the directory that you specify for the "graphDir" field in the configuration file.

### Step 3 (Sub)graph matching
For detecting code relatives, DyCLINK takes each graph (each execution of each method) as a testing graph to query all the others (target graphs). Each graph plays as a testing graph and a target graph. DyCLINK uses a testing graph to match part of the target graph.

For computing the behavioral similarity between methods, you can either assign a single graph repository, which exhaustively compare all graphs in all zip files under this repository:

./scripts/dyclink\_sim.sh -target ./path/to/your/graphrepo1

For not boosting the number of detected code relatives, you can set "exclPkg" in your configuration file as true to exclude the comparison between methods that invoke the same callees.

You can assign two graph repositories, which compare every pair of graphs (one from graphrepo1 while the other one from graphrepo2):

./scripts/dyclink\_sim.sh -target /path/to/your/graphrepo1 -test /path/to/your/graphrepo2

Notes: You can also specify "-iginit" to filter out constructors and static constructor (which may only set up some values for classes/objects without any business logic for DyCLINK to detect). This can save you some analysis time.

The detected code relatives will be stored in your MySQL database.

### Step 4 Code relatives
For reviewing the detected code relatives, it will be convenient for you to have an UI for MySQL. If you are a MAC user, you can use [Sequel Pro](http://www.sequelpro.com/). If you are a Linux user, you can find some useful tools [here](http://alternativeto.net/software/sequel-pro/?platform=linux).

The UI tool can help you collect the comparison ID you need.

Then you can run the following command to review the detected code relatives by DyCLINK:

./scripts/dyclink\_query.sh compId insts sim -f 

compId represents the comparison ID, which can be seen from the MySQL database. insts represents the minimum size of code relatives you care. The default value is 45. sim represents the similarity threshold. -f is optional and solely for the experiments in the paper. This argument can filter out some simple utility methods that do not contribute to the real business logic of the application.

### Test Run
For testing if your system set-up is successful, you can use the command to drive a simple test case of DyCLINK:
./scripts/test\_run.sh

This script will execute two simple methods (which are a code relative) and conduct Step 2 and Step 3 sequentially. DyCLINK will ask if you want to store the information of detected code relatives in the database. Type "true", if you want to test your database. The URL and user name of your database can be specified in the configuration file. The results can also be seen in a csv file under the "results" directory.

Questions, concerns, comments
----
Please email [Mike Su](mailto:mikefhsu@cs.columbia.edu) with any feedback. This project is still under heavy development, and we have several future plans. Thus we would very much welcome any feedback.

License
-------
This software is released under the MIT license.

Copyright (c) 2016, by The Trustees of Columbia University in the City of New York.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Acknowledgements
--------
The authors of this software are [Mike Su](mailto:mikefhsu@cs.columbia.edu), [Jonathan Bell](mailto:jbell@cs.columbia.edu), [Kenneth Harvey](mailto:kh2333@caa.columbia.edu), [Simha Sethumadhavan](mailto:simha@cs.columbia.edu), [Gail Kaiser](mailto:kaiser@cs.columbia.edu) and [Tony Jebara](mailto:jebara@cs.columbia.edu). This work is funded in part by NSF CCF-1302269, CCF-1161079 and NSF CNS-0905246.

