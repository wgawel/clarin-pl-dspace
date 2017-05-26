#!/bin/bash

#Login 
email=$1

#Plain passowrd
password=$2

#Firstname + Lastname
fullname=$3

db_host=10.17.0.80
db_user=kuznia
db_name=kuznia

PGPASSWORD=kuznia psql -h $db_host -p 5432 -U $db_user -d $db_name -c "select add_user('$email','$password','$email','$fullname')"
