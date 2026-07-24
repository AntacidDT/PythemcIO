#!/usr/bin/env python3
import urllib.request
import json

req = urllib.request.Request("https://catfact.ninja/fact", headers={"User-Agent": "Mozilla/5.0"})
data = urllib.request.urlopen(req).read()
fact = json.loads(data)["fact"]
print(f"[Cat Fact] {fact}")
