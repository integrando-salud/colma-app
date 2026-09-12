import os
import json
import urllib.request
import urllib.error

BASE_URL = "http://158.69.219.243"
OUTPUT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "test", "api_responses"))

USERS = [
    {
        "folder": "guillermo_arribas_HTA_PE",
        "name": "Guillermo Arribas",
        "country": "PE",
        "condition": "HTA",
        "dni": "15864485",
        "password": "123456"
    },
    {
        "folder": "erika_becerra_DBT_PE",
        "name": "Erika Becerra",
        "country": "PE",
        "condition": "DBT",
        "dni": "81306555",
        "password": "123456"
    },
    {
        "folder": "denisse_molina_DBT_HTA_PE",
        "name": "Denisse Molina",
        "country": "PE",
        "condition": "DBT + HTA",
        "dni": "12253242",
        "password": "123456"
    },
    {
        "folder": "maria_gonzalez_HTA_AR",
        "name": "María González",
        "country": "AR",
        "condition": "HTA",
        "dni": "88888888",
        "password": "123456"
    },
    {
        "folder": "pedro_flores_DBT_HTA_AR",
        "name": "Pedro Flores",
        "country": "AR",
        "condition": "DBT + HTA",
        "dni": "77777777",
        "password": "123456"
    }
]

import urllib.parse

def make_request(url, method="GET", headers=None, data=None, is_form=False):
    if headers is None:
        headers = {}
    
    req_data = None
    if data is not None:
        if is_form:
            req_data = urllib.parse.urlencode(data).encode("utf-8")
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        else:
            req_data = json.dumps(data).encode("utf-8")
            headers["Content-Type"] = "application/json"
    
    headers["Accept"] = "application/json"
    req = urllib.request.Request(url, data=req_data, headers=headers, method=method)
    
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            status_code = resp.status
            body_bytes = resp.read()
            body_str = body_bytes.decode("utf-8")
            try:
                body_json = json.loads(body_str)
            except Exception:
                body_json = {"raw": body_str}
            return status_code, body_json
    except urllib.error.HTTPError as e:
        body_bytes = e.read()
        body_str = body_bytes.decode("utf-8")
        try:
            body_json = json.loads(body_str)
        except Exception:
            body_json = {"raw": body_str}
        return e.code, body_json

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"Saving API responses to: {OUTPUT_DIR}")

    summary = []

    for user in USERS:
        user_dir = os.path.join(OUTPUT_DIR, user["folder"])
        os.makedirs(user_dir, exist_ok=True)
        print(f"\nProcessing user: {user['name']} ({user['country']} - {user['condition']})...")

        # 1. Login (FormUrlEncoded with 'documento' and 'password')
        login_url = f"{BASE_URL}/api/login"
        login_payload = {"documento": user["dni"], "password": user["password"]}
        code_login, body_login = make_request(login_url, method="POST", data=login_payload, is_form=True)

        
        login_file = os.path.join(user_dir, "1_login_response.json")
        with open(login_file, "w", encoding="utf-8") as f:
            json.dump(body_login, f, indent=2, ensure_ascii=False)
        print(f"  [1] Login (Status {code_login}) -> {login_file}")

        token = body_login.get("token")
        if not token:
            print(f"  ERROR: No token obtained for {user['name']}")
            continue

        auth_headers = {"Authorization": f"Bearer {token}"}

        # 2. Tratamiento
        tratamiento_url = f"{BASE_URL}/api/tratamiento"
        code_trat, body_trat = make_request(tratamiento_url, method="GET", headers=auth_headers)
        trat_file = os.path.join(user_dir, "2_tratamiento_response.json")
        with open(trat_file, "w", encoding="utf-8") as f:
            json.dump(body_trat, f, indent=2, ensure_ascii=False)
        print(f"  [2] Tratamiento (Status {code_trat}) -> {trat_file}")

        # 3. Multimedia Completados
        completados_url = f"{BASE_URL}/api/multimedia-completados"
        code_comp, body_comp = make_request(completados_url, method="GET", headers=auth_headers)
        comp_file = os.path.join(user_dir, "3_multimedia_completados_response.json")
        with open(comp_file, "w", encoding="utf-8") as f:
            json.dump(body_comp, f, indent=2, ensure_ascii=False)
        print(f"  [3] Multimedia Completados (Status {code_comp}) -> {comp_file}")

        # Count stats from tratamiento
        semanas = body_trat.get("tratamiento", {}).get("semanas", [])
        total_items = 0
        items_with_multimedia_id = 0
        for s in semanas:
            for ses in s.get("sesiones", []):
                for c in ses.get("content", []):
                    total_items += 1
                    if "tratamiento_multimedia_id" in c and c["tratamiento_multimedia_id"] is not None:
                        items_with_multimedia_id += 1

        summary.append({
            "usuario": user["name"],
            "pais": user["country"],
            "condicion": user["condition"],
            "dni": user["dni"],
            "login_status": code_login,
            "tratamiento_status": code_trat,
            "multimedia_completados_status": code_comp,
            "total_items_multimedia": total_items,
            "items_con_tratamiento_multimedia_id": items_with_multimedia_id,
            "multimedia_completados_recibidos": body_comp.get("multimedia_completados", []),
            "directorio": user["folder"]
        })

    summary_file = os.path.join(OUTPUT_DIR, "summary_all_responses.json")
    with open(summary_file, "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)
    print(f"\nResumen guardado en: {summary_file}")

if __name__ == "__main__":
    main()
