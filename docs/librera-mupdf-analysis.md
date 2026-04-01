# LibreraReader MuPDF Implementation Analysis

**Date:** 2026-04-01
**Source:** `/home/samurai/dev/git/LibreraReader/`

## Overview

LibreraReader does **NOT** use `com.artifex.mupdf.viewer.DocumentActivity`. It has a completely custom MuPDF integration that builds its own UI on top of the low-level rendering engine.

## Integration Method

### Native Libraries
- **Custom JNI bridge:** `libmupdf-librera.c` (1735+ lines) connecting MuPDF C library to Java
- **Two native libraries loaded:**
  - `MuPDF` - Custom Librera JNI bridge
  - `mupdf_java` / `mupdf_java64` / `mupdf_java32` - Official MuPDF Java bindings

### Java Package Structure

| Package | Purpose |
|---------|---------|
| `com.artifex.mupdf.fitz.*` | Official MuPDF Java bindings (62 files) - Context, Document, Page, Pixmap, Matrix, Device, etc. |
| `org.ebookdroid.droids.mupdf.codec.*` | Librera's codec layer (11 files) - MuPdfDocument, MuPdfPage, links, outlines, text extraction |

## Architecture

### Custom Reader Activities
- `VerticalViewActivity` - Primary PDF/eBook reader (vertical scrolling)
- `HorizontalViewActivity` - Horizontal page-by-page reader
- `ViewerActivityController` - Controller for viewer activities

### Rendering Pipeline
1. **Document Opening** - Native `open()` call with CSS, anti-aliasing, and accelerator cache
2. **Page Loading** - Native `open(dochandle, pageno)` loads page into MuPDF display list
3. **Bitmap Rendering** - Pages rendered to Android Bitmaps via display list + draw device pipeline
   - Pixels written directly into Java `int[]` buffer (RGB_565)
   - Native code creates `fz_pixmap` backed by the Java array
4. **Text Extraction** - Two code paths based on MuPDF version (1.11 vs 1.16+)

### Manifest
- **No MuPDF-specific entries** - All file handling goes through custom activities
- File type intent filters for PDF, EPUB, DJVU, CBZ, CBR, MOBI, FB2, TXT, HTML, RTF, DOCX, ODT, XPS, MD, TIFF, AZW, AZW3, and more

## MuPDF Versions
- Runtime version detection via `getFzVersion()`
- Supports both MuPDF 1.11 and 1.16+ APIs
- Builder directory contains source from MuPDF 1.23.7 and 1.24.6

## Key Differences from Codex

| Aspect | LibreraReader | Codex |
|--------|---------------|-------|
| MuPDF integration | Low-level rendering only (`com.artifex.mupdf.fitz.*`) | Full viewer AAR (`com.artifex.mupdf:viewer:1.15.+`) |
| UI | Custom activities (`VerticalViewActivity`, `HorizontalViewActivity`) | Uses bundled `DocumentActivity` |
| Manifest | No MuPDF entries | Requires `DocumentActivity` entry with `android:exported="false"` |
| Rendering | Direct `int[]` buffer rendering | Viewer AAR handles rendering internally |
| Native libs | Custom JNI + official bindings | Bundled in AAR (`libmupdf_java.so`) |

## Implications for Codex

The `android:exported="false"` fix applied to `AndroidManifest.xml` for `DocumentActivity` is correct for the current AAR-based approach. If we ever want to match LibreraReader's flexibility (custom UI, no external activity dependencies), we would need to:

1. Drop the viewer AAR in favor of low-level `fitz` bindings
2. Implement custom JNI bridge or use official Java bindings directly
3. Build our own Compose-based PDF reader UI
4. Handle rendering pipeline ourselves (bitmap generation, zoom, pan, etc.)
