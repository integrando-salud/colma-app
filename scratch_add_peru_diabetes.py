import json
import re
import os
import pypdf
import copy

# 1. Read lecturas_peru_hta_dbt PDF files
lecturas_dir = 'app/src/main/assets/desings/lecturas_peru_hta_dbt'
lectura_files = sorted(os.listdir(lecturas_dir))

def clean_reading_text(text):
    text = re.sub(r'[ \t]+', ' ', text)
    text = re.sub(r'\s*(●|\b\d+\.(?=\s|$))', r'<P_BREAK>\1', text)
    text = re.sub(r'([.?!:])\s*\n+', r'\1<P_BREAK>', text)
    text = re.sub(r'\s*\n+\s*', ' ', text)
    text = text.replace('<P_BREAK>', '\n \n')
    text = re.sub(r' +', ' ', text)
    return text.strip()

lecturas_map = {}
for f in lectura_files:
    if not f.endswith('.pdf'):
        continue
    path = os.path.join(lecturas_dir, f)
    reader = pypdf.PdfReader(path)
    page_texts = []
    for p in reader.pages:
        p_text = p.extract_text() or ''
        page_texts.append(clean_reading_text(p_text))
    lecturas_map[f] = '\n\n'.join(page_texts)

print(f"Loaded {len(lecturas_map)} Peru reading text files.")

# 2. Read Contenidos Perú - Diabetes PDF
pdf_path = 'app/src/main/assets/desings/Contenidos Perú - Diabetes.docx.pdf'
reader = pypdf.PdfReader(pdf_path)
full_pdf_text = '\n'.join([re.sub(r'[ \t]+', ' ', p.extract_text()) for p in reader.pages])

entry_pattern = r'(S\d+\.\d+(?:\s*\([^)]+\))?\s*-\s*\d+)'
tokens = re.split(entry_pattern, full_pdf_text)

parsed_pdf_entries = {}
for i in range(1, len(tokens), 2):
    code = tokens[i].strip()
    body = tokens[i+1].strip() if i+1 < len(tokens) else ''
    code_norm = re.sub(r'\s+', '', code)
    parsed_pdf_entries[code_norm] = body

print(f"Parsed {len(parsed_pdf_entries)} entries from Peru Diabetes PDF.")

# 3. Read content.json
with open('app/src/main/assets/content.json', 'r', encoding='utf-8') as f:
    content_dict = json.load(f)

# Find DIABETES (AR) template
diabetes_ar = None
for t in content_dict['countries']['AR']:
    if t['id'] == 'DIABETES':
        diabetes_ar = t
        break

if not diabetes_ar:
    raise Exception("DIABETES (AR) treatment template not found.")

# Remove existing DIABETES (PE) to support idempotency/reruns
content_dict['countries']['PE'] = [t for t in content_dict['countries']['PE'] if t['id'] != 'DIABETES']

diabetes_pe = copy.deepcopy(diabetes_ar)
diabetes_pe['name'] = 'Diabetes'

