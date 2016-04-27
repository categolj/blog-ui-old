#!/bin/sh
echo y | fly -t do sp -p blog-ui -c pipeline.yml -l ../../credentials.yml
