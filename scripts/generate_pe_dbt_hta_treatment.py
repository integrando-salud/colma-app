#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_pe_dbt_hta_treatment.py

Generates the corrected JSON content for Peru Diabetes + Hypertension (DBT_HTA) treatment in ColmaApp.
Parses `app/src/main/assets/desings/Contenidos Perú - Diabetes e Hipertensión.docx.md` table row by row
and reads reading markdown files from `app/src/main/assets/desings/lecturas_peru_hta_dbt/`.

Synchronizes across:
- tratamientos_corregidos_para_backend/tratamiento_PE_DBT_HTA.json
- app/src/main/assets/mock_treatment_pe_dbt_hta.json
- app/test/api_responses/denisse_molina_DBT_HTA_PE/2_tratamiento_response.json
- test/api_responses/denisse_molina_DBT_HTA_PE/2_tratamiento_response.json
- app/app/test/api_responses/denisse_molina_DBT_HTA_PE/2_tratamiento_response.json
- app/test/api_responses/ignacio_heaton_HTA_DBT_PE/2_tratamiento_response.json
- test/api_responses/ignacio_heaton_HTA_DBT_PE/2_tratamiento_response.json
- app/app/test/api_responses/ignacio_heaton_HTA_DBT_PE/2_tratamiento_response.json
"""

import json
import os
import re
import urllib.parse

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DOCX_MD_PATH = os.path.join(BASE_DIR, "app/src/main/assets/desings/Contenidos Perú - Diabetes e Hipertensión.docx.md")
LECTURAS_DIR = os.path.join(BASE_DIR, "app/src/main/assets/desings/lecturas_peru_hta_dbt")

DESTINATION_FILES = [
    os.path.join(BASE_DIR, "tratamientos_corregidos_para_backend/tratamiento_PE_DBT_HTA.json"),
    os.path.join(BASE_DIR, "app/src/main/assets/mock_treatment_pe_dbt_hta.json"),
    os.path.join(BASE_DIR, "app/test/api_responses/denisse_molina_DBT_HTA_PE/2_tratamiento_response.json"),
    os.path.join(BASE_DIR, "test/api_responses/denisse_molina_DBT_HTA_PE/2_tratamiento_response.json"),
    os.path.join(BASE_DIR, "app/app/test/api_responses/denisse_molina_DBT_HTA_PE/2_tratamiento_response.json"),
    os.path.join(BASE_DIR, "app/test/api_responses/ignacio_heaton_HTA_DBT_PE/2_tratamiento_response.json"),
    os.path.join(BASE_DIR, "test/api_responses/ignacio_heaton_HTA_DBT_PE/2_tratamiento_response.json"),
    os.path.join(BASE_DIR, "app/app/test/api_responses/ignacio_heaton_HTA_DBT_PE/2_tratamiento_response.json"),
]

def clean_text(raw):
    """
    Cleans general markdown table text for Bienvenidas / Fin de sesión:
    - Unescapes backslash-escaped punctuation (e.g. \\!, \\., \\(, \\), etc.)
    - Replaces non-breaking space (\\xa0) with regular space
    - Removes single asterisks (*text*) used for italics so raw asterisks are not shown in UI
    - Removes empty bold tags (** **)
    - Converts vertical tabs (\\x0b) and manual breaks to double newlines (\\n\\n)
    - Trims whitespace line by line and collapses redundant empty lines
    """
    t = re.sub(r"\\([^\\])", r"\1", raw)
    t = t.replace("\xa0", " ")
    # Remove single asterisks (italics)
    t = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"\1", t)
    # Remove empty bold tags like ** **
    t = re.sub(r"\*\*\s*\*\*", "", t)
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
    cleaned = "\n".join(result).strip()
    return cleaned

def clean_reading_md(content):
    """
    Cleans markdown reading files:
    - Unescapes punctuation like \\!, \\., \\(, \\)
    - Replaces non-breaking space (\\xa0) with regular space
    - Protects bold ** markers first so they are never corrupted
    - Cleans single asterisks used for italics around punctuation, quotes, and words
    - Preserves list bullets (* )
    - Normalizes line breaks and whitespace
    """
    text = re.sub(r"\\([^\\])", r"\1", content)
    text = text.replace("\xa0", " ")
    
    # 1. Protect bold markers '**'
    BOLD_TOKEN = "___BOLD_MARKER___"
    text = text.replace("**", BOLD_TOKEN)
    
    # 2. Clean isolated single asterisks around punctuation or quotes
    text = re.sub(r"\*([.,!?:;])\*", r"\1", text)
    text = re.sub(r"\*([.,!?:;])", r"\1", text)
    text = re.sub(r"([.,!?:;])\*", r"\1", text)
    text = text.replace("“*", "“").replace("*”", "”")
    text = text.replace("*“", "“").replace("”*", "”")
    text = text.replace('"*', '"').replace('*"', '"')
    
    # 3. Handle lines and list bullets
    lines = []
    for line in text.splitlines():
        l_strip = line.rstrip()
        if not l_strip.strip():
            lines.append("")
            continue
            
        m_bullet = re.match(r"^(\s*)\*\s+(.*)$", l_strip)
        if m_bullet:
            indent = m_bullet.group(1)
            rest = m_bullet.group(2)
            rest_clean = rest.replace("*", "")
            lines.append(f"{indent}* {rest_clean}")
        else:
            clean_line = l_strip.replace("*", "")
            lines.append(clean_line)
            
    # 4. Restore bold markers
    text = "\n".join(lines)
    text = text.replace(BOLD_TOKEN, "**")
    
    # 5. Normalize empty lines and trim whitespace
    result = []
    prev_empty = False
    for l in text.splitlines():
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
    t = t.replace("\xa0", " ")
    t = re.sub(r"[\x0b\r\n]+", "\n", t).strip()
    
    # Extract correct answer
    ans_match = re.search(r"\*?Respuesta correcta:\s*([^*.\n]+)\*?", t)
    correct_str = ans_match.group(1).strip() if ans_match else ""
    
    body = t[:ans_match.start()].strip() if ans_match else t
    
    # True / False check
    if ("Verdadero" in body and "Falso" in body) and ("- Verdadero" in body or "-Verdadero" in body or ("A:" not in body and "A." not in body and "A)" not in body)):
        q_match = re.split(r"-\s*Verdadero", body)
        q_text = q_match[0].strip()
        options = ["Verdadero", "Falso"]
        correct_idx = 0 if "verdadero" in correct_str.lower() else 1
        return q_text, options, correct_idx
    
    # Multiple choice A / B / C
    parts = re.split(r"(?:^|\n|\s+)([A-C])[\.:\)]\s*", body)
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
    """
    t = re.sub(r"\\([^\\])", r"\1", raw_text)
    t = t.replace("\xa0", " ")
    t = re.sub(r"[\x0b\r\n]+", "\n", t)
    
    if re.search(r"(?:^|\n|\s)\s*1[\.\)]\s+", t):
        items = re.split(r"(?:^|\n|\s)\s*\d+[\.\)]\s+", t)
        options = [re.sub(r"\s+", " ", it).strip() for it in items if it.strip()]
        return options
    
    lines = [re.sub(r"^\d+[\.\)]\s*", "", l).strip() for l in t.split("\n") if l.strip()]
    return lines

