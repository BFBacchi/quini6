#!/usr/bin/env python3
"""Generate a professional PDF report for Quini6 Analytics."""

import os
from fpdf import FPDF
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch
import numpy as np

OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGES_DIR = os.path.join(OUTPUT_DIR, "images")
os.makedirs(IMAGES_DIR, exist_ok=True)

# ── Color Palette ──────────────────────────────────────────────────────────
PRIMARY = "#2563EB"      # Blue
SECONDARY = "#059669"    # Green
ACCENT = "#DC2626"       # Red
DARK = "#1E293B"         # Slate dark
LIGHT = "#F8FAFC"        # Slate light
GRAY = "#94A3B8"         # Slate gray
COLORS = ["#2563EB", "#059669", "#F59E0B", "#DC2626", "#8B5CF6", "#06B6D4"]

# ── Generate Diagrams ─────────────────────────────────────────────────────

def gen_architecture_diagram():
    fig, ax = plt.subplots(1, 1, figsize=(8, 5))
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 7)
    ax.axis('off')
    fig.patch.set_facecolor('white')

    def box(x, y, w, h, text, color, fontsize=10, textcolor='white'):
        rect = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.1",
                              facecolor=color, edgecolor='#334155', linewidth=1.5)
        ax.add_patch(rect)
        ax.text(x + w/2, y + h/2, text, ha='center', va='center',
                fontsize=fontsize, fontweight='bold', color=textcolor)

    def arrow(x1, y1, x2, y2):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle='->', color=GRAY, lw=1.5))

    # Title
    ax.text(5, 6.7, 'Quini6 Analytics -- Architecture', ha='center', va='center',
            fontsize=14, fontweight='bold', color=DARK)

    # Layers
    box(1, 5.5, 8, 0.8, 'FRONTEND  |  React 18 + TypeScript + Vite  |  :3000', '#3B82F6', 11)
    box(1, 4.2, 8, 0.8, 'API LAYER  |  Spring Boot 3.3.2 + Java 21  |  :8082', '#2563EB', 11)

    # Backend services
    box(0.5, 2.8, 2.2, 0.9, 'PostgreSQL\n16\n:5434', '#334155', 9)
    box(2.9, 2.8, 2.2, 0.9, 'Elasticsearch\n8.14\n:9200', '#334155', 9)
    box(5.3, 2.8, 2.2, 0.9, 'RabbitMQ\n3.13\n:5672', '#334155', 9)
    box(7.7, 2.8, 2.2, 0.9, 'Prometheus\n:9090', '#334155', 9)

    # Monitoring
    box(2.0, 1.2, 2.8, 0.8, 'Grafana  |  :3001', '#6366F1', 10)
    box(5.5, 1.2, 2.8, 0.8, 'Kibana  |  :5601', '#0EA5E9', 10)

    # Arrows
    arrow(5, 5.5, 5, 5.0)   # Frontend -> API
    arrow(5, 4.2, 1.6, 3.7) # -> PostgreSQL
    arrow(5, 4.2, 4.0, 3.7) # -> Elasticsearch
    arrow(5, 4.2, 6.4, 3.7) # -> RabbitMQ
    arrow(5, 4.2, 8.8, 3.7) # -> Prometheus
    arrow(3.4, 2.8, 3.4, 2.0) # PG -> Grafana
    arrow(6.4, 2.8, 6.9, 2.0) # ES -> Kibana

    plt.tight_layout()
    path = os.path.join(IMAGES_DIR, "architecture.png")
    plt.savefig(path, dpi=200, bbox_inches='tight', facecolor='white')
    plt.close()
    return path


