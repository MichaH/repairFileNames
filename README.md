<br>
copyright 2012, 2018 by Orange Objects GmbH & Co. KG
#  O r a n g e  O b j e c t s
[http://www.OrangeObjects.de](URL)<br>
Author: A. Kling, M. Hofmann

<br>
## repairFileNames.groovy
Repairs bad or broken filenames within a given directory

### Environment

tested with Groovy Version: 1.7.3 JVM: 1.6.0_21
tested with Groovy Version: 2.5.0 JVM: 1.8.0_171 Vendor: Oracle Corporation OS: Linux

### Tipp
* create a symbolic link `repairFileNames` to `..../src/repairFileNames.groovy` in your local `~/bin/`.

### Test
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

### Notes
* currently everything is hardcoded based on the utf8 charset


