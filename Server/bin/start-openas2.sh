#!/bin/sh
# purpose: runs the OpenAS2 application     
x=`basename $0`

keyStorePwd=$1
PWD_OVERRIDE=""
# Uncomment any of the following for enhanced debug
#EXTRA_PARMS="$EXTRA_PARMS -Dmaillogger.debug.enabled=true"
#EXTRA_PARMS="$EXTRA_PARMS -DlogRxdMsgMimeBodyParts=true"
#EXTRA_PARMS="$EXTRA_PARMS -DlogRxdMdnMimeBodyParts=true"

if [  ! -z $keyStorePwd ]; then
  PWD_OVERRIDE="-Dorg.openas2.cert.Password=$keyStorePwd"
fi
if [ -z $JAVA_HOME ]; then
  OS=$(uname -s)

  if [[ "${OS}" == *Darwin* ]]; then
    # Mac OS X platform
    JAVA_HOME=$(/usr/libexec/java_home)
  elif [[ "${OS}" == *Linux* ]]; then
    # Linux platform
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
  elif [[ "${OS}" == *MINGW* ]]; then
    # Windows NT platform
    echo "Windows not supported by this script"
  fi
fi

if [ -z $JAVA_HOME ]; then
  echo "ERROR: Cannot find JAVA_HOME"
  exit
fi
JAVA_EXE=$JAVA_HOME/bin/java 
#    
# remove -Dorg.apache.commons.logging.Log=org.openas2.logging.Log if using another logging package    
#
$JAVA_EXE ${PWD_OVERRIDE} -Xms32m -Xmx384m -Dorg.apache.commons.logging.Log=org.openas2.logging.Log -DCmdProcessorSocketCipher=SSL_DH_anon_WITH_RC4_128_MD5  -jar ../lib/openas2-server.jar ../config/config.xml
