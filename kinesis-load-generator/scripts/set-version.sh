

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/../


mvn versions:set -DnewVersion=1.2

#CloudFormation - template
#  change download path

#reference in start script
#refernce in package
#reference in package-upload
#change upload folder
