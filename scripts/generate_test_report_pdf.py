#!/usr/bin/env python3
import os
import glob
import xml.etree.ElementTree as ET
from datetime import datetime

class SimplePDF:
    def __init__(self, page_width=595.28, page_height=841.89): # A4
        self.width = page_width
        self.height = page_height
        self.pages = []
        self.current_stream = []
        self.new_page()

    def new_page(self):
        if self.current_stream:
            self.pages.append(b"".join(self.current_stream))
            self.current_stream = []
        self.current_stream = []

    def _hex_to_rgb(self, hex_color):
        hex_color = hex_color.lstrip('#')
        r = int(hex_color[0:2], 16) / 255.0
        g = int(hex_color[2:4], 16) / 255.0
        b = int(hex_color[4:6], 16) / 255.0
        return f"{r:.3f} {g:.3f} {b:.3f}"

    def draw_rect(self, x, y, w, h, fill_hex=None, stroke_hex=None, stroke_width=1):
        cmds = []
        if stroke_hex:
            cmds.append(f"{self._hex_to_rgb(stroke_hex)} RG")
            cmds.append(f"{stroke_width} w")
        if fill_hex:
            cmds.append(f"{self._hex_to_rgb(fill_hex)} rg")
        cmds.append(f"{x:.2f} {y:.2f} {w:.2f} {h:.2f} re")
        if fill_hex and stroke_hex:
            cmds.append("B")
        elif fill_hex:
            cmds.append("f")
        elif stroke_hex:
            cmds.append("S")
        self.current_stream.append((" ".join(cmds) + "\n").encode('latin-1'))

    def draw_line(self, x1, y1, x2, y2, stroke_hex="#000000", stroke_width=1):
        cmd = f"{self._hex_to_rgb(stroke_hex)} RG {stroke_width} w {x1:.2f} {y1:.2f} m {x2:.2f} {y2:.2f} l S\n"
        self.current_stream.append(cmd.encode('latin-1'))

    def draw_text(self, text, x, y, font="F1", size=10, color_hex="#000000"):
        # Encode with CP1252 / Latin-1 for proper Spanish accents (á, é, í, ó, ú, ñ, etc.)
        encoded_bytes = text.encode('cp1252', 'replace')
        escaped_bytes = bytearray()
        for b in encoded_bytes:
            if b in (ord('\\'), ord('('), ord(')')):
                escaped_bytes.append(ord('\\'))
            escaped_bytes.append(b)
            
        rgb = self._hex_to_rgb(color_hex)
        header = f"BT /{font} {size} Tf {rgb} rg 1 0 0 1 {x:.2f} {y:.2f} Tm (".encode('latin-1')
        footer = b") Tj ET\n"
        self.current_stream.append(header + bytes(escaped_bytes) + footer)

    def finish_page(self):
        if self.current_stream:
            self.pages.append(b"".join(self.current_stream))
            self.current_stream = []

    def build_pdf_bytes(self):
        self.finish_page()
        objects = []
        
        obj_count = 4
        page_obj_ids = []
        content_obj_ids = []
        
        for _ in self.pages:
            obj_count += 1
            page_obj_ids.append(obj_count)
            obj_count += 1
            content_obj_ids.append(obj_count)

        # 1: Catalog
        objects.append(b"1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        
        # 2: Pages
        kids_str = " ".join([f"{pid} 0 R" for pid in page_obj_ids])
        objects.append(f"2 0 obj\n<< /Type /Pages /Kids [{kids_str}] /Count {len(self.pages)} >>\nendobj\n".encode('latin-1'))
        
        # 3: Font Regular with WinAnsiEncoding for accents
        objects.append(b"3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n")
        # 4: Font Bold with WinAnsiEncoding for accents
        objects.append(b"4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n")
        
        for i, page_content in enumerate(self.pages):
            pid = page_obj_ids[i]
            cid = content_obj_ids[i]
            
            p_obj = (f"{pid} 0 obj\n<< /Type /Page /Parent 2 0 R "
                     f"/MediaBox [0 0 {self.width:.2f} {self.height:.2f}] "
                     f"/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> "
                     f"/Contents {cid} 0 R >>\nendobj\n").encode('latin-1')
            objects.append(p_obj)
            
            stream_len = len(page_content)
            c_obj_header = f"{cid} 0 obj\n<< /Length {stream_len} >>\nstream\n".encode('latin-1')
            c_obj_footer = b"\nendstream\nendobj\n"
            objects.append(c_obj_header + page_content + c_obj_footer)
            
        # Assembly
        out = [b"%PDF-1.4\n"]
        xref = [0]
        offset = len(out[0])
        
        for obj_bytes in objects:
            xref.append(offset)
            out.append(obj_bytes)
            offset += len(obj_bytes)
            
        startxref = offset
        xref_str = f"xref\n0 {len(xref)}\n0000000000 65535 f \n"
        for off in xref[1:]:
            xref_str += f"{off:010d} 00000 n \n"
        xref_str += f"trailer\n<< /Size {len(xref)} /Root 1 0 R >>\nstartxref\n{startxref}\n%%EOF\n"
        
        out.append(xref_str.encode('latin-1'))
        return b"".join(out)


def wrap_text(text, max_chars=58):
    words = text.split()
    if not words:
        return [""]
    lines = []
    current_line = []
    current_len = 0
    
    for word in words:
        if current_len + len(word) + (1 if current_line else 0) <= max_chars:
            current_line.append(word)
            current_len += len(word) + (1 if len(current_line) > 1 else 0)
        else:
            if current_line:
                lines.append(" ".join(current_line))
            current_line = [word]
            current_len = len(word)
            
    if current_line:
        lines.append(" ".join(current_line))
    return lines


def parse_test_results(results_dir):
    suites = []
    xml_files = sorted(glob.glob(os.path.join(results_dir, "TEST-*.xml")))
    
    total_tests = 0
    total_failures = 0
    total_errors = 0
    total_skipped = 0
    total_time = 0.0

    for xml_file in xml_files:
        try:
            tree = ET.parse(xml_file)
            root = tree.getroot()
            name = root.attrib.get('name', os.path.basename(xml_file))
            short_name = name.split('.')[-1]
            tests = int(root.attrib.get('tests', 0))
            failures = int(root.attrib.get('failures', 0))
            errors = int(root.attrib.get('errors', 0))
            skipped = int(root.attrib.get('skipped', 0))
            time_val = float(root.attrib.get('time', 0.0))
            
            total_tests += tests
            total_failures += failures
            total_errors += errors
            total_skipped += skipped
            total_time += time_val
            
            cases = []
            for tc in root.findall('testcase'):
                tc_name = tc.attrib.get('name', 'unnamed')
                tc_time = float(tc.attrib.get('time', 0.0))
                failure = tc.find('failure')
                error = tc.find('error')
                
                status = "APROBADO"
                message = ""
                if failure is not None:
                    status = "FALLIDO"
                    message = failure.attrib.get('message', failure.text or '')
                elif error is not None:
                    status = "ERROR"
                    message = error.attrib.get('message', error.text or '')
                
                cases.append({
                    "name": tc_name,
                    "time": tc_time,
                    "status": status,
                    "message": message
                })
                
            suites.append({
                "fullName": name,
                "name": short_name,
                "tests": tests,
                "failures": failures + errors,
                "skipped": skipped,
                "time": time_val,
                "cases": cases
            })
        except Exception as e:
            print(f"Error parsing {xml_file}: {e}")
            
    return {
        "suites": suites,
        "total_tests": total_tests,
        "total_failures": total_failures + total_errors,
        "total_skipped": total_skipped,
        "total_passed": total_tests - (total_failures + total_errors) - total_skipped,
        "total_time": total_time,
        "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }


def generate_pdf(data, output_path):
    pdf = SimplePDF()
    margin_x = 40
    content_width = 515.28
    y = 800

    def check_page_break(needed_space=30):
        nonlocal y
        if y - needed_space < 50:
            # Footer on current page
            pdf.draw_line(margin_x, 40, margin_x + content_width, 40, stroke_hex="#CBD5E1", stroke_width=0.5)
            pdf.draw_text("ColmaApp - Reporte Oficial de Calidad y Pruebas", margin_x, 28, font="F1", size=8, color_hex="#64748B")
            pdf.draw_text(f"Generado el: {data['timestamp']}", margin_x + content_width - 150, 28, font="F1", size=8, color_hex="#64748B")
            pdf.new_page()
            y = 800
            # Header on new page
            pdf.draw_rect(margin_x, y - 20, content_width, 24, fill_hex="#1E293B")
            pdf.draw_text("ColmaApp | Resumen de Resultados QA (Continuación)", margin_x + 10, y - 14, font="F2", size=10, color_hex="#FFFFFF")
            y -= 40

    # Header Top Bar
    pdf.draw_rect(0, 770, 595.28, 72, fill_hex="#1E3A8A")
    pdf.draw_text("COLMA APP - REPORTE DE EJECUCIÓN QA", margin_x, 815, font="F2", size=18, color_hex="#FFFFFF")
    pdf.draw_text("Suite de Pruebas Unitarias, Integración y Reglas Clínicas", margin_x, 795, font="F1", size=10, color_hex="#93C5FD")
    pdf.draw_text(f"Fecha de Ejecución: {data['timestamp']}", margin_x + content_width - 175, 795, font="F1", size=9, color_hex="#E2E8F0")

    y = 745

    # Summary Cards
    passed = data["total_passed"]
    failed = data["total_failures"]
    total = data["total_tests"]
    rate = (passed / total * 100) if total > 0 else 0
    status_overall = "APROBADO" if failed == 0 and total > 0 else "CON ERRORES"
    status_bg = "#DCFCE7" if failed == 0 else "#FEE2E2"
    status_fg = "#166534" if failed == 0 else "#991B1B"

    # Status banner
    pdf.draw_rect(margin_x, y - 30, content_width, 32, fill_hex=status_bg, stroke_hex=status_fg, stroke_width=1)
    pdf.draw_text(f"ESTADO GENERAL DE LA SUITE:  {status_overall}", margin_x + 12, y - 18, font="F2", size=12, color_hex=status_fg)
    pdf.draw_text(f"Tasa de Éxito: {rate:.1f}%", margin_x + content_width - 120, y - 18, font="F2", size=12, color_hex=status_fg)

    y -= 50

    # 4 KPI boxes
    box_w = (content_width - 30) / 4
    boxes = [
        ("Total de Pruebas", str(total), "#F8FAFC", "#334155", "#0F172A"),
        ("Pruebas Exitosas", str(passed), "#F0FDF4", "#16A34A", "#166534"),
        ("Pruebas Fallidas", str(failed), "#FEF2F2" if failed > 0 else "#F8FAFC", "#DC2626" if failed > 0 else "#64748B", "#991B1B" if failed > 0 else "#0F172A"),
        ("Tiempo de Ejecución", f"{data['total_time']:.2f} s", "#F8FAFC", "#2563EB", "#1E3A8A"),
    ]

    for idx, (title, val, bg, border, fg) in enumerate(boxes):
        bx = margin_x + idx * (box_w + 10)
        pdf.draw_rect(bx, y - 48, box_w, 48, fill_hex=bg, stroke_hex=border, stroke_width=1)
        pdf.draw_text(title, bx + 8, y - 18, font="F1", size=8, color_hex="#64748B")
        pdf.draw_text(val, bx + 8, y - 38, font="F2", size=16, color_hex=fg)

    y -= 68

    # Section Title
    pdf.draw_text("DETALLE DESGLOSADO POR SUITE DE PRUEBA", margin_x, y, font="F2", size=12, color_hex="#1E293B")
    pdf.draw_line(margin_x, y - 6, margin_x + content_width, y - 6, stroke_hex="#CBD5E1", stroke_width=1)
    y -= 22

    for suite in data["suites"]:
        check_page_break(50)
        
        # Suite Header Row
        suite_pass = suite["failures"] == 0
        s_badge_bg = "#DCFCE7" if suite_pass else "#FEE2E2"
        s_badge_fg = "#166534" if suite_pass else "#991B1B"
        
        pdf.draw_rect(margin_x, y - 20, content_width, 22, fill_hex="#F1F5F9", stroke_hex="#CBD5E1", stroke_width=0.5)
        pdf.draw_text(f"Suite: {suite['name']}", margin_x + 8, y - 14, font="F2", size=10, color_hex="#1E293B")
        pdf.draw_text(f"({suite['tests']} pruebas | {suite['time']:.2f}s)", margin_x + 230, y - 14, font="F1", size=9, color_hex="#64748B")
        
        # Badge
        badge_text = "APROBADO" if suite_pass else f"{suite['failures']} FALLIDOS"
        pdf.draw_rect(margin_x + content_width - 68, y - 18, 62, 16, fill_hex=s_badge_bg, stroke_hex=s_badge_fg, stroke_width=0.5)
        pdf.draw_text(badge_text, margin_x + content_width - 62, y - 13, font="F2", size=7.5, color_hex=s_badge_fg)
        
        y -= 24

        for tc in suite["cases"]:
            lines = wrap_text(tc["name"], max_chars=58)
            row_height = 14 + (len(lines) - 1) * 11
            check_page_break(row_height + 6)
            
            is_tc_pass = tc["status"] == "APROBADO"
            dot_color = "#16A34A" if is_tc_pass else "#DC2626"
            
            # Row background
            pdf.draw_rect(margin_x, y - row_height, content_width, row_height, fill_hex="#FFFFFF")
            # Left status indicator dot
            pdf.draw_rect(margin_x + 8, y - 10, 5, 5, fill_hex=dot_color)
            
            # Render multi-line test name
            for line_idx, line_str in enumerate(lines):
                line_y = y - 11 - (line_idx * 11)
                pdf.draw_text(line_str, margin_x + 20, line_y, font="F1", size=8.5, color_hex="#334155")
            
            # Time and status
            status_color = "#166534" if is_tc_pass else "#991B1B"
            pdf.draw_text(f"{tc['time']:.3f}s", margin_x + content_width - 105, y - 11, font="F1", size=8, color_hex="#94A3B8")
            pdf.draw_text(tc["status"], margin_x + content_width - 55, y - 11, font="F2", size=8, color_hex=status_color)
            
            y -= (row_height + 2)
            
        y -= 6

    # Final Footer on last page
    pdf.draw_line(margin_x, 40, margin_x + content_width, 40, stroke_hex="#CBD5E1", stroke_width=0.5)
    pdf.draw_text("ColmaApp - Reporte Oficial de Calidad y Pruebas", margin_x, 28, font="F1", size=8, color_hex="#64748B")
    pdf.draw_text(f"Generado el: {data['timestamp']}", margin_x + content_width - 150, 28, font="F1", size=8, color_hex="#64748B")

    pdf_bytes = pdf.build_pdf_bytes()
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "wb") as f:
        f.write(pdf_bytes)
    print(f"Reporte PDF generado exitosamente en: {output_path}")


def main():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    results_dir = os.path.join(base_dir, "app", "build", "test-results", "testDebugUnitTest")
    output_pdf_app = os.path.join(base_dir, "app", "test", "Reporte_Resultados_QA.pdf")
    output_pdf_root = os.path.join(base_dir, "test", "Reporte_Resultados_QA.pdf")

    if not os.path.exists(results_dir):
        print(f"Directorio de resultados no encontrado: {results_dir}")
        return

    data = parse_test_results(results_dir)
    generate_pdf(data, output_pdf_app)
    try:
        generate_pdf(data, output_pdf_root)
    except Exception:
        pass

if __name__ == "__main__":
    main()
