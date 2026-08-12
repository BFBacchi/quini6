import urllib.request, json
resp = urllib.request.urlopen("http://localhost:9090/api/v1/targets")
d = json.loads(resp.read())
for t in d['data']['activeTargets']:
    job = t['labels'].get('job', '?')
    health = t['health']
    err = t.get('lastError', '')[:80]
    print(f"  {job}: {health} {err}")