# 4. Map Lectura files to items in diabetes_pe (using 1-based index w_id)
file_mapping_diabetes = [
    (1, 3, "Lectura 1", "S1.3.docx (1).pdf"),
    (2, 1, "Lectura 1", "S2.1 (Parte 1).docx (1).pdf"),
    (2, 1, "Lectura 2", "S2.1 (Parte 2).docx (1).pdf"),
    (2, 1, "Lectura 3", "S2.1 (Parte 3).docx (1).pdf"),
    (2, 2, "Lectura 1", "S2.2 (Parte 1).docx (1).pdf"),
    (2, 2, "Lectura 2", "S2.2 (Parte 2).docx (1).pdf"),
    (2, 2, "Lectura 3", "S2.2 (Parte 3).docx (1).pdf"),
    (2, 2, "Lectura 4", "S2.2 (Parte 4).docx (1).pdf"),
    (3, 1, "Lectura 1", "S3.1 (Parte 1).docx (1).pdf"),
    (3, 1, "Lectura 2", "S3.1 (Parte 2).docx (1).pdf"),
    (3, 3, "Lectura 1", "S3.3 (Parte 1).docx (1).pdf"),
    (3, 3, "Lectura 2", "S3.3 (Parte 2).docx (1).pdf"),
    
    # DM specific weeks
    (4, 1, "Lectura 1", "S4.1 DM.docx (1).pdf"),
    (4, 2, "Lectura 1", "S4.2 DM (Parte 1).docx (1).pdf"),
    (4, 2, "Lectura 2", "S4.2 DM (Parte 2).docx (1).pdf"),
    (4, 3, "Lectura 1", "S4.3 DM.docx (1).pdf"),
    
    (5, 2, "Lectura 1", "S5.2.docx (1).pdf"),
    (5, 3, "Lectura 1", "S5.3.docx (1).pdf"),
    (6, 1, "Lectura 1", "S6.1 (Parte 1).docx (1).pdf"),
    (6, 1, "Lectura 2", "S6.1 (Parte 2).docx (1).pdf"),
    
    (8, 2, "Lectura 1", "S8.2 (Parte 1).docx (1).pdf"),
    (8, 2, "Lectura 2", "S8.2 (Parte 2).docx (1).pdf"),
    (9, 1, "Lectura 1", "S9.1 (Parte 1).docx (1).pdf"),
    (9, 1, "Lectura 2", "S9.1 (Parte 2).docx (1).pdf"),
    (9, 2, "Lectura 1", "S9.2 (Parte 1).docx (1).pdf"),
    (9, 2, "Lectura 2", "S9.2 (Parte 2).docx (1).pdf"),
    (9, 2, "Lectura 3", "S9.2 (Parte 3).docx (1).pdf"),
    
    # DM specific weeks
    (10, 2, "Lectura 1", "S10.2 DM (Parte 1).docx (1).pdf"),
    (10, 2, "Lectura 2", "S10.2 DM (Parte 2).docx (1).pdf"),
    
    (10, 3, "Lectura 1", "S10.3 (Parte 1).docx (1).pdf"),
    (10, 3, "Lectura 2", "S10.3 (Parte 2).docx (1).pdf"),
    
    # DM specific weeks
    (11, 1, "Lectura 1", "S11.1 DM (Parte 1).docx (1).pdf"),
    (11, 1, "Lectura 2", "S11.1 DM (Parte 2).docx (1).pdf"),
    
    (12, 1, "Lectura 1", "S12.1 (Parte 1).docx (1).pdf"),
    (12, 1, "Lectura 2", "S12.1 (Parte 2).docx (1).pdf"),
    (12, 1, "Lectura 3", "S12.1 (Parte 3).docx (1).pdf"),
    (12, 2, "Lectura 1", "S12.2 (Parte 1).docx (1).pdf"),
    (12, 2, "Lectura 2", "S12.2 (Parte 2).docx (1).pdf"),
    (12, 2, "Lectura 3", "S12.2 (Parte 3).docx (1).pdf"),
    (12, 3, "Lectura 1", "S12.3 (Parte 1).docx (1).pdf"),
    (12, 3, "Lectura 2", "S12.3 (Parte 2).docx (1).pdf"),
    (12, 3, "Lectura 3", "S12.3 (Parte 3).docx (1).pdf"),
    (13, 2, "Lectura 1", "S13.2.docx (1).pdf"),
    
    # DM specific weeks
    (13, 3, "Lectura 1", "S13.3 DM.docx (1).pdf"),
    
    (14, 2, "Lectura 1", "S14.2 (Parte 1).docx (1).pdf"),
    (14, 2, "Lectura 2", "S14.2 (Parte 2).docx (1).pdf"),
    (14, 2, "Lectura 3", "S14.2 (Parte 3).docx (1).pdf"),
    (14, 2, "Lectura 4", "S14.2 (Parte 4).docx (1).pdf"),
    (14, 2, "Lectura 5", "S14.2 (Parte 5).docx (1).pdf"),
    (14, 3, "Lectura 1", "S14.3 (Parte 1).docx (1).pdf"),
    (14, 3, "Lectura 2", "S14.3 (Parte 2).docx (1).pdf"),
    (14, 3, "Lectura 3", "S14.3 (Parte 3).docx (1).pdf"),
    (14, 3, "Lectura 4", "S14.3 (Parte 4).docx (1).pdf"),
    (15, 2, "Lectura 1", "S15.2 (Parte 1).docx (1).pdf"),
    (15, 2, "Lectura 2", "S15.2 (Parte 2).docx (1).pdf"),
    (15, 3, "Lectura 1", "S15.3.docx (1).pdf"),
    (16, 1, "Lectura 1", "S16.1 (Parte 1).docx (1).pdf"),
    (16, 1, "Lectura 2", "S16.1 (Parte 2).docx (1).pdf"),
    (16, 3, "Lectura 1", "S16.3.docx (1).pdf"),
]

