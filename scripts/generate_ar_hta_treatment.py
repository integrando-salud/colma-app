#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_ar_hta_treatment.py

Generates the corrected JSON content for Argentina Hypertension (HTA) treatment in ColmaApp.
Parses `app/src/main/assets/desings/Contenidos Argentina  - Hipertensión.docx.md` table row by row
and reads reading markdown files from `app/src/main/assets/desings/lecturas_hta_dbt/`.

Synchronizes across:
- tratamientos_corregidos_para_backend/tratamiento_AR_HTA.json
- app/src/main/assets/mock_treatment_ar_hta.json
- app/test/api_responses/maria_gonzalez_HTA_AR/2_tratamiento_response.json
- test/api_responses/maria_gonzalez_HTA_AR/2_tratamiento_response.json
- app/app/test/api_responses/maria_gonzalez_HTA_AR/2_tratamiento_response.json
"""

import json
import os
import re
import urllib.parse

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DOCX_MD_PATH = os.path.join(BASE_DIR, "app/src/main/assets/desings/Contenidos Argentina  - Hipertensión.docx.md")
LECTURAS_DIR = os.path.join(BASE_DIR, "app/src/main/assets/desings/lecturas_hta_dbt")

DESTINATION_FILES = [
    os.path.join(BASE_DIR, "tratamientos_corregidos_para_backend/tratamiento_AR_HTA.json"),
    os.path.join(BASE_DIR, "app/src/main/assets/mock_treatment_ar_hta.json"),
    os.path.join(BASE_DIR, "app/test/api_responses/maria_gonzalez_HTA_AR/2_tratamiento_response.json"),
    os.path.join(BASE_DIR, "test/api_responses/maria_gonzalez_HTA_AR/2_tratamiento_response.json"),
    os.path.join(BASE_DIR, "app/app/test/api_responses/maria_gonzalez_HTA_AR/2_tratamiento_response.json"),
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
    if "hoario" in cleaned:
        cleaned = cleaned.replace("hoario", "horario")
    if "el protagonista" in cleaned:
        cleaned = cleaned.replace("el protagonista", "el/la protagonista")
    return cleaned

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
    text = text.replace("*“", "“").replace("”*", "”")
    text = text.replace('"*', '"').replace('*"', '"')
    text = text.replace('* "', '"').replace('" *', '"')
    
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
    t = t.replace("\xa0", " ")
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
    Handles numbered lists (1. ... 10.), semicolon/newline separated, or specific categories.
    """
    t = re.sub(r"\\([^\\])", r"\1", raw_text)
    t = t.replace("\xa0", " ")
    
    # 1. Numbered: 1. ... 2. ...
    if re.search(r"(?:^|\x0b|\n|\s)\s*1\.\s+", t):
        items = re.split(r"(?:^|\x0b|\n|\s)\s*\d+\.\s+", t)
        options = [re.sub(r"[\x0b\r\n\s]+", " ", it).strip() for it in items if it.strip()]
        return options
    
    # 2. HTA Health/Medication metas (S10.3 and S11.3)
    if "Tomar todos mis medicamentos" in t and "Organizar el pastillero" in t:
        return [
            "Tomar todos mis medicamentos del día como me los indicó el médico",
            "Organizar el pastillero para toda la semana",
            "Medir mi presión arterial y anotar el resultado",
            "Anotar dudas o síntomas en un papel para llevar a mi próxima consulta médica",
            "Agendar mi próxima consulta médica si me corresponde un control",
            "Revisar cuántos medicamentos me quedan y anotar la fecha para ir a solicitar más al centro de salud",
            "Poner una alarma en el celular para tomar mi medicación"
        ]
        
    # 3. S3.3 Logro metas
    if "Lavar los platos" in t and "Pedir una cita médica" in t:
        return [
            "Lavar los platos inmediatamente después de comer",
            "Pedir una cita médica o hacer un trámite pendiente",
            "Reparar algo pequeño en casa (ejemplo: cambiar bombilla, coser botón)",
            "Sacar a pasear a mi mascota",
            "Borrar fotos o mensajes antiguos del celular para liberar espacio",
            "Dedicar 20 minutos a jardinería o cuidado de plantas",
            "Organizar los gastos de la semana",
            "Ordenar un mueble, un cajón o un estante",
            "Limpiar una zona de la casa (ejemplo: heladera)",
            "Lavar, doblar o planchar ropa durante 30 minutos"
        ]

    # 4. S14.3 Social support metas
    if "Contarle a un amigo" in t and "Invitar a un amigo" in t:
        return [
            "Contarle a un amigo o familiar algo bueno que me pasó esta semana",
            "Invitar a un amigo o familiar a tomar un café, té o mate",
            "Pedirle a un amigo o familiar que me acompañe a caminar",
            "Enviar un mensaje de texto cariñoso a alguien que quiero",
            "Visitar a un familiar o amigo",
            "Participar en una actividad comunitaria o religiosa",
            "Compartir una comida (almuerzo/cena) con alguien, sin prender la televisión o usar el celular",
            "Pedir ayuda a un amigo o familiar para una tarea sencilla (ejemplo: hacer un arreglo en casa).",
            "Jugar un juego de mesa o cartas con la familia o amigos",
            "Agradecer a un familiar o amigo que me ha ayudado con algo"
        ]

    # 5. S1.3, S2.3, S15.3 Placer metas
    if "Escuchar una de mis canciones" in t and "Leer un libro" in t:
        return [
            "Escuchar una de mis canciones favoritas",
            "Leer un libro o una revista o el diario",
            "Ver un capítulo de mi serie favorita o una película",
            "Dedicar 15 minutos a una actividad que me guste (ejemplo: tejer, pintar, jardinería).",
            "Jugar con mi mascota",
            "Tomarme un breve descanso para tomar un té o café o mate",
            "Jugar cartas, dominó o algún otro juego de mesa",
            "Imaginarme un lugar tranquilo, hermoso o relajante (ejemplo: playa, campo) por 2 minutos",
            "Escribir algo lindo que me haya pasado en la semana.",
            "Dar un paseo"
        ]

    # Fallback to splitting by 2 or more spaces or vertical tabs
    items = re.split(r"(?:\x0b|\s{2,})", t)
    return [re.sub(r"\s+", " ", it).strip() for it in items if it.strip()]

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
    current_element_id = 1
    
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
                    video_url = f"http://158.69.219.243/api/multimedia/tratamientos/AR/HTA/semana-{w_id}/sesion-{s_id}/video-{video_idx}.mp4"
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
                    metas_opts = parse_metas(col_content)
                    elem = {
                        "type": "METAS",
                        "title": "Metas",
                        "metasId": "hta",
                        "iconResId": "outline_trophy_24",
                        "description": "Configura tus metas para esta sesión",
                        "metasOptions": metas_opts,
                        "tratamiento_elemento_id": current_element_id,
                        "fullDescription": ""
                    }
                    content_items.append(elem)
                    current_element_id += 1
                    
                elif "Texto" in col_type:
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
                    
                    audio_url = f"http://158.69.219.243/api/multimedia/tratamientos/AR/HTA/semana-{w_id}/sesion-{s_id}/audio-{reading_idx}.m4a"
                    image_url = f"http://158.69.219.243/api/multimedia/tratamientos/AR/HTA/semana-{w_id}/sesion-{s_id}/imagen-{reading_idx}.{img_ext}"
                    
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
            "tipo": "HTA",
            "nombre": "Hipertensión",
            "semanas": semanas
        }
    }
    
    total_elements = current_element_id - 1
    print(f"Treatment built successfully! Total elements: {total_elements} (IDs 1 to {current_element_id - 1})")
    assert total_elements == 203, f"Expected 203 elements, got {total_elements}"
    assert current_element_id - 1 == 203, f"Expected last element ID 203, got {current_element_id - 1}"
    
    return final_data

def main():
    print("Building Argentina Hypertension (HTA) Treatment JSON...")
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
