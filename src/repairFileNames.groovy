#!/usr/bin/env groovy

/*
 *  O R A N G E   O B J E C T S
 *  copyright by Orange Objects
 * 
 *  http://www.OrangeObjects.de
 * 
 *
 * Repairs bad or broken filenames within a given directory
 */

import java.util.logging.Logger;
import java.util.logging.Level;

class Repair {

    String baseDirName
    Closure renamer
    Closure fixName
    boolean random
    Logger log = Logger.getLogger('')
    List tasks = []

    def run() {
        File baseDir = new File(baseDirName)
        assert baseDir.directory : "baseDir '$baseDir' is not a directory"
        log.info("starting with $baseDirName")
        // Note:
        // Sub-directories are recursively searched in a depth-first fashion
        baseDir.eachFileRecurse { file ->
            def oldName = file.name
            def newName = fixName(oldName)
            assert newName
            log.fine('checking ' + (file.directory ? 'directory' : 'file') +
                     " \"$file\": " + (newName != oldName ? "rename to '$newName'"
                                       : 'name is ok'));
            if (newName != oldName) {
                File newFile = new File(file.parentFile, newName)
                tasks << [file, newFile]
            }
        }
        def sortedTasks = tasks.sort { taskA, taskB ->
            // note: taskA and taskB are switched
            // be longer names will be on top of the list
            taskB[0].path.length() <=> taskA[0].path.length()
        }
        log.info("about to rename ${sortedTasks.size()} elements...")
        for (task in sortedTasks) {
            def oldFile = task[0]
            def newFile = task[1]
            assert oldFile.exists() : "file '$oldFile' dosnt exist any more"
            log.fine("rename '$oldFile' to '$newFile'")
            if (newFile.exists()) {
	    	if (random) {
	    	    log.info("generating new random filename...")
	    	    def r = UUID.randomUUID().toString().split('-')[-1..-2].join()
	    	    def fooName = newFile.name
	    	    def ext = fooName.tokenize('.').last()
	    	    if (ext == fooName) {
			newFile = new File(newFile.parentFile, fooName + "." + r)
		    } else {
		        fooName = fooName.replaceAll("$ext\$", r + "." + ext)
		        newFile = new File(newFile.parentFile, fooName)
		    }
	    	} else {
    	    	    println "Sorry '$newFile' exists, please rename manually or use random mode (-r)"
		    System.exit(1)
	    	}
	    }
            assert ! newFile.exists() : "'$newFile' exists, please rename manually"
            renamer(oldFile, newFile)
        }
    }
}

class CheapFormatter extends java.util.logging.SimpleFormatter {
    public synchronized String format(java.util.logging.LogRecord record) {
        def stacktrace = ""
        if (record.thrown) {
            StringWriter sw = new StringWriter();
	        PrintWriter pw = new PrintWriter(sw);
	        record.thrown.printStackTrace(pw);
	        pw.close();
            stacktrace="\n$sw"
        }
        "${record.level.name}: ${record.message} $stacktrace\n"
    }
}

def configureLogging(Level level) {
    assert level
    def formatter = new CheapFormatter()
    def rootLogger = Logger.getLogger('')
    rootLogger.setLevel(level)
    for (handler in rootLogger.getHandlers()) {
        handler.setLevel(level)
        handler.setFormatter(formatter)
    }
}

def checkLogging() {
    Logger log = Logger.getLogger('')
    log.finest("logger finest message");
    log.log(Level.SEVERE, "throw message", new NullPointerException("dummy -- ignore"));
    //assert false : "shit happens"
}

// populateTestDir recursivly, starting in dir
def populateTestDir(File dir, int level, String charset) {
    if (level <= 0) return  // stop recursion now
    Logger log = Logger.getLogger('')
    assert dir.directory
    boolean ok
    def samples_latin1 = ['A B C', 'ok', 'D E F', 'okname.txt', 'wrong name.txt',
                   'G H',
                   'latin1_umlaute ������',
                   'sonderZeichen \'?![]{}',
                   'sharpsz �.txt']
    def samples_utf8 = ['A B C', 'ok', 'D E F', 'okname.txt', 'wrong name.txt',
                   'G H',
                   'utf8_umlaute \u00e4\u00dc\u00f6\u00c4\u00d6\u00df',
                   'sonderZeichen \'?![]{}',
                   'sharpsz \u00df.txt']
    def samples = charset == 'UTF8' ? samples_utf8 : samples_latin1
    for (sample in samples) {
        File sampleFile = new File(dir, sample)
        assert !sampleFile.exists()
        if (sample.endsWith('.txt')) {
            ok = sampleFile.createNewFile()
            assert ok : "failed to create file '$sampleFile'"
            log.fine("file '$sampleFile' created")
        } else {
            ok = sampleFile.mkdir()
            assert ok : "failed to create directory '$sampleFile'"
            log.fine("directory '$sampleFile' created")
            populateTestDir(sampleFile, level - 1, charset)
        }
    }
}

