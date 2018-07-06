 *  O r a n g e O b j e c t s 
 *  copyright 2012, 2018 by Orange Objects GmbH & Co. KG
 * 
 *  http://www.OrangeObjects.de

Author: A. Kling, M. Hofmann

# repairFileNames
Repairs bad or broken filenames within a given directory

currently everything is hardcoded based on the utf8 charset;
tested with Groovy Version: 1.7.3 JVM: 1.6.0_21

~~~
To run the testcase do the following steps:
$ cd /var/tmp
$ rm -rf testcase && repairFileNames.groovy -v -T testcase
$ repairFileNames.groovy -v testcase

the second run should produce no rename tasks any more
$ repairFileNames.groovy -v testcase

cleanup the testcase now
$ rm -rf testcase
~~~

TODO: sowohl im LATIN1 als auch im UTF-8 behandeln