def gen_layered_diagram():
    fig, ax = plt.subplots(figsize=(8, 4))
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 5)
    ax.axis('off')
    fig.patch.set_facecolor('white')

    layers = [
        (0.5, 3.8, 9, 0.8, 'PRESENTATION  --  React + TypeScript + Vite', '#3B82F6'),
        (0.5, 2.8, 9, 0.8, 'CONTROLLER  --  REST Endpoints + DTOs + Validation', '#2563EB'),
        (0.5, 1.8, 9, 0.8, 'SERVICE  --  Business Logic + Resilience4j + Audit', '#1D4ED8'),
        (0.5, 0.8, 9, 0.8, 'REPOSITORY  --  Spring Data JPA + Flyway Migrations', '#1E3A5F'),
    ]

    for x, y, w, h, text, color in layers:
        rect = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.08",
                              facecolor=color, edgecolor='white', linewidth=2)
        ax.add_patch(rect)
        ax.text(x + w/2, y + h/2, text, ha='center', va='center',
                fontsize=11, fontweight='bold', color='white')

    # Side annotations
    ax.annotate('TypeScript\nStrict Mode', xy=(9.7, 4.2), fontsize=8, color=GRAY, ha='center')
    ax.annotate('JWT Auth\nInput Validation', xy=(9.7, 3.2), fontsize=8, color=GRAY, ha='center')
    ax.annotate('Circuit Breaker\nRetry + Fallback', xy=(9.7, 2.2), fontsize=8, color=GRAY, ha='center')
    ax.annotate('UUID PKs\nACID Transactions', xy=(9.7, 1.2), fontsize=8, color=GRAY, ha='center')

    ax.text(5, 4.75, 'Layered Architecture -- Separation of Concerns', ha='center',
            fontsize=13, fontweight='bold', color=DARK)

    plt.tight_layout()
    path = os.path.join(IMAGES_DIR, "layers.png")
    plt.savefig(path, dpi=200, bbox_inches='tight', facecolor='white')
    plt.close()
    return path


def gen_testing_pyramid():
    fig, ax = plt.subplots(figsize=(6, 5))
    ax.set_xlim(0, 6)
    ax.set_ylim(0, 5.5)
    ax.axis('off')
    fig.patch.set_facecolor('white')

    # Pyramid layers
    triangles = [
        ([3, 1.5, 4.5], [4.8, 3.8, 3.8], '#DC2626', 'E2E Tests\n(Cypress/Playwright)', 'white'),
        ([3, 1.0, 5.0], [3.8, 2.6, 2.6], '#F59E0B', 'Integration Tests\n(Testcontainers)', 'white'),
        ([3, 0.5, 5.5], [2.6, 1.2, 1.2], '#059669', 'Unit Tests\n(JUnit 5 + Mockito)', 'white'),
    ]

    for xs, ys, color, text, tc in triangles:
        ax.fill(xs, ys, color=color, alpha=0.9)
        ax.plot(xs + [xs[0]], ys + [ys[0]], color='white', linewidth=2)
        cy = sum(ys) / 3
        ax.text(3, cy, text, ha='center', va='center', fontsize=10,
                fontweight='bold', color=tc)

    ax.text(3, 5.2, 'Testing Pyramid', ha='center', fontsize=13,
            fontweight='bold', color=DARK)

    # Annotations
    ax.annotate('Fast, isolated\nMocked dependencies', xy=(5.5, 1.8), fontsize=8, color=GRAY)
    ax.annotate('Real containers\nPG + ES + RabbitMQ', xy=(5.8, 3.0), fontsize=8, color=GRAY)
    ax.annotate('Full stack\nBrowser automation', xy=(5.2, 4.3), fontsize=8, color=GRAY)

    plt.tight_layout()
    path = os.path.join(IMAGES_DIR, "testing_pyramid.png")
    plt.savefig(path, dpi=200, bbox_inches='tight', facecolor='white')
    plt.close()
    return path


def gen_jwt_flow():
    fig, ax = plt.subplots(figsize=(8, 3.5))
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 4)
    ax.axis('off')
    fig.patch.set_facecolor('white')

    def box(x, y, w, h, text, color):
        rect = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.08",
                              facecolor=color, edgecolor='white', linewidth=1.5)
        ax.add_patch(rect)
        ax.text(x + w/2, y + h/2, text, ha='center', va='center',
                fontsize=8, fontweight='bold', color='white')

    def arrow(x1, y1, x2, y2, label=''):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle='->', color=GRAY, lw=1.5))
        if label:
            mx, my = (x1+x2)/2, (y1+y2)/2
            ax.text(mx, my+0.15, label, ha='center', fontsize=7, color=DARK)

    box(0.2, 2.5, 1.5, 0.8, 'Client\n(Browser)', '#6366F1')
    box(2.5, 2.5, 1.5, 0.8, 'Auth\nController', '#2563EB')
    box(5.0, 2.5, 1.5, 0.8, 'JWT\nProvider', '#1D4ED8')
    box(7.5, 2.5, 1.8, 0.8, 'Protected\nResources', '#059669')

    # Login flow
    arrow(1.7, 3.2, 2.5, 3.2, 'POST /auth/login')
    arrow(4.0, 3.2, 5.0, 3.2, 'Validate + Sign')
    arrow(6.5, 3.2, 7.5, 3.2, 'Bearer Token')

    # Request flow
    arrow(1.7, 2.5, 2.5, 2.5, 'JWT Filter')
    arrow(4.0, 2.5, 5.0, 2.5, 'Verify Signature')
    arrow(6.5, 2.5, 7.5, 2.5, 'SecurityContext')

    ax.text(5, 3.8, 'JWT Authentication Flow', ha='center', fontsize=12,
            fontweight='bold', color=DARK)

    plt.tight_layout()
    path = os.path.join(IMAGES_DIR, "jwt_flow.png")
    plt.savefig(path, dpi=200, bbox_inches='tight', facecolor='white')
    plt.close()
    return path


