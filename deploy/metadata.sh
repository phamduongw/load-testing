#!/bin/bash

# JDBC
export JDBC_DRIVER="com.mysql.cj.jdbc.Driver"
export JDBC_URL="jdbc:mysql://10.10.12.238:3306"
export JDBC_USERNAME="dbadmin"
export JDBC_PASSWORD="oracle_4U"

# CONN
export CONN_COUNT="10"
# export LOOP_COUNT="100"

# TEST_CASE
export SELECT_QUERY="SELECT * FROM CX6_AUDITING.TEST"
export SELECT_WEIGHT="0.25"

export INSERT_QUERY="INSERT INTO cx6_auditing.test (NAME) VALUES ('Example Name')"
export INSERT_WEIGHT="0.25"

export UPDATE_QUERY="UPDATE cx6_auditing.test SET NAME='ABC' WHERE ID = '10'"
export UPDATE_WEIGHT="0.25"

export DELETE_QUERY="DELETE FROM cx6_auditing.test WHERE ID = '100'"
export DELETE_WEIGHT="0.25"
