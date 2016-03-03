import sys
import subprocess
from os import listdir
from os.path import isdir, join

giveupList = list()
rootPath = ''
clusterName = ''
rootWithCluster = ''

def readGiveupList(gloc):
  global giveupList

  with open(gloc, 'r') as f:
    giveupList = f.read().splitlines()
  
  print 'Load giveupList: ' + str(giveupList)

def listPossiblePaths():
  global rootWithCluster
  global clusterName

  allUsrPkgs = list()
  for f in listdir(rootWithCluster):
    if isdir(join(rootWithCluster, f)) and (f not in giveupList):
      allUsrPkgs.append(clusterName + '.' + f)
  
  #allUsrPkgs = [f for f in listdir(rootPath) if isdir(join(rootPath, f))]
  #print 'Check allUsrPkgs: ' + str(allUsrPkgs)
  return allUsrPkgs

def execPkgs(usrPkgs):
  binRoot = join(rootPath, 'bin');
  #print 'Check binRoot: ' + binRoot
  for pkg in usrPkgs:
    mainName = pkg + '.A'
    command = '/Library/Java/JavaVirtualMachines/jdk1.7.0_07.jdk/Contents/Home/bin/java -cp \"' + binRoot + ':/Users/mikefhsu/ccws/jvm-clones/MIB/lib/*\" -XX:-UseSplitVerifier ' + mainName
    #print 'Check command: ' + command
    print 'Execute ' + mainName
    p = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    for line in p.stdout.readlines():
      print line
    retval = p.wait()

def main(args):
  global rootPath
  global clusterName
  global rootWithCluster

  readGiveupList(args[0])
  rootPath = args[1]
  clusterName = args[2]
  rootWithCluster = join(rootPath, 'bin', clusterName)
  #print 'Check root with cluster: ' + rootWithCluster
  usrPkgs = listPossiblePaths()
  execPkgs(usrPkgs)

if __name__ == '__main__':
  #print 'Argv length' + str(len(sys.argv))
  #print '1st arg' + sys.argv[1]
  main(sys.argv[1:])
