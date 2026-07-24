#!/usr/bin/env python3
import sys
import time

event = sys.argv[1] if len(sys.argv) > 1 else "unknown"
timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
print(f"[PythemcIO TEST] Event: {event} | Time: {timestamp}")
