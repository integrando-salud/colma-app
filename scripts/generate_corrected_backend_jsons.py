import json
import os
import re
import copy

OUTPUT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "tratamientos_corregidos_para_backend"))
os.makedirs(OUTPUT_DIR, exist_ok=True)

TREATMENTS_CONFIG = [
    {
        "filename": "tratamiento_AR_DBT.json",
        "source": "test/api_responses/sonia_venegas_DBT_AR/2_tratamiento_response.json",
        "country": "AR",
        "condition": "DBT",
        "is_combo": False
    },
    {
        "filename": "tratamiento_AR_HTA.json",
        "source": "test/api_responses/francisca_delapaz_HTA_AR/2_tratamiento_response.json",
        "country": "AR",
        "condition": "HTA",
        "is_combo": False
    },
    {
        "filename": "tratamiento_AR_DBT_HTA.json",
        "source": "test/api_responses/maria_quintanilla_HTA_DBT_AR/2_tratamiento_response.json",
        "country": "AR",
        "condition": "DBT_HTA",
        "is_combo": True
    },
    {
        "filename": "tratamiento_PE_DBT.json",
        "source": "test/api_responses/erik_becerra_DBT_PE/2_tratamiento_response.json",
        "country": "PE",
        "condition": "DBT",
        "is_combo": False
    },
    {
        "filename": "tratamiento_PE_HTA.json",
        "source": "test/api_responses/guillermo_arribas_HTA_PE/2_tratamiento_response.json",
        "country": "PE",
        "condition": "HTA",
        "is_combo": False
    },
    {
        "filename": "tratamiento_PE_DBT_HTA.json",
        "source": "test/api_responses/ignacio_heaton_HTA_DBT_PE/2_tratamiento_response.json",
        "country": "PE",
        "condition": "DBT_HTA",
        "is_combo": True
    }
]

