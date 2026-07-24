#!/usr/bin/env python3
import urllib.request
import json
import sys

API_URL = "http://127.0.0.1:8080/event"
API_KEY = "pythemcio"

event = sys.argv[1] if len(sys.argv) > 1 else "send_chat"
message = sys.argv[2] if len(sys.argv) > 2 else "Hello from Python!"

payload = json.dumps({
    "event": event,
    "message": message,
    "command": message,
    "title": message,
    "subtitle": message,
    "text": message,
    "api_key": API_KEY
}).encode("utf-8")

req = urllib.request.Request(API_URL, data=payload, headers={
    "Content-Type": "application/json",
    "X-Api-Key": API_KEY
})

try:
    response = urllib.request.urlopen(req).read()
    data = json.loads(response)
    print(f"[PythemcIO] Response: {data['status']} - {data.get('action', data.get('message', ''))}")
except Exception as e:
    print(f"[PythemcIO] Error: {e}")
