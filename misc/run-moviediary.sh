#!/bin/sh

#if is already running -> kill
if [ -f /tmp/moviediary.pid ]; then
	kill $(cat /tmp/moviediary.pid)
	sleep 3
fi

nohup java -jar moviediary.jar > /dev/null 2>error.log &
echo $! > /tmp/moviediary.pid

#if recommender is already running
if [ -f /tmp/recommender.pid ]; then
	kill $(cat /tmp/recommender.pid)
        sleep 3
fi

nohup python3 Server.py > /dev/null 2>recommender-error.log &
echo $! > /tmp/recommender.pid