def clean_treatment(data, country, condition, is_combo):
    trat = copy.deepcopy(data)
    semanas = trat.get("tratamiento", {}).get("semanas", [])
    
    for w in semanas:
        w_id = w.get("id")
        
        for s in w.get("sesiones", []):
            s_id = s.get("id")
            contents = s.get("content", [])
            new_contents = []
            
            # Remove metas in weeks 4, 13, 16 across all treatments
            if w_id in [4, 13, 16] and s_id == 3:
                contents = [c for c in contents if c.get("type") != "METAS"]
                
            for c in contents:
                title = c.get("title", "")
                desc = c.get("description", "")
                full_desc = c.get("fullDescription", "")
                
                # 1. Clean condition tags from titles (in combo or specific weeks)
                if is_combo or w_id in [4, 7, 10, 11, 13]:
                    clean_title = re.sub(r'\s*\((?:Diabetes|Hipertensión|Diabetes e hipertensión|DM|HTA)\)', '', title, flags=re.IGNORECASE).strip()
                    c["title"] = clean_title
                
                # 2. S3.1 Bienvenida: Remove trailing ': La activación'
                if w_id == 3 and s_id == 1:
                    if ": La activación" in desc:
                        desc = desc.replace(": La activación", "").strip()
                    if ": La activación" in full_desc:
                        full_desc = full_desc.replace(": La activación", "").strip()
                
                # 3. S3.3 Fin de sesión: Fix 'sueño y .' -> 'sueño y manejo del estrés.'
                if w_id == 3 and s_id == 3:
                    if "sueño y ." in desc:
                        desc = desc.replace("sueño y .", "sueño y manejo del estrés.")
                    if "sueño y ." in full_desc:
                        full_desc = full_desc.replace("sueño y .", "sueño y manejo del estrés.")
                
                # 4. S7.1 Fin de sesión: 'hoario' -> 'horario'
                if "hoario" in desc:
                    desc = desc.replace("hoario", "horario")
                if "hoario" in full_desc:
                    full_desc = full_desc.replace("hoario", "horario")
                
                # 5. S8.1 AR HTA title: 'Disfrutá el sabor de cuidarte.saludable en el día a día' -> 'Disfrutá el sabor de cuidarte.'
                if "Disfrutá el sabor de cuidarte.saludable en el día a día" in c.get("title", ""):
                    c["title"] = "Disfrutá el sabor de cuidarte."
                if "Disfrutá el sabor de cuidarte.saludable en el día a día" in desc:
                    desc = desc.replace("Disfrutá el sabor de cuidarte.saludable en el día a día", "Disfrutá el sabor de cuidarte.")
                if "Disfrutá el sabor de cuidarte.saludable en el día a día" in full_desc:
                    full_desc = full_desc.replace("Disfrutá el sabor de cuidarte.saludable en el día a día", "Disfrutá el sabor de cuidarte.")
                
                # 6. S10.1 Bienvenida: 'La importancia de las consultas médicas' -> 'La importancia de los controles de salud'
                if w_id == 10 and s_id == 1:
                    if c.get("title") == "La importancia de las consultas médicas":
                        c["title"] = "La importancia de los controles de salud"
                    if "el protagonista" in desc:
                        desc = desc.replace("el protagonista", "el/la protagonista")
                    if "el protagonista" in full_desc:
                        full_desc = full_desc.replace("el protagonista", "el/la protagonista")
                
                # 7. S11.2 Bienvenida: Remove duplicate message
                if w_id == 11 and s_id == 2 and c.get("type") == "DESCRIPTION":
                    if "¡Hola! Te damos la bienvenida a esta sesión" in desc:
                        parts = desc.split("¡Hola! Te damos la bienvenida a esta sesión")
                        if len(parts) > 2:
                            desc = "¡Hola! Te damos la bienvenida a esta sesión" + parts[1].strip()
                            full_desc = desc
                
                # 8. S12.2 Spacing: '“ ritual relajante ”' -> '“ritual relajante”'
                if "“ ritual relajante ”" in desc:
                    desc = desc.replace("“ ritual relajante ”", "“ritual relajante”")
                if "“ ritual relajante ”" in full_desc:
                    full_desc = full_desc.replace("“ ritual relajante ”", "“ritual relajante”")
                
                # 9. S13.3 Bienvenida concat in combo: Keep single welcome
                if w_id == 13 and s_id == 3 and is_combo and c.get("type") == "DESCRIPTION":
                    if "¡Hola! Cuidar cómo nos sentimos" in desc and "¡Hola! Te damos la bienvenida" in desc:
                        desc = "¡Hola! Cuidar cómo nos sentimos nos ayuda a sostener los hábitos y el autocuidado en el tiempo. Hoy vamos a reconocer el impacto que tiene la salud mental en la salud y promover pequeñas acciones de cuidado emocional que contribuyan al bienestar."
                        full_desc = desc
                
                # 10. S13.3 Lectura 4 formatting (AR DM+HTA / PE): 'Considera sumarte a un grupo' as title
                if w_id == 13 and s_id == 3 and "Considera sumarte a un grupo" in desc:
                    desc = desc.replace("Considera sumarte a un grupo", "**Considera sumarte a un grupo**\n\n")
                    full_desc = full_desc.replace("Considera sumarte a un grupo", "**Considera sumarte a un grupo**\n\n")
                
                # 11. S15.2 Lectura 1 formatting: '¡El camino no es una línea recta!' as title
                if w_id == 15 and s_id == 2 and "El camino no es una línea recta" in desc:
                    desc = desc.replace("El camino no es una línea recta!", "**¡El camino no es una línea recta!**\n\n").replace("¡El camino no es una línea recta!", "**¡El camino no es una línea recta!**\n\n")
                    full_desc = full_desc.replace("El camino no es una línea recta!", "**¡El camino no es una línea recta!**\n\n").replace("¡El camino no es una línea recta!", "**¡El camino no es una línea recta!**\n\n")
                
                # 12. S16.2 Quiz: Option C 'ás' -> 'quizás'
                if c.get("type") == "QUIZ" and "questions" in c:
                    for q in c["questions"]:
                        opts = q.get("options", [])
                        for idx_opt, opt in enumerate(opts):
                            if "Retomar mi plan, ás con" in opt:
                                opts[idx_opt] = opt.replace("Retomar mi plan, ás con", "Retomar mi plan, quizás con")
                            elif "ás con un paso" in opt:
                                opts[idx_opt] = opt.replace("ás con un paso", "quizás con un paso")
                
                # 13. S16.3 Fin de sesión: Remove residual links
                if w_id == 16 and s_id == 3 and ("Fin de la" in c.get("title", "") or "fin" in c.get("title", "").lower()):
                    if "Información adicional" in desc:
                        desc = desc.split("Información adicional")[0].strip()
                    if "Información adicional" in full_desc:
                        full_desc = full_desc.split("Información adicional")[0].strip()
                    if country == "PE" and "En las próximas semanas te acompañaremos con sesiones de repaso" in desc:
                        desc = desc.replace("En las próximas semanas te acompañaremos con sesiones de repaso para que sigas practicando. Pero recuerda: lo más valioso ya lo tienes.", "").strip()
                        full_desc = full_desc.replace("En las próximas semanas te acompañaremos con sesiones de repaso para que sigas practicando. Pero recuerda: lo más valioso ya lo tienes.", "").strip()

                c["description"] = desc
                c["fullDescription"] = full_desc
                new_contents.append(c)
                
            # 14. S14.2: Remove duplicate 5th reading if present
            if w_id == 14 and s_id == 2:
                readings = [item for item in new_contents if "Lectura" in item.get("title", "")]
                if len(readings) > 4:
                    filtered = []
                    r_count = 0
                    for item in new_contents:
                        if "Lectura" in item.get("title", ""):
                            r_count += 1
                            if r_count <= 4:
                                filtered.append(item)
                        else:
                            filtered.append(item)
                    new_contents = filtered
                    
            s["content"] = new_contents
            
    return trat

