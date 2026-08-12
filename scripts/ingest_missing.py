import urllib.request, json, time

# Login
login_data = json.dumps({"username":"admin","password":"admin123"}).encode()
req = urllib.request.Request("http://localhost:8082/api/auth/login", data=login_data, headers={"Content-Type":"application/json"})
token = json.loads(urllib.request.urlopen(req).read())["token"]

missing = [3397,3390,3389,3388,3387,3386,3385,3384,3383,3382,3381,3380,3379]
ok = 0
fail = 0
for num in missing:
    req = urllib.request.Request(f"http://localhost:8082/api/admin/ingesta/sorteo/{num}", method="POST", headers={"Authorization": f"Bearer {token}"})
    try:
        resp = json.loads(urllib.request.urlopen(req, timeout=30).read())
        if resp.get("exitoso"):
            ok += 1
            mod = resp.get("modalidadesIngeridas", 0)
            print(f"Sorteo {num}: OK ({mod} modalidades)")
        else:
            fail += 1
            msg = resp.get("mensaje", "?")
            print(f"Sorteo {num}: FAIL - {msg}")
    except Exception as e:
        fail += 1
        print(f"Sorteo {num}: ERROR - {e}")
    time.sleep(0.5)

print(f"\nTotal: {ok} ingestado, {fail} fallos")
