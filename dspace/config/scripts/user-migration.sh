#!/bin/bash

#Login 
email=$1

#Plain passowrd
password=$2

#Firstname + Lastname
fullname=$3

sh /var/lib/tomcat7/webapps/dspace/bin/config/scripts/inforex-user-migration.sh $email $password $fullname
sh /var/lib/tomcat7/webapps/dspace/bin/config/scripts/wielowyr-user-migration.sh $email $password $fullname
