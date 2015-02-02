#/bin/sh
rm -rf build
rm -rf out
find . -name .svn -type d -print0 | xargs -0 rm -rf 