def gen_db_schema():
    fig, ax = plt.subplots(figsize=(8, 4.5))
    ax.set_xlim(0, 12)
    ax.set_ylim(0, 6)
    ax.axis('off')
    fig.patch.set_facecolor('white')

    def table(x, y, name, cols, color):
        w = 2.4
        h = 0.3 * (len(cols) + 1)
        rect = FancyBboxPatch((x, y - h), w, h, boxstyle="round,pad=0.05",
                              facecolor='white', edgecolor=color, linewidth=2)
        ax.add_patch(rect)
        # Header
        header = FancyBboxPatch((x, y - 0.3), w, 0.3, boxstyle="round,pad=0.02",
                                facecolor=color, edgecolor=color)
        ax.add_patch(header)
        ax.text(x + w/2, y - 0.15, name, ha='center', va='center',
                fontsize=9, fontweight='bold', color='white')
        # Columns
        for i, col in enumerate(cols):
            ax.text(x + 0.1, y - 0.3 - 0.3*(i+0.5), col, fontsize=7, color=DARK, family='monospace')
        return y - h

    # Tables
    table(0.3, 5.5, 'usuarios', ['UUID PK', 'username', 'password_hash', 'role', 'enabled'], '#2563EB')
    table(3.2, 5.5, 'sorteos', ['UUID PK', 'numero_sorteo UNIQUE', 'fecha', 'pozo_monto', 'raw_json'], '#059669')
    table(6.1, 5.5, 'resultados', ['UUID PK', 'sorteo_id FK', 'modalidad', 'numero_1..6', 'premio_*'], '#F59E0B')
    table(9.0, 5.5, 'numeros_sorteados', ['BIGSERIAL PK', 'resultado_id FK', 'numero', 'posicion', 'fecha'], '#DC2626')
    table(3.2, 1.5, 'audit_log', ['BIGSERIAL PK', 'entidad', 'accion', 'actor', 'timestamp'], '#8B5CF6')

    # Relationships (arrows)
    ax.annotate('', xy=(3.2, 5.3), xytext=(2.7, 5.3),
                arrowprops=dict(arrowstyle='->', color=GRAY, lw=1.5))
    ax.annotate('', xy=(6.1, 5.3), xytext=(5.6, 5.3),
                arrowprops=dict(arrowstyle='->', color=GRAY, lw=1.5))
    ax.annotate('', xy=(9.0, 5.3), xytext=(8.5, 5.3),
                arrowprops=dict(arrowstyle='->', color=GRAY, lw=1.5))

    ax.text(6, 5.8, 'Database Schema -- PostgreSQL', ha='center', fontsize=12,
            fontweight='bold', color=DARK)

    plt.tight_layout()
    path = os.path.join(IMAGES_DIR, "db_schema.png")
    plt.savefig(path, dpi=200, bbox_inches='tight', facecolor='white')
    plt.close()
    return path


def gen_tech_radar():
    fig, ax = plt.subplots(figsize=(6, 6), subplot_kw=dict(polar=True))
    fig.patch.set_facecolor('white')

    categories = ['Frontend', 'Backend', 'Database', 'DevOps', 'Testing', 'Security']
    N = len(categories)
    angles = [n / float(N) * 2 * np.pi for n in range(N)]
    angles += angles[:1]

    # Scores (1-5)
    scores = [4.5, 5, 4, 4.5, 4, 4]
    scores += scores[:1]

    ax.plot(angles, scores, 'o-', linewidth=2, color=PRIMARY)
    ax.fill(angles, scores, alpha=0.25, color=PRIMARY)

    ax.set_xticks(angles[:-1])
    ax.set_xticklabels(categories, fontsize=10, fontweight='bold', color=DARK)
    ax.set_ylim(0, 5)
    ax.set_yticks([1, 2, 3, 4, 5])
    ax.set_yticklabels(['1', '2', '3', '4', '5'], fontsize=7, color=GRAY)
    ax.set_title('Technology Proficiency Radar', fontsize=12, fontweight='bold',
                 color=DARK, pad=20)

    plt.tight_layout()
    path = os.path.join(IMAGES_DIR, "tech_radar.png")
    plt.savefig(path, dpi=200, bbox_inches='tight', facecolor='white')
    plt.close()
    return path


