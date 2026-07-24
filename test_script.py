#!/usr/bin/env python3
import sys
import time

event = sys.argv[1] if len(sys.argv) > 1 else "unknown"
context = sys.argv[2] if len(sys.argv) > 2 else "none"
timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
print(f"[PythemcIO TEST] Event: {event} | Context: {context} | Time: {timestamp}")