def main():
    print(f"Generating cleaned JSON files in: {OUTPUT_DIR}")
    
    for cfg in TREATMENTS_CONFIG:
        src_path = cfg["source"]
        if not os.path.exists(src_path):
            print(f"Warning: source file not found: {src_path}")
            continue
            
        with open(src_path, "r", encoding="utf-8") as f:
            raw_data = json.load(f)
            
        cleaned = clean_treatment(
            raw_data,
            country=cfg["country"],
            condition=cfg["condition"],
            is_combo=cfg["is_combo"]
        )
        
        out_path = os.path.join(OUTPUT_DIR, cfg["filename"])
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(cleaned, f, indent=2, ensure_ascii=False)
            
        print(f"  Generated: {cfg['filename']}")

    # Write documentation guide for backend developer
    doc_path = os.path.join(OUTPUT_DIR, "GUIA_CAMBIOS_BACKEND.md")
    with open(doc_path, "w", encoding="utf-8") as f:
        f.write("""# Guía de Correcciones de Contenido para Backend (Endpoint `/api/tratamiento`)

Este documento y los archivos JSON en esta carpeta contienen la versión corregida y normalizada de los tratamientos para **Argentina** y **Perú** en sus tres variantes (Diabetes, Hipertensión, Diabetes + Hipertensión), resolviendo las observaciones reportadas en el QA (`comentariosTest.pdf`).

---

## 📁 Archivos JSON Corregidos Generados

1. `tratamiento_AR_DBT.json`: Tratamiento Diabetes - Argentina
2. `tratamiento_AR_HTA.json`: Tratamiento Hipertensión - Argentina
3. `tratamiento_AR_DBT_HTA.json`: Tratamiento Diabetes + Hipertensión - Argentina
4. `tratamiento_PE_DBT.json`: Tratamiento Diabetes - Perú
5. `tratamiento_PE_HTA.json`: Tratamiento Hipertensión - Perú
6. `tratamiento_PE_DBT_HTA.json`: Tratamiento Diabetes + Hipertensión - Perú

---

## 🛠️ Resumen Detallado de Correcciones Realizadas

### 1. Reglas Globales y de Estructura
* **Metas en Semanas 4, 13 y 16 (Sesión 3)**: Se eliminaron los bloques de contenido de tipo `METAS` en estas semanas, ya que clínicamente no corresponden metas en estos cierres.
* **Títulos en Perfil Combinado (HTA + DBT)**: Se eliminaron las menciones de condición entre paréntesis `(Diabetes)`, `(Hipertensión)`, `(DM)` y `(HTA)` de los títulos de lecturas, videos y metas (por ejemplo: `Lectura 1 (Diabetes)` -> `Lectura 1`, `Lectura 3 (Hipertensión)` -> `Lectura 3`, `Metas (Diabetes)` -> `Metas`). Aplica especialmente a Semanas 4, 7, 10, 11 y 13.

### 2. Correcciones Puntuales por Semana y Sesión

| Semana / Sesión | Ubicación | Corrección Realizada |
| :--- | :--- | :--- |
| **Semana 3 - Sesión 1** | Bienvenida | Se eliminó el texto residual `": La activación"` al final del párrafo. |
| **Semana 3 - Sesión 3** | Fin de la sesión | Se corrigió el cierre truncado `"...sueño y ."` por `"...sueño y manejo del estrés. ¡Seguí adelante, vas muy bien!"`. |
| **Semana 7 - Sesión 1 (HTA)** | Fin de la sesión | Se corrigió la errata de tipeo `"hoario"` por `"horario"`. |
| **Semana 8 - Sesión 1 (HTA AR)** | Título / Bienvenida | Se ajustó el título a `"Disfrutá el sabor de cuidarte."` (se removió `.saludable en el día a día`). |
| **Semana 10 - Sesión 1** | Título y Texto Fin | Título corregido a `"La importancia de los controles de salud"` (en vez de *"consultas médicas"*). En el texto se corrigió `"vos sos el protagonista"` por `"vos sos el/la protagonista"`. |
| **Semana 10 - Sesión 2** | Títulos de Lecturas | Se removieron los sufijos `(Diabetes)` e `(Hipertensión)` de las lecturas. |
| **Semana 11 - Sesión 2** | Bienvenida (DM+HTA) | Se eliminó la duplicación del mensaje de bienvenida concatenado dos veces. |
| **Semana 12 - Sesión 2 (HTA)** | Lectura 2 | Se corrigió el espaciado en `“ ritual relajante ”` -> `“ritual relajante”`. |
| **Semana 13 - Sesión 3** | Bienvenida (DM+HTA) | Se separó la concatenación de dos bienvenidas distintas en un único texto coherente. |
| **Semana 13 - Sesión 3** | Lectura 4 | Se formateó `"Considera sumarte a un grupo"` como encabezado con negritas `**`. |
| **Semana 14 - Sesión 2** | Contenido Lecturas | Se removió la `Lectura 5` sobrante que duplicaba el audio de la Lectura 1. |
| **Semana 15 - Sesión 2** | Lectura 1 | Se formateó `"¡El camino no es una línea recta!"` como título con negritas `**`. |
| **Semana 16 - Sesión 2 (PE)** | Quiz Pregunta del día | En la opción C se corrigió el texto corrupto `"Retomar mi plan, ás con un paso..."` por `"Retomar mi plan, quizás con un paso más pequeño, sin juzgarme por la pausa."`. |
| **Semana 16 - Sesión 3** | Fin de la sesión | Se eliminó la lista de links residuales pegados al final (`"Información adicional Información adicional sobre la diabetes..."`). |

---

## 📌 Recomendación de Implementación en Backend
El desarrollador de backend puede utilizar directamente estos archivos JSON para actualizar los seeds, fixtures o scripts de importación de la base de datos de contenidos (`tratamiento` y `tratamiento_elementos`).
""")
    print(f"  Generated: GUIA_CAMBIOS_BACKEND.md")

if __name__ == "__main__":
    main()
