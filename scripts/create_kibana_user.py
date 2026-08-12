import urllib.request, json, base64

auth = base64.b64encode(b"elastic:quini6_dev_2026").decode()
headers = {"Content-Type": "application/json", "Authorization": f"Basic {auth}"}

# Create kibana_system user
data = json.dumps({"password": "quini6_dev_2026", "roles": ["kibana_system"]}).encode()
req = urllib.request.Request("http://localhost:9200/_security/user/kibana_system", data=data, headers=headers, method="POST")
try:
    resp = urllib.request.urlopen(req)
    print("1. User created:", json.loads(resp.read()))
except urllib.error.HTTPError as e:
    body = e.read().decode()
    if "already exists" in body:
        print("1. User already exists, setting password...")
    else:
        print(f"1. Error: {body}")

# Set password
pw_data = json.dumps({"password": "quini6_dev_2026"}).encode()
req2 = urllib.request.Request("http://localhost:9200/_security/user/kibana_system/_password", data=pw_data, headers=headers, method="POST")
try:
    resp2 = urllib.request.urlopen(req2)
    print("2. Password set:", json.loads(resp2.read()))
except urllib.error.HTTPError as e:
    print(f"2. Error: {e.read().decode()}")

print("\nDone. Restart Kibana with kibana_system user.")
