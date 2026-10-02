import os
import json
import urllib.request
import urllib.parse
import urllib.error

BASE_URL = "http://158.69.219.243"
OUTPUT_DIRS = [
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "test", "api_responses")),
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "app", "app", "test", "api_responses")),
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "app", "test", "api_responses")),
]

USERS = [
    {
        "folder": "sonia_venegas_DBT_AR",
        "name": "Sonia Venegas",
        "country": "AR",
        "condition": "DBT",
        "dni": "03843296",
        "password": "123456"
    },
    {
        "folder": "maria_quintanilla_HTA_DBT_AR",
        "name": "Maria Quintanilla",
        "country": "AR",
        "condition": "HTA + DBT",
        "dni": "50697085",
        "password": "123456"
    },
    {
        "folder": "francisca_delapaz_HTA_AR",
        "name": "Francisca Delapaz",
        "country": "AR",
        "condition": "HTA",
        "dni": "44876430",
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
    },
    {
        "folder": "erik_becerra_DBT_PE",
        "name": "Erik Becerra",
        "country": "PE",
        "condition": "DBT",
        "dni": "81306555",
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
        "folder": "ignacio_heaton_HTA_DBT_PE",
        "name": "Ignacio Heaton",
        "country": "PE",
        "condition": "HTA + DBT",
        "dni": "98765432",
        "password": "123456"
    },
    {
        "folder": "guillermo_arribas_HTA_PE",
        "name": "Guillermo Arribas",
        "country": "PE",
        "condition": "HTA",
        "dni": "15864485",
        "password": "123456"
    },
    {
        "folder": "denisse_molina_DBT_HTA_PE",
        "name": "Denisse Molina",
        "country": "PE",
        "condition": "DBT + HTA",
        "dni": "12253242",
        "password": "123456"
    }
]

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

def save_json(rel_path, data):
    for base_dir in OUTPUT_DIRS:
        full_path = os.path.join(base_dir, rel_path)
        os.makedirs(os.path.dirname(full_path), exist_ok=True)
        with open(full_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)

def main():
    for d in OUTPUT_DIRS:
        os.makedirs(d, exist_ok=True)
    print(f"Saving API responses to: {OUTPUT_DIRS}")

    summary = []

    for user in USERS:
        print(f"\nProcessing user: {user['name']} ({user['country']} - {user['condition']})...")

        # 1. Login
        login_url = f"{BASE_URL}/api/login"
        login_payload = {"documento": user["dni"], "password": user["password"]}
        code_login, body_login = make_request(login_url, method="POST", data=login_payload, is_form=True)
        
        save_json(os.path.join(user["folder"], "1_login_response.json"), body_login)
        print(f"  [1] Login (Status {code_login})")

        token = body_login.get("token")
        if not token:
            print(f"  ERROR: No token obtained for {user['name']}")
            continue

        auth_headers = {"Authorization": f"Bearer {token}"}

        # 2. Tratamiento
        tratamiento_url = f"{BASE_URL}/api/tratamiento"
        code_trat, body_trat = make_request(tratamiento_url, method="GET", headers=auth_headers)
        save_json(os.path.join(user["folder"], "2_tratamiento_response.json"), body_trat)
        print(f"  [2] Tratamiento (Status {code_trat})")

        # 3. Elementos Completados (Nuevo Endpoint)
        completados_url = f"{BASE_URL}/api/elementos-completados"
        code_comp, body_comp = make_request(completados_url, method="GET", headers=auth_headers)
        save_json(os.path.join(user["folder"], "3_elementos_completados_response.json"), body_comp)
        print(f"  [3] Elementos Completados (Status {code_comp})")

        # 4. Datos Funcionales Tratamiento (Nuevo Endpoint)
        funcionales_url = f"{BASE_URL}/api/datos-funcionales-tratamiento"
        code_func, body_func = make_request(funcionales_url, method="GET", headers=auth_headers)
        save_json(os.path.join(user["folder"], "4_datos_funcionales_response.json"), body_func)
        print(f"  [4] Datos Funcionales (Status {code_func})")

        # Count stats from tratamiento
        semanas = body_trat.get("tratamiento", {}).get("semanas", [])
        total_items = 0
        items_with_elemento_id = 0
        type_counts = {}
        for s in semanas:
            for ses in s.get("sesiones", []):
                for c in ses.get("content", []):
                    total_items += 1
                    t = c.get("type", "UNKNOWN")
                    type_counts[t] = type_counts.get(t, 0) + 1
                    if "tratamiento_elemento_id" in c and c["tratamiento_elemento_id"] is not None:
                        items_with_elemento_id += 1

        summary.append({
            "usuario": user["name"],
            "pais": user["country"],
            "condicion": user["condition"],
            "dni": user["dni"],
            "login_status": code_login,
            "tratamiento_status": code_trat,
            "elementos_completados_status": code_comp,
            "datos_funcionales_status": code_func,
            "total_items_content": total_items,
            "items_con_tratamiento_elemento_id": items_with_elemento_id,
            "distribucion_tipos": type_counts,
            "elementos_completados_recibidos": body_comp.get("elementos_completados", []),
            "directorio": user["folder"]
        })

    save_json("summary_all_responses.json", summary)
    print(f"\nResumen guardado exitosamente en todos los directorios.")

if __name__ == "__main__":
    main()