def get_reading_filename(cell_content):
    """
    Extracts the .docx filename from markdown link and returns the matching .docx.md filename.
    """
    m = re.search(r"\[(?:Link\s*🔗|Texto)\]\((.*?\.docx.*?)\)", cell_content)
    if not m:
        m = re.search(r"\(([^)]*?\.docx[^)]*?)\)", cell_content)
    if not m:
        raise ValueError(f"Could not find docx link in cell: {cell_content[:80]}")
    url = m.group(1).replace(r"\(", "(").replace(r"\)", ")")
    unquoted = urllib.parse.unquote(url)
    m_doc = re.search(r"/([^/?#]+\.docx)", unquoted)
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
    current_element_id = 1525
    
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
                    
                    # Determine custom video index URL if needed (e.g. S4.1 video 2 is video-3.mp4 in API)
                    if w_id == 4 and s_id == 1 and video_idx == 2:
                        v_num = 3
                    else:
                        v_num = video_idx
                    video_url = f"http://158.69.219.243/api/multimedia/tratamientos/PE/DBT_HTA/semana-{w_id}/sesion-{s_id}/video-{v_num}.mp4"
                    
                    # Video title
                    if "Receta 1" in col_title:
                        v_title = "Video Receta 1"
                    elif "Receta 2" in col_title:
                        v_title = "Video Receta 2"
                    elif col_title == "Video":
                        v_title = "Video"
                    else:
                        v_title = col_title
                        
                    # Video description
                    if w_id == 4 and s_id == 1 and video_idx == 1:
                        v_desc = "¿Qué es la diabetes?"
                    elif w_id == 7 and s_id == 3 and video_idx == 1:
                        v_desc = "Comer sano teniendo diabetes"
                    elif w_id == 11 and s_id == 2 and video_idx == 1:
                        v_desc = "¿Por qué es importante la medicación y cuidar nuestra diabetes todos los días?"
                    else:
                        v_desc = "Ver video de la sesión"
                        
                    elem = {
                        "type": "VIDEO",
                        "title": v_title,
                        "video": video_url,
                        "description": v_desc,
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
                    md_filename = get_reading_filename(col_content)
                    md_path = os.path.join(LECTURAS_DIR, md_filename)
                    if not os.path.exists(md_path):
                        raise FileNotFoundError(f"Missing reading file: {md_path}")
                        
                    with open(md_path, "r", encoding="utf-8") as f_md:
                        raw_reading_content = f_md.read()
                    cleaned_reading = clean_reading_md(raw_reading_content)
                    
                    unquoted_content = urllib.parse.unquote(col_content)
                    img_ext = "gif" if ".gif" in unquoted_content.lower() else "png"
                    
                    audio_url = f"http://158.69.219.243/api/multimedia/tratamientos/PE/DBT_HTA/semana-{w_id}/sesion-{s_id}/audio-{reading_idx}.mp3"
                    image_url = f"http://158.69.219.243/api/multimedia/tratamientos/PE/DBT_HTA/semana-{w_id}/sesion-{s_id}/imagen-{reading_idx}.{img_ext}"
                    
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
            "id": None,
            "tipo": "DBT_HTA",
            "nombre": "Diabetes e hipertensión arterial",
            "semanas": semanas
        }
    }
    
    total_elements = current_element_id - 1525
    print(f"Treatment built successfully! Total elements: {total_elements} (IDs 1525 to {current_element_id - 1})")
    assert total_elements == 218, f"Expected 218 elements, got {total_elements}"
    assert current_element_id - 1 == 1742, f"Expected last element ID 1742, got {current_element_id - 1}"
    
    return final_data

def main():
    print("Building Peru Diabetes + Hypertension (DBT_HTA) Treatment JSON...")
    data = build_treatment()
    
    formatted_json = json.dumps(data, indent=2, ensure_ascii=False)
    
    for dest in DESTINATION_FILES:
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, "w", encoding="utf-8") as f:
            f.write(formatted_json)
        print(f"  Synchronized: {os.path.relpath(dest, BASE_DIR)}")

    print(f"All {len(DESTINATION_FILES)} files synchronized successfully!")

if __name__ == "__main__":
    main()
