#!/usr/bin/env sh

xargs -rt -a /service_dataset/application.pid kill -SIGTERM
sleep 29