updated_lecturas = 0
for w_id, sub_id, title_match, filename in file_mapping_diabetes:
    sess = diabetes_pe['sessions'][w_id - 1]
    sub = [sub for sub in sess['subSessions'] if sub['id'] == sub_id][0]

    matched_item = None
    for item in sub['multimedia']:
        if item['type'] == 'DESCRIPTION' and item.get('title') == title_match:
            matched_item = item
            break
    if matched_item and filename in lecturas_map:
        matched_item['fullDescription'] = lecturas_map[filename]
        updated_lecturas += 1

print(f"Updated {updated_lecturas} Lecturas in content.json for PE Diabetes.")

# Helper function to extract text for non-reading DESCRIPTION items from PDF body
def parse_description_body(body_text):
    lines = [l.strip() for l in body_text.split('\n') if l.strip()]
    content_lines = []
    skip_keywords = ["Sesión", "Bienvenida/Cierre:", "Texto", "Link 🔗", "Audio:", "Imagen:"]
    for line in lines:
        if any(line.startswith(kw) for kw in skip_keywords) and len(line) < 30:
            continue
        content_lines.append(line)
    
    text = ' '.join(content_lines).strip()
    return text

# Also update non-reading DESCRIPTIONs in diabetes_pe
updated_descriptions = 0
for session_idx, session in enumerate(diabetes_pe['sessions']):
    w_id = session_idx + 1 # Logical week index (1-based)
    for sub in session['subSessions']:
        s_id = sub['id']
        items = sub['multimedia']
        
        pdf_idx = 1
        for idx, item in enumerate(items):
            if idx > 0 and item['type'] == 'METAS' and items[idx-1]['type'] == 'METAS':
                pass
            elif idx > 0 and item['type'] == 'QUIZ' and items[idx-1]['type'] == 'QUIZ':
                pass
            
            possible_keys = [
                f"S{w_id}.{s_id}-{pdf_idx}",
                f"S{w_id}.{s_id}(DM)-{pdf_idx}",
                f"S{w_id}.{s_id}(HTA+DM)-{pdf_idx}",
            ]
            
            found_key = None
            for pk in possible_keys:
                if pk in parsed_pdf_entries:
                    found_key = pk
                    break
            
            if found_key:
                body = parsed_pdf_entries[found_key]
                if item['type'] == 'DESCRIPTION' and 'Lectura' not in item.get('title', ''):
                    # Extract description text from PDF body
                    desc_text = parse_description_body(body)
                    if desc_text:
                        item['description'] = desc_text
                        item['fullDescription'] = desc_text
                        updated_descriptions += 1
            
            if not ((idx < len(items)-1 and item['type'] == 'METAS' and items[idx+1]['type'] == 'METAS') or 
                    (idx < len(items)-1 and item['type'] == 'QUIZ' and items[idx+1]['type'] == 'QUIZ')):
                pdf_idx += 1

print(f"Updated {updated_descriptions} non-reading descriptions for PE Diabetes.")

# Append Peru Diabetes treatment to PE list
content_dict['countries']['PE'].append(diabetes_pe)

# Save updated content.json
with open('app/src/main/assets/content.json', 'w', encoding='utf-8') as f:
    json.dump(content_dict, f, ensure_ascii=False, indent=2)

print("Saved updated content.json successfully.")