def gen_cicd_pipeline():
    fig, ax = plt.subplots(figsize=(8, 3))
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 3)
    ax.axis('off')
    fig.patch.set_facecolor('white')

    steps = [
        (0.3, 'Git Push', '#6366F1'),
        (2.0, 'Lint\n(Spotless + ESLint)', '#8B5CF6'),
        (4.0, 'Test Backend\n(Unit + IT)', '#2563EB'),
        (6.0, 'Test Frontend\n(Vitest + TSC)', '#059669'),
        (8.0, 'Docker Build\n+ Trivy Scan', '#F59E0B'),
    ]

    for x, text, color in steps:
        rect = FancyBboxPatch((x, 0.8), 1.5, 1.2, boxstyle="round,pad=0.1",
                              facecolor=color, edgecolor='white', linewidth=2)
        ax.add_patch(rect)
        ax.text(x + 0.75, 1.4, text, ha='center', va='center',
                fontsize=8, fontweight='bold', color='white')

    for i in range(len(steps) - 1):
        x1 = steps[i][0] + 1.5
        x2 = steps[i+1][0]
        ax.annotate('', xy=(x2, 1.4), xytext=(x1, 1.4),
                    arrowprops=dict(arrowstyle='->', color=GRAY, lw=2))

    ax.text(5, 2.5, 'CI/CD Pipeline -- GitHub Actions', ha='center',
            fontsize=12, fontweight='bold', color=DARK)

    plt.tight_layout()
    path = os.path.join(IMAGES_DIR, "cicd_pipeline.png")
    plt.savefig(path, dpi=200, bbox_inches='tight', facecolor='white')
    plt.close()
    return path


# ── PDF Document ───────────────────────────────────────────────────────────

class PDF(FPDF):
    def header(self):
        if self.page_no() > 1:
            self.set_font('Helvetica', 'I', 8)
            self.set_text_color(150, 150, 150)
            self.cell(0, 8, 'Quini6 Analytics - Documento Tecnico', align='L')
            self.cell(0, 8, f'Pagina {self.page_no()}', align='R', new_x="LMARGIN", new_y="NEXT")
            self.set_draw_color(200, 200, 200)
            self.line(10, 12, 200, 12)
            self.ln(3)

    def footer(self):
        self.set_y(-15)
        self.set_font('Helvetica', 'I', 7)
        self.set_text_color(180, 180, 180)
        self.cell(0, 10, 'Los sorteos del Quini 6 son eventos independientes (i.i.d.). Analisis descriptivo solamente.', align='C')

    def section_title(self, num, title):
        self.set_font('Helvetica', 'B', 14)
        self.set_text_color(37, 99, 235)
        self.cell(0, 10, f'{num}. {title}', new_x="LMARGIN", new_y="NEXT")
        self.set_draw_color(37, 99, 235)
        self.line(10, self.get_y(), 200, self.get_y())
        self.ln(3)

    def subsection(self, title):
        self.set_font('Helvetica', 'B', 11)
        self.set_text_color(30, 41, 59)
        self.cell(0, 7, title, new_x="LMARGIN", new_y="NEXT")
        self.ln(1)

    def body_text(self, text):
        self.set_font('Helvetica', '', 9)
        self.set_text_color(51, 65, 85)
        self.multi_cell(0, 5, text)
        self.ln(2)

    def bullet(self, text):
        self.set_font('Helvetica', '', 9)
        self.set_text_color(51, 65, 85)
        self.cell(5, 5, '-')
        x = self.get_x()
        self.multi_cell(185, 5, text)

    def add_table(self, headers, rows, col_widths=None):
        if col_widths is None:
            col_widths = [190 / len(headers)] * len(headers)
        # Header
        self.set_font('Helvetica', 'B', 8)
        self.set_fill_color(37, 99, 235)
        self.set_text_color(255, 255, 255)
        for i, h in enumerate(headers):
            self.cell(col_widths[i], 7, h, border=1, fill=True, align='C')
        self.ln()
        # Rows
        self.set_font('Helvetica', '', 8)
        self.set_text_color(51, 65, 85)
        fill = False
        for row in rows:
            if fill:
                self.set_fill_color(241, 245, 249)
            else:
                self.set_fill_color(255, 255, 255)
            for i, cell in enumerate(row):
                self.cell(col_widths[i], 6, str(cell), border=1, fill=True, align='C')
            self.ln()
            fill = not fill
        self.ln(3)

    def add_image_centered(self, path, w=170):
        x = (210 - w) / 2
        self.image(path, x=x, w=w)
        self.ln(5)


