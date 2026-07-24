package com.example.data

object DefaultTemplates {

    val SYSTEM_ARCHITECTURE_TEMPLATE = """
# LoongArch Engine Architecture Spec
> **Version 2.0.0** | Author: UN Portfolio Technical Lead | Paper: A4

---

## 1. Executive Summary
This document specifies the **LoongPDF Document & Diagram Engine** architecture. LoongPDF processes raw Markdown text, parses embedded **Mermaid flowchart diagrams**, renders vector graphics, and packages the results into standardized PDF publications.

### Key Performance Targets:
- **Parser Speed:** < 50ms per 1,000 Markdown lines
- **Mermaid Render:** Pure vector Canvas node rendering with smooth arrows
- **PDF Engine:** Native Android `PdfDocument` pagination with headers & footers

---

## 2. System Layer Diagram

```mermaid
flowchart TD
    MarkdownInput[Markdown Source Code] --> Parser[LoongParser AST]
    Parser --> MermaidEngine[Mermaid Diagram Renderer]
    Parser --> StyleFormatter[M3 Document Styler]
    MermaidEngine --> CanvasVector[Vector Drawing Canvas]
    StyleFormatter --> LayoutPaginator[PDF Page Layout Engine]
    CanvasVector --> LayoutPaginator
    LayoutPaginator --> PdfDocument[Target PDF Publication]
```

---

## 3. Sequence Flow

```mermaid
sequenceDiagram
    participant User
    participant Editor
    participant Parser
    participant PDFEngine
    participant Storage

    User->>Editor: Type Markdown & Mermaid code
    Editor->>Parser: Parse AST & Flowchart Nodes
    Parser->>PDFEngine: Send Render Tree
    PDFEngine->>Storage: Save PDF File
    PDFEngine-->>User: Display Live Preview
```

---

## 4. Feature Matrix

| Feature Module | Support Level | Export Compatibility |
| :--- | :--- | :--- |
| **Headings & Typography** | Full (H1 to H6) | Vector Embedded Fonts |
| **Mermaid Diagrams** | Flowcharts & Sequences | Scalable Paths |
| **Code Blocks** | Monospaced Syntax | Dark Theme Box |
| **Data Tables** | Bordered & Styled Rows | Multi-page Overflow |

---

> *"Build with self-reliance, precision, and elegance."*
""".trimIndent()

    val EXECUTIVE_REPORT_TEMPLATE = """
# Q3 Digital Transformation Report
> **Prepared for:** Executive Leadership Board
> **Author:** Business Intelligence Office

---

## Strategic Overview
During Q3, our engineering and product teams executed a comprehensive overhaul of our mobile publishing stack. Transitioning to **Jetpack Compose** and local-first SQLite persistence delivered immediate speed improvements and offline reliability.

### Key Quarterly Milestones
- **Offline PDF Processing:** Reduced server dependency by **100%**
- **Document Creation Rate:** Increased by **42%** month-over-month
- **System Uptime:** Maintained **99.99%** operational stability

---

## Operations Flowchart

```mermaid
flowchart TD
    Ingest[Data Ingestion] --> Audit[Automated Audit]
    Audit --> Decision{Pass Verification?}
    Decision -->|Yes| Render[Compile PDF Report]
    Decision -->|No| Review[Manual Compliance Review]
    Review --> Render
    Render --> Publish[Distribute to Board]
```

---

## Financial Highlights

| Category | Allocated Budget | Actual Expense | Variance |
| :--- | :--- | :--- | :--- |
| **Core Architecture** | ${'$'}45,000 | ${'$'}42,300 | +${'$'}2,700 |
| **UI/UX Refinement** | ${'$'}20,000 | ${'$'}18,500 | +${'$'}1,500 |
| **Security Audit** | ${'$'}15,000 | ${'$'}15,000 | ${'$'}0 |

```
// Sample Report Config
{
  "report_id": "REP-2026-Q3",
  "confidential": true,
  "export_format": "PDF/A-1b"
}
```

---
*End of Report. Confidential document intended for authorized personnel.*
""".trimIndent()

    val RESUME_CV_TEMPLATE = """
# Alex Mercer
**Senior Software Architect & Systems Designer**
*Email: alex.mercer@dev.io | GitHub: @alexmercer | Location: San Francisco, CA*

---

## Professional Summary
Accomplished Software Architect with **8+ years** of experience designing high-throughput mobile platforms, document processing engines, and reactive UI frameworks. Specialist in Kotlin, Jetpack Compose, and custom graphics rendering.

---

## Core Competencies
- **Languages & Frameworks:** Kotlin, Java, Jetpack Compose, Coroutines, Flow, Room DB, SQL
- **Architecture & System Design:** Clean Architecture, MVVM, Multi-module Gradle, AST Parsers
- **Graphics & Documents:** Android Canvas API, PDF Engine, SVG/Vector pathing, Data Visualization

---

## Professional Experience

### Lead Mobile Architect — DataCraft Systems
*2022 — Present*
- Architected an offline-first document publishing platform used by over **250,000** active professionals.
- Designed a zero-dependency Markdown & Mermaid diagram parser reducing PDF export latency by **65%**.

### Senior Android Engineer — Velocity Apps
*2019 — 2022*
- Led a team of 6 engineers building real-time data sync modules with Room DB and Kotlin Flow.
- Achieved a **99.8%** crash-free user rate across 1M+ app installs.

---

## Education
- **B.S. in Computer Science & Engineering** — Stanford University (*Magna Cum Laude*)
""".trimIndent()

    fun getInitialDocuments(): List<DocumentEntity> {
        return listOf(
            DocumentEntity(
                title = "LoongArch Engine Spec",
                author = "UN Portfolio Technical Lead",
                content = SYSTEM_ARCHITECTURE_TEMPLATE,
                category = "Technical",
                paperSize = "A4",
                primaryColorHex = "#0F172A",
                isFavorite = true
            ),
            DocumentEntity(
                title = "Q3 Digital Transformation Report",
                author = "Business Intelligence Office",
                content = EXECUTIVE_REPORT_TEMPLATE,
                category = "Report",
                paperSize = "A4",
                primaryColorHex = "#1E3A8A",
                isFavorite = false
            ),
            DocumentEntity(
                title = "Alex Mercer — CV Resume",
                author = "Alex Mercer",
                content = RESUME_CV_TEMPLATE,
                category = "Resume",
                paperSize = "Letter",
                primaryColorHex = "#065F46",
                isFavorite = true
            )
        )
    }
}
