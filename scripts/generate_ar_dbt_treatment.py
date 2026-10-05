#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_ar_dbt_treatment.py

Generates the corrected JSON content for Argentina Diabetes (DBT) treatment in ColmaApp.
Parses `app/src/main/assets/desings/Contenidos Argentina  - Diabetes.docx.md` table row by row
and reads reading markdown files from `app/src/main/assets/desings/lecturas_hta_dbt/`.

Synchronizes across:
- tratamientos_corregidos_para_backend/tratamiento_AR_DBT.json
- app/src/main/assets/mock_treatment_ar_dbt.json
- app/test/api_responses/sonia_venegas_DBT_AR/2_tratamiento_response.json
- test/api_responses/sonia_venegas_DBT_AR/2_tratamiento_response.json
- app/app/test/api_responses/sonia_venegas_DBT_AR/2_tratamiento_response.json
"""

import json
import os
import re
import urllib.parse

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DOCX_MD_PATH = os.path.join(BASE_DIR, "app/src/main/assets/desings/Contenidos Argentina  - Diabetes.docx.md")
LECTURAS_DIR = os.path.join(BASE_DIR, "app/src/main/assets/desings/lecturas_hta_dbt")

DESTINATION_FILES = [
    os.path.join(BASE_DIR, "tratamientos_corregidos_para_backend/tratamiento_AR_DBT.json"),
    os.path.join(BASE_DIR, "app/src/main/assets/mock_treatment_ar_dbt.json"),
    os.path.join(BASE_DIR, "app/test/api_responses/sonia_venegas_DBT_AR/2_tratamiento_response.json"),
    os.path.join(BASE_DIR, "test/api_responses/sonia_venegas_DBT_AR/2_tratamiento_response.json"),
    os.path.join(BASE_DIR, "app/app/test/api_responses/sonia_venegas_DBT_AR/2_tratamiento_response.json"),
]

def clean_text(raw):
    """
    Cleans general markdown table text for Bienvenidas / Fin de sesión:
    - Unescapes backslash-escaped punctuation (e.g. \\!, \\., \\(, \\), etc.)
    - Converts vertical tabs (\\x0b) and manual breaks to double newlines (\\n\\n)
    - Trims whitespace line by line and collapses redundant empty lines
    """
    t = re.sub(r"\\([^\\])", r"\1", raw)
    t = re.sub(r"[\x0b\r\n]+", "\n\n", t)
    lines = [l.strip() for l in t.split("\n")]
    result = []
    prev_empty = False
    for l in lines:
        if not l:
            if not prev_empty:
                result.append("")
                prev_empty = True
        else:
            result.append(l)
            prev_empty = False
    return "\n".join(result).strip()

def clean_reading_md(content):
    """
    Cleans markdown reading files:
    - Unescapes punctuation like \\!, \\., \\(, \\)
    - Removes asterisks inside quotes (e.g. “*Texto*” -> “Texto”) so Compose doesn't show asterisks
    - Preserves bold ** and list bullets *
    - Normalizes line breaks and whitespace
    """
    text = re.sub(r"\\([^\\])", r"\1", content)
    # Clean italics inside typographic and regular quotes
    text = text.replace("“*", "“").replace("*”", "”")
    text = text.replace('"*', '"').replace('*"', '"')
    
    lines = [l.rstrip() for l in text.splitlines()]
    result = []
    prev_empty = False
    for l in lines:
        if not l.strip():
            if not prev_empty:
                result.append("")
                prev_empty = True
        else:
            result.append(l)
            prev_empty = False
    return "\n".join(result).strip()

def parse_quiz(raw_text):
    """
    Extracts questionText, options list, and correctAnswerIndex from Quiz table cell.
    """
    t = re.sub(r"\\([^\\])", r"\1", raw_text)
    t = re.sub(r"[\x0b\r\n]+", "\n", t)
    
    # Extract correct answer
    ans_match = re.search(r"\*Respuesta correcta:\s*([^*]+)\*", t)
    if not ans_match:
        ans_match = re.search(r"Respuesta correcta:\s*([^\n\.]+)", t)
    correct_str = ans_match.group(1).strip().rstrip(".") if ans_match else ""
    
    # Remove the correct answer clause
    body = t[:ans_match.start()].strip() if ans_match else t.strip()
    
    # True / False check
    if "Verdadero" in body and "Falso" in body and ("- Verdadero" in body or "-Verdadero" in body or ("A:" not in body and "A." not in body and "A)" not in body)):
        q_match = re.split(r"-\s*Verdadero", body)
        q_text = q_match[0].strip()
        options = ["Verdadero", "Falso"]
        correct_idx = 0 if "verdadero" in correct_str.lower() else 1
        return q_text, options, correct_idx
    
    # Multiple choice A / B / C
    parts = re.split(r"(?:^|\s+)([A-C])[\.:\)]\s*", body)
    q_text = parts[0].strip()
    options = []
    for i in range(1, len(parts), 2):
        options.append(parts[i+1].strip())
    
    letter_map = {"A": 0, "B": 1, "C": 2}
    correct_idx = letter_map.get(correct_str.upper(), 0)
    return q_text, options, correct_idx

def parse_metas(raw_text):
    """
    Extracts individual meta options from Metas table cell.
    Handles numbered lists (1. ... 10.), semicolon/newline separated, or space separated.
    """
    t = re.sub(r"\\([^\\])", r"\1", raw_text)
    
    # Numbered: 1. ... 2. ...
    if re.search(r"(?:^|\x0b|\n|\s)\s*1\.\s+", t):
        items = re.split(r"(?:^|\x0b|\n|\s)\s*\d+\.\s+", t)
        options = [re.sub(r"[\x0b\r\n\s]+", " ", it).strip() for it in items if it.strip()]
        return options
    
    # Check for specific unnumbered diabetes health metas (S10.3 and S11.3)
    if "Tomar todos mis medicamentos" in t and "Organizar el pastillero" in t:
        diabetes_health_metas = [
            "Tomar todos mis medicamentos del día como me los indicó el médico",
            "Organizar el pastillero para toda la semana",
            "Medir mi glucosa/azúcar y anotar el resultado",
            "Revisar mis pies con un espejo para ver que no estén lastimados",
            "Anotar dudas o síntomas en un papel para llevar a mi próxima consulta médica",
            "Agendar mi próxima consulta médica si me corresponde un control",
            "Revisar cuántos medicamentos me quedan y anotar la fecha para ir a solicitar más al centro de salud",
            "Aplicar crema humectante en los pies",
            "Poner una alarma en el celular para tomar mi medicación"
        ]
        return diabetes_health_metas
    
    # Check vertical tabs
    if "\x0b" in t:
        items = t.split("\x0b")
        options = [re.sub(r"\s+", " ", it).strip() for it in items if it.strip()]
        if len(options) >= 5:
            return options
    
    # Fallback to splitting by 2 or more spaces
    items = re.split(r"\s{2,}", t)
    options = [re.sub(r"\s+", " ", it).strip() for it in items if it.strip()]
    return options

def get_reading_filename(cell_content):
    """
    Extracts the .docx filename from [Texto](url) and returns the matching .docx.md filename.
    """
    m = re.search(r"\[Texto\]\((.*?\.docx.*?)\)", cell_content)
    if not m:
        raise ValueError(f"Could not find [Texto](...) link in cell: {cell_content[:80]}")
    url = m.group(1).replace(r"\(", "(").replace(r"\)", ")")
    unquoted = urllib.parse.unquote(url)
    m_doc = re.search(r"Texto/([^/?#]+\.docx)", unquoted)
    if not m_doc:
        raise ValueError(f"Could not extract docx name from: {unquoted}")
    doc_name = m_doc.group(1)
    return f"{doc_name}.md"

def build_treatment():
    # 1. Read table lines
    with open(DOCX_MD_PATH, "r", encoding="utf-8") as f:
        lines = [l.strip() for l in f if l.strip().startswith("|")]
    
    rows = []
    for l in lines:
        parts = [p.strip() for p in l.split("|")[1:-1]]
        if len(parts) >= 6 and parts[0].startswith("Semana "):
            if "Texto que va en la app" in parts[0] or ":----" in parts[0]:
                continue
            rows.append(parts)
            
    # Group rows by (week_id, session_id)
    table_by_ws = {}
    week_titles = {}
    for r in rows:
        m_w = re.match(r"Semana\s+(\d+)", r[0])
        w_id = int(m_w.group(1))
        week_titles[w_id] = r[0].strip()
        m_s = re.match(r"Sesión\s+(\d+)", r[2])
        s_id = int(m_s.group(1))
        table_by_ws.setdefault((w_id, s_id), []).append(r)
        
    semanas = []
    current_element_id = 208
    
    for w_id in range(1, 17):
        week_title = week_titles.get(w_id, f"Semana {w_id}")
        sesiones = []
        
        for s_id in range(1, 4):
            session_title = f"Sesión {s_id}"
            raw_items = table_by_ws.get((w_id, s_id), [])
            content_items = []
            
            video_idx = 0
            reading_idx = 0
            
            for item in raw_items:
                col_type = item[3]
                col_title = item[4].replace("**", "").strip()
                col_content = item[5]
                
                if "Bienvenida" in col_type:
                    cleaned_desc = clean_text(col_content)
                    elem = {
                        "type": "DESCRIPTION",
                        "title": col_title,
                        "iconResId": "outline_unknown_document_24",
                        "description": cleaned_desc,
                        "fullDescription": cleaned_desc,
                        "tratamiento_elemento_id": current_element_id
                    }
                    content_items.append(elem)
                    current_element_id += 1
                    
                elif "Video" in col_type:
                    video_idx += 1
                    video_url = f"http://158.69.219.243/api/multimedia/tratamientos/AR/DBT/semana-{w_id}/sesion-{s_id}/video-{video_idx}.mp4"
                    # Handle custom recipe video titles if present
                    if "Receta 1" in col_title:
                        v_title = "Video Receta 1"
                    elif "Receta 2" in col_title:
                        v_title = "Video Receta 2"
                    else:
                        v_title = "Video"
                        
                    elem = {
                        "type": "VIDEO",
                        "title": v_title,
                        "video": video_url,
                        "description": "Ver video de la sesión",
                        "contentAsset": "exercise",
                        "iconImageVector": "PlayArrow",
                        "tratamiento_elemento_id": current_element_id,
                        "link": video_url,
                        "fullDescription": ""
                    }
                    content_items.append(elem)
                    current_element_id += 1
                    
                elif "Quiz" in col_type:
                    q_text, opts, correct_idx = parse_quiz(col_content)
                    elem = {
                        "type": "QUIZ",
                        "title": "Pregunta del día",
                        "iconResId": "outline_trophy_24",
                        "questions": [
                            {
                                "options": opts,
                                "questionText": q_text,
                                "correctAnswerIndex": correct_idx
                            }
                        ],
                        "description": "Responde las preguntas de la sesión",
                        "tratamiento_elemento_id": current_element_id,
                        "fullDescription": ""
                    }
                    content_items.append(elem)
                    current_element_id += 1
                    
                elif "Metas" in col_type:
                    # In case metas exist in this session
                    metas_opts = parse_metas(col_content)
                    elem = {
                        "type": "METAS",
                        "title": "Metas",
                        "metasId": "diabetes",
                        "iconResId": "outline_trophy_24",
                        "description": "Configura tus metas para esta sesión",
                        "metasOptions": metas_opts,
                        "tratamiento_elemento_id": current_element_id,
                        "fullDescription": ""
                    }
                    content_items.append(elem)
                    current_element_id += 1
                    
                elif "Texto" in col_type:
                    # Reading item
                    reading_idx += 1
                    if w_id == 2 and s_id == 2 and reading_idx == 4:
                        # Fix typo in table: row S2.2 - 5 erroneously linked S2.2 (Parte 3).docx instead of Parte 4
                        md_filename = "S2.2 (Parte 4).docx.md"
                    else:
                        md_filename = get_reading_filename(col_content)
                    md_path = os.path.join(LECTURAS_DIR, md_filename)
                    if not os.path.exists(md_path):
                        raise FileNotFoundError(f"Missing reading file: {md_path}")
                        
                    with open(md_path, "r", encoding="utf-8") as f_md:
                        raw_reading_content = f_md.read()
                    cleaned_reading = clean_reading_md(raw_reading_content)
                    
                    # Extension of image: .gif if link contains .gif, else .png
                    unquoted_content = urllib.parse.unquote(col_content)
                    img_ext = "gif" if ".gif" in unquoted_content.lower() else "png"
                    
                    audio_url = f"http://158.69.219.243/api/multimedia/tratamientos/AR/DBT/semana-{w_id}/sesion-{s_id}/audio-{reading_idx}.m4a"
                    image_url = f"http://158.69.219.243/api/multimedia/tratamientos/AR/DBT/semana-{w_id}/sesion-{s_id}/imagen-{reading_idx}.{img_ext}"
                    
                    elem = {
                        "type": "DESCRIPTION",
                        "audio": audio_url,
                        "image": image_url,
                        "title": col_title,
                        "iconResId": "outline_unknown_document_24",
                        "description": cleaned_reading,
                        "fullDescription": cleaned_reading,
                        "tratamiento_elemento_id": current_element_id
                    }
                    content_items.append(elem)
                    current_element_id += 1
                    
                    # SPECIAL CASE: Week 14 Session 2 split (Lectura 4 de Ana & Lectura 5 de Carlos)
                    if w_id == 14 and s_id == 2 and reading_idx == 4:
                        # Add Lectura 5
                        carlos_filename = "S14.2 (Parte 5).docx.md"
                        carlos_path = os.path.join(LECTURAS_DIR, carlos_filename)
                        with open(carlos_path, "r", encoding="utf-8") as f_carlos:
                            raw_carlos = f_carlos.read()
                        cleaned_carlos = clean_reading_md(raw_carlos)
                        
                        elem_carlos = {
                            "type": "DESCRIPTION",
                            "title": "Lectura 5",
                            "iconResId": "outline_unknown_document_24",
                            "description": cleaned_carlos,
                            "fullDescription": cleaned_carlos,
                            "tratamiento_elemento_id": current_element_id
                        }
                        content_items.append(elem_carlos)
                        current_element_id += 1
                else:
                    raise ValueError(f"Unknown content type: {col_type}")

            sesiones.append({
                "id": s_id,
                "title": session_title,
                "duration": "05:00",
                "content": content_items
            })
            
        semanas.append({
            "id": w_id,
            "title": week_title,
            "duration": "10 min",
            "thumbnail": "missing_photo",
            "sesiones": sesiones
        })
        
    final_data = {
        "tratamiento": {
            "tipo": "DIABETES",
            "nombre": "Diabetes",
            "semanas": semanas
        }
    }
    
    total_elements = current_element_id - 208
    print(f"Treatment built successfully! Total elements: {total_elements} (IDs 208 to {current_element_id - 1})")
    assert total_elements == 206, f"Expected 206 elements, got {total_elements}"
    assert current_element_id - 1 == 413, f"Expected last element ID 413, got {current_element_id - 1}"
    
    return final_data

def main():
    print("Building Argentina Diabetes Treatment JSON...")
    data = build_treatment()
    
    formatted_json = json.dumps(data, indent=2, ensure_ascii=False)
    
    for dest in DESTINATION_FILES:
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, "w", encoding="utf-8") as f:
            f.write(formatted_json)
        print(f"  Synchronized: {os.path.relpath(dest, BASE_DIR)}")

    print("All 5 files updated successfully!")

if __name__ == "__main__":
    main()