def build_pdf():
    pdf = PDF()
    pdf.set_auto_page_break(auto=True, margin=20)

    # ── Cover Page ─────────────────────────────────────────────────────
    pdf.add_page()
    pdf.ln(30)
    pdf.set_font('Helvetica', 'B', 28)
    pdf.set_text_color(37, 99, 235)
    pdf.cell(0, 15, 'Quini6 Analytics', align='C', new_x="LMARGIN", new_y="NEXT")
    pdf.set_font('Helvetica', '', 14)
    pdf.set_text_color(100, 116, 139)
    pdf.cell(0, 10, 'Documento Tecnico', align='C', new_x="LMARGIN", new_y="NEXT")
    pdf.ln(5)
    pdf.set_draw_color(37, 99, 235)
    pdf.line(60, pdf.get_y(), 150, pdf.get_y())
    pdf.ln(10)
    pdf.set_font('Helvetica', '', 11)
    pdf.set_text_color(51, 65, 85)
    pdf.cell(0, 8, 'Arquitectura de Software - Entrevista Tecnica', align='C', new_x="LMARGIN", new_y="NEXT")
    pdf.ln(15)

    # Tech radar on cover
    radar_path = gen_tech_radar()
    pdf.add_image_centered(radar_path, w=100)

    pdf.ln(10)
    pdf.set_font('Helvetica', 'I', 9)
    pdf.set_text_color(180, 180, 180)
    pdf.cell(0, 6, 'Java 21 | Spring Boot 3.3 | React 18 | PostgreSQL | Docker', align='C', new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 6, 'Los sorteos del Quini 6 son eventos independientes (i.i.d.)', align='C', new_x="LMARGIN", new_y="NEXT")

    # ── Page 2: Architecture ───────────────────────────────────────────
    pdf.add_page()
    pdf.section_title('1', 'Arquitectura General')
    pdf.body_text(
        'Quini6 Analytics sigue un patron de arquitectura por capas (Layered Architecture) '
        'con separacion clara de responsabilidades. El sistema esta dividido en: '
        'Presentacion (React), Controladores (REST API), Servicios (logica de negocio), '
        'y Repositorios (persistencia).'
    )

    arch_path = gen_architecture_diagram()
    pdf.add_image_centered(arch_path, w=160)

    pdf.subsection('Decision de Diseno: Monolito Modular')
    pdf.body_text(
        'Se eligio un monolito modular en lugar de microservicios por tres razones: '
        '(1) La escala actual no justifica la complejidad de red y distribucion. '
        '(2) La separacion por paquetes permite extraccion futura a microservicios. '
        '(3) El despliegue y mantenimiento son significativamente mas simples.'
    )

    # ── Page 3: Layers + DB ────────────────────────────────────────────
    pdf.add_page()
    pdf.section_title('2', 'Capas de la Arquitectura')

    layers_path = gen_layered_diagram()
    pdf.add_image_centered(layers_path, w=155)

    pdf.subsection('Patrones Aplicados')
    pdf.add_table(
        ['Patron', 'Implementacion', 'Beneficio'],
        [
            ['Repository', 'Spring Data JPA', 'Abstraccion de persistencia'],
            ['DTO', 'Records Java', 'Inmutabilidad, sin boilerplate'],
            ['Circuit Breaker', 'Resilience4j', 'Graceful degradation'],
            ['Publisher/Listener', 'RabbitMQ', 'Desacoplamiento async'],
            ['AOP Audit', '@Audit annotation', 'Trail de cambios'],
            ['JWT Filter', 'OncePerRequestFilter', 'Auth stateless'],
        ],
        [55, 60, 75]
    )

    pdf.section_title('3', 'Base de Datos')

    db_path = gen_db_schema()
    pdf.add_image_centered(db_path, w=165)

    pdf.subsection('Decisiones de Modelado')
    pdf.bullet('UUID como PK: Evita auto-increment expuesto, mejor para distribucion')
    pdf.bullet('Tabla numeros_sorteados: Tabla aplanada para analytics (CQRS ligero)')
    pdf.bullet('Flyway: Migraciones versionadas como codigo, nunca se modifican ejecutadas')
    pdf.bullet('PostgreSQL + Elasticsearch: Transacciones ACID + busquedas full-text')

    # ── Page 4: Security + CI/CD + Testing ─────────────────────────────
    pdf.add_page()
    pdf.section_title('4', 'Seguridad y Autenticacion')

    jwt_path = gen_jwt_flow()
    pdf.add_image_centered(jwt_path, w=160)

    pdf.body_text(
        'El sistema utiliza JWT (JSON Web Tokens) con HS256 para autenticacion stateless. '
        'El password se almacena con BCrypt. El filtro JWT se ejecuta en cada request '
        'para validar token y establecer SecurityContext.'
    )

    pdf.section_title('5', 'CI/CD y Testing')

    cicd_path = gen_cicd_pipeline()
    pdf.add_image_centered(cicd_path, w=155)

    pdf.subsection('Testing Pyramid')
    testing_path = gen_testing_pyramid()
    pdf.add_image_centered(testing_path, w=100)

    pdf.body_text(
        'La estrategia de testing sigue la piramide: muitos unit tests (rapidos, aislados), '
        'algunos integration tests (con Testcontainers para PG, ES, RabbitMQ), '
        'y pocos E2E tests (futuro). Cobertura minima: 75% en servicios de negocio.'
    )

    # ── Page 5: Trade-offs + Conclusion ────────────────────────────────
    pdf.add_page()
    pdf.section_title('6', 'Trade-offs y Decisiones Tecnicas')

    pdf.add_table(
        ['Alternativa Rechazada', 'Razon'],
        [
            ['Microservicios', 'Complejidad prematura para la escala actual'],
            ['MongoDB', 'Relaciones complejas requieren ACID'],
            ['Redis como DB', 'Cache, no almacenamiento persistente'],
            ['GraphQL', 'REST es suficiente y mas simple'],
            ['JOOM / Exposed', 'JPA tiene mas comunidad en ecosistema Spring'],
            ['Webpack', 'Vite es mas rapido por ESM nativo'],
        ],
        [70, 120]
    )

    pdf.section_title('7', 'Lecciones Aprendidas')
    pdf.bullet('TypeScript evita bugs que serian runtime errors en JavaScript puro')
    pdf.bullet('El disclaimer legal es critico en apps de loteria -- debe ser visible en cada vista')
    pdf.bullet('Docker Compose es invaluable para desarrollo y demos (8 servicios, 1 comando)')
    pdf.bullet('Testcontainers descubrieron problemas con JPA lazy loading')
    pdf.bullet('Monitoring desde el dia 1 detecto memory leak en HikariCP')

    pdf.ln(5)
    pdf.section_title('8', 'Conclusion')
    pdf.body_text(
        'Este proyecto demuestra la aplicacion de patrones de arquitectura de software en un caso real: '
        'capas bien definidas, separacion de responsabilidades, observabilidad completa, y un pipeline '
        'de CI/CD que garantiza calidad. Los conceptos aplicados -- JWT, circuit breakers, CQRS ligero, '
        'event-driven architecture, infrastructure as code -- son directamente transferibles a cualquier '
        'proyecto empresarial.'
    )

    # Save
    output_path = os.path.join(OUTPUT_DIR, "Quini6_Analytics_Documento_Tecnico.pdf")
    pdf.output(output_path)
    return output_path


if __name__ == "__main__":
    print("Generando diagramas...")
    print(f"  Architecture: {gen_architecture_diagram()}")
    print(f"  Layers: {gen_layered_diagram()}")
    print(f"  Testing: {gen_testing_pyramid()}")
    print(f"  JWT Flow: {gen_jwt_flow()}")
    print(f"  DB Schema: {gen_db_schema()}")
    print(f"  Tech Radar: {gen_tech_radar()}")
    print(f"  CI/CD: {gen_cicd_pipeline()}")
    print("\nGenerando PDF...")
    path = build_pdf()
    print(f"PDF generado: {path}")
