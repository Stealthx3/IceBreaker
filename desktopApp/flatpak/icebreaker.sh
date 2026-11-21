#!/bin/sh
trap 'kill -TERM "$pid" 2>/dev/null' TERM INT
while :; do
    /app/lib/icebreaker/bin/icebreaker "$@" &
    pid=$!
    wait "$pid"
    status=$?
    while kill -0 "$pid" 2>/dev/null; do
        wait "$pid"
        status=$?
    done
    [ "$status" = 75 ] || exit "$status"
done
