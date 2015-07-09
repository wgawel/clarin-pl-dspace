#!/bin/bash

#Login 
email=$1

#Plain passowrd
password=$2

#Firstname + Lastname
#Firstname + Lastname
if [ -z "$3" ]; then
 fullname=$email
else
 fullname=$3
fi

db_host=10.17.0.70
db_user=inforex
db_pass=inforex1qaz

CHECK_EXISTS="$(mysql -sN --host=$db_host -u $db_user -p$db_pass -e 'select exists (select 1 from inforex.users where login="'"$email"'")')"
echo $CHECK_EXISTS
if [ $CHECK_EXISTS = 1 ]
then
 mysql --host=$db_host -u $db_user -p$db_pass -e "UPDATE inforex.users set password=MD5('$password') where login='$email'"
else
 mysql --host=$db_host -u $db_user -p$db_pass -e "INSERT INTO inforex.users (login,password,email,screename) VALUES('$email',MD5('$password'),'$email','$fullname')"
fi