def buildTestCase(baseDirName, charset) {
    Logger log = Logger.getLogger('')
    File baseDir = new File(baseDirName)
    assert ! baseDir.exists() : "Sorry '$baseDir' already exists"
    def ok = baseDir.mkdir()
    assert ok : "failed to create directory '$baseDir'"
    // now create files and dirs
    populateTestDir(baseDir, 4, charset)
}

// Closure which shows what would have happend (used by dry-run option)
def dummyRenamer = { oldFile, newFile ->
    assert oldFile instanceof File
    assert newFile instanceof File
    println "mv '$oldFile' '$newFile'"
}

// Closure which renames Files
def realRenamer = { oldFile, newFile ->
    assert oldFile instanceof File
    assert newFile instanceof File
    def ok = oldFile.renameTo(newFile)
    assert ok : "failed to rename '$oldFile' to '$newFile'"
}

// get a name and return the possibly corrected name
// it is assumed that oldName is coded in the utf8 charset
def nameFixerUTF8 = { oldName ->
    // shortcut to avoid unecessary string manipulation
    if (oldName =~ /^[0-9A-Za-z\_\-.]+$/) return oldName

    // Whitespaces werden durch einen Underscore ersetzt
    String newName = oldName.replaceAll('\\s+', '_');
    // Umlauts deserve a special
    def specials = [['\u00e4','ae'], ['\u00f6','oe'], ['\u00fc','\u00fce'],
                    ['\u00c4','Ae'], ['\u00d6','Oe'], ['\u00dc','Ue'],['\u00df','sz']]
    for (special in specials) {
        newName = newName.replaceAll(special[0], special[1])
    }
    // und alles was unbekannt ist wird entfernt
    newName = newName.replaceAll('[^a-zA-Z0-9\\_\\-.]', '');
    return newName
}

// get a name and return the possibly corrected name
// it is assumed that oldName is coded in the latin1 charset
def nameFixerLATIN1 = { oldName ->
    // shortcut to avoid unecessary string manipulation
    if (oldName =~ /^[0-9A-Za-z\_\-.]+$/) return oldName

    // Whitespaces werden durch einen Underscore ersetzt
    String newName = oldName.replaceAll('\\s+', '_');
    // Umlauts deserve a special
    def specials= [['�','ae'], ['�','oe'], ['�','ue'],
                   ['�','Ae'], ['�','Oe'], ['�','Ue'],['�','sz']]
    for (special in specials) {
        newName = newName.replaceAll(special[0], special[1])
    }
    // und alles was unbekannt ist wird entfernt
    newName = newName.replaceAll('[^a-zA-Z0-9\\_\\-.]', '');
    return newName
}

def parseCommandLine(args) {
    def cli = new CliBuilder(usage:'repairFilenNames [option]* directory')
    cli.h(longOpt: 'help', 'usage information')
    cli.n('dry-run')
    cli.v('verbose')
    cli.r('random postfix for duplicate filenames')
    cli.c(args:1, argName:'charset', 'currently only LATIN1 or UTF8 supported, default is UTF8')
    cli.T('build testcases in directory, then exit')
    def options = cli.parse(args)
    if (options.h) { cli.usage(); System.exit(1) }
    if (options.c) {
        if (options.c == 'UTF8' || options.c == 'LATIN1') {
        } else {
            println "wrong charset option: $options.c"
            cli.usage(); System.exit(1)
        }
    }
    if (options.getArgList().size != 1) { cli.usage(); System.exit(1) }
    return options
}

// ----------------------------------------------------------------------------
// main programm
// ----------------------------------------------------------------------------

options = parseCommandLine(args)
configureLogging(options.v ? Level.FINEST : Level.INFO)
//checkLogging()

def charset = options.c ? options.c : 'UTF8'
//println "charset=$charset"; System.exit(0)

if (options.T) {
    buildTestCase(options.getArgList()[0], charset)
    System.exit(0)
}

def repair = new Repair(baseDirName:options.getArgList()[0],
                        fixName:(charset == 'UTF8' ? nameFixerUTF8 : nameFixerLATIN1),
                        renamer: (options.n ? dummyRenamer : realRenamer),
                        random:options.r)
// do the real work now
repair.run()
