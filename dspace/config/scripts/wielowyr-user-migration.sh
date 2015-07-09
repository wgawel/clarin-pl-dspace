#!/bin/bash

#Login 
email=$1

#Plain passowrd
password=$2

#Firstname + Lastname
fullname=$3

db_host=mewex.clarin-pl.eu
db_user=tnaskret
db_pass=!tnaskret123##
db_name=wielowyr

"$(mysql -sN --host=$db_host -u $db_user -p$db_pass -D $db_name -e 'select add_user("'"$email"'","'"$password"'")')"
