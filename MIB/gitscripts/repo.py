import requests
import json
import sys
import os
import subprocess
import shlex
import shutil

ANDROID = 'android'

class Repo:
    """Repo contains git url, stars and size"""
    def __init__(self, name, url, desc, stars, size):
        self.name = name
        self.url = url
        self.desc = desc
        self.stars = stars
        self.size = size
        
def cleanRepo(localrepo):
    if localrepo == '/' or len(localrepo) == 1:
        print 'Forbid to remove the root dir ' + localrepo
        sys.exit(-1)
        
    print 'Clean repo: ' + localrepo
    
    if os.path.exists(localrepo):
        for root, dirs, files in os.walk(localrepo, topdown=False):
            for name in files:
                os.remove(os.path.join(root, name))
            for name in dirs:
                os.rmdir(os.path.join(root, name))
                    
def execCommand(command):
    p = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    for line in p.stdout.readlines():
        print line;
    
    retval = p.wait();
    print 'Return from subprocess: ' + str(retval)
    return retval

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print 'Invalid input'
        sys.exit(-1)
    
    term = sys.argv[1]
    lan = sys.argv[2]
    '''sizeLim = sys.argv[2]'''
    localbase = sys.argv[3]
        
    '''repoLimit = int(sys.argv[3])'''
    
    query = 'https://api.github.com/search/repositories?q=' + term + '+language:' + lan + '&sort=stars&order=desc&per_page=50'
    print 'Confirm query: ' + query
    
    if (os.path.exists(localbase)):
        print 'Confirm local codebase: ' + localbase
    else:
        print 'Creating codebase: ' + localbase
        os.makedirs(localbase)
    
    r = requests.get(query)
    
    gitList = list()
    if (r.ok):
        queryItems = json.loads(r.content or r.text)
        totalRepos = int(queryItems['total_count'])
        repos = queryItems['items']
        print 'Reported total repos: ' + str(totalRepos)
        print 'Returned repos: ' + str(len(repos))
        '''print json.dumps(queryItems['items'][0], indent=4)'''
        repoLimit = len(repos)
            
        print 'Repo limit: ' + str(repoLimit)
        reportPath = './repos.txt'
        if os.path.exists(reportPath):
            os.remove(reportPath)
        report = open(reportPath, 'w')
        
        compileReportPath = "./compiled.txt"
        if os.path.exists(compileReportPath):
            os.remove(compileReportPath)
        compileReport = open(compileReportPath, 'w')
        
        for i in xrange(repoLimit):
            curItem = repos[i]
            name = curItem['name'].lower()
            desc = curItem['description'].lower();
            if ANDROID in desc or ANDROID in name:
                print 'Android-related repos: ' + curItem['name'] + ' ' + desc
            else:
                gitRepo = Repo(curItem['name'], curItem['git_url'], desc, curItem['stargazers_count'], curItem['size'])
                gitList.append(gitRepo)
        
        print 'Selected git respos: ' + str(len(gitList))
        curPath = os.getcwd()
        os.environ['JAVA_HOME'] = '/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre'
        for g in gitList:
            print g.name + ' ' + g.url + ' ' + str(g.stars) + ' ' + str(g.size)
            
            repoLoc = localbase + g.name
            command = 'git clone ' + g.url + ' ' + repoLoc
            cleanRepo(repoLoc)
            retval = execCommand(command)
            
            if retval == 0:
                report.write("S: " + g.name + " " + g.url + "\n")
            else:
                report.write("F: " + g.name + " " + g.url + "\n")
            
            mvnCommand = '/usr/local/Cellar/maven/3.1.1/libexec/bin/mvn clean compile'
            os.chdir(repoLoc)
            subprocess.call(mvnCommand, shell=True)
            retval = execCommand(mvnCommand)
            os.chdir(curPath)
            
            if retval == 0:
                compileReport.write('S: ' + g.name + " " + g.url + "\n")
                print 'Successful compilation: ' + g.name + ' ' + g.url
            else:
                compileReport.write('F: ' + g.name + " " + g.url + "\n")
                print 'Fail compilation, remove repo: ' + g.name + ' ' + g.url
                shutil.rmtree(repoLoc)
                
                        
        report.close()
        compileReport.close()
    else:
        print "Fail to retrieve repo info from " + query
    
