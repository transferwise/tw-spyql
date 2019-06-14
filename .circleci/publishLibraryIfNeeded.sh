#!/bin/bash

GRADLE_PROPERTY_FILE_FOR_API_LIB=gradle.properties
ARTIFACTORY_URL="https://arti.tw.ee/artifactory/libs-release-local/com/transferwise/common"

getHttpStatusCodeOfUrl() {
  url=$1
  statusCode="$(curl -o /dev/null --silent --head --write-out '%{http_code}\n' $url)"
  echo $statusCode
}

isLibAlreadyPublishedForVersion() {
  artifact_name=$1
  version=$2
  fullPathToVersion="$ARTIFACTORY_URL/$artifact_name/$version"
  [ $(getHttpStatusCodeOfUrl $fullPathToVersion) -eq 302 ]
}

getPropertyFromFile() {
   propKey=$1
   propertyFile=$2
   propValue=`cat $propertyFile | grep "$propKey" | cut -d'=' -f2`
   echo $propValue
}

isLibAlreadyPublished() {
  artifact_name=$1
  version=$(getPropertyFromFile version $GRADLE_PROPERTY_FILE_FOR_API_LIB)
  echo "Checking if $artifact_name:$version is already published ...";
  isLibAlreadyPublishedForVersion $artifact_name $version;
}

execute() {
    artifact_name=$1
    if isLibAlreadyPublished $artifact_name;
    then
      echo "Library $1:$version is already published";
    else
      echo "Library $1:$version is NOT published, publishing...";
      ./gradlew :$1:clean :$1:assemble :$1:publish
    fi
}

execute $1
