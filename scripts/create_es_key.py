import urllib.request, json, base64

auth = base64.b64encode(b"elastic:quini6_dev_2026").decode()
data = json.dumps({
    "name": "quini6-app",
    "role_descriptors": {
        "quini6_role": {
            "cluster": ["monitor", "manage_index_templates"],
            "indices": [
                {
                    "names": ["quini6-*"],
                    "privileges": ["all"]
                }
            ]
        }
    }
}).encode()

req = urllib.request.Request(
    "http://localhost:9200/_security/api_key",
    data=data,
    headers={"Content-Type": "application/json", "Authorization": f"Basic {auth}"},
    method="POST"
)
try:
    resp = json.loads(urllib.request.urlopen(req).read())
    encoded = resp["encoded"]
    
    print("=== API Key Creada ===")
    print(f"Name:     quini6-app")
    print(f"ID:       {resp['id']}")
    print(f"API Key:  {resp['api_key']}")
    print(f"Encoded:  {encoded}")
    print()
    print("=== Para usar en la app ===")
    print(f"Header:   Authorization: ApiKey {encoded}")
    print()
    
    # Test it
    test_req = urllib.request.Request(
        "http://localhost:9200/_cat/indices?v",
        headers={"Authorization": f"ApiKey {encoded}"}
    )
    result = urllib.request.urlopen(test_req).read().decode()
    print("=== Test ===")
    print(result)
except urllib.error.HTTPError as e:
    print(f"Error {e.code}: {e.read().decode()}")
