# Test Data Directory

This directory contains subdirectories for testing different eBook formats supported by Codex.

## Supported Formats

- **epub/**: Place EPUB files here for testing EPUB format support
- **pdf/**: Place PDF files here for testing PDF format support
- **txt/**: Place TXT files here for testing plain text format support
- **fb2/**: Place FB2 files here for testing FictionBook format support

## Usage

1. Place your test eBook files in the appropriate subdirectory based on their format
2. Build and run the app in Android Studio
3. In the app, go to Browse screen
4. Navigate to this testdata directory
5. Select and add books to test the parsing and reading functionality

## Notes

- Make sure the files have appropriate extensions (.epub, .pdf, .txt, .fb2)
- Test files should contain valid content for the respective format
- You can add more subdirectories for other supported formats as needed