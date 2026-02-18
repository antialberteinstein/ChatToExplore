#!/bin/bash

echo "Building chat_executor..."
mvn package -DskipTests -q

if [ $? -eq 0 ]; then
    echo "Build thành công!"
else
    echo "Build thất bại!"
    exit 1
fi
