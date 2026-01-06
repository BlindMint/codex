# Things to add

# Optional - implement later

## Support calibre .opf files
Add support for Calibre external metadata (.opf) files. After processing, Calibre saves each book to its own directory and creates an .opf file with the metadata. If a book is added and there is a .opf file in that directory, the book's metadata, description, etc should be updated within Codex. Each metadata.opf file applies to only a single book, and the book's title is declared in the .opf file, so if multiple books are in a directory with a single metadata.opf file, it should apply to the proper book. Below is an example file structure for a single book directory.

../Site Reliability Engineering (2727) $ ls -l
total 10072
-rw-r--r--. 1 user user   191438 Oct  6  2023  cover.jpg
-rw-r--r--. 1 user user     1472 Oct  6  2023  metadata.opf
-rw-r--r--. 1 user user 10114893 Oct  6  2023 'Site Reliability Engineering - Betsy Beyer, Chris Jones, Jennifer Petoff.pdf'

Here is an the metadata.opf file from the above directory:
<?xml version='1.0' encoding='utf-8'?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="uuid_id" version="2.0">
    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
        <dc:identifier opf:scheme="calibre" id="calibre_id">2727</dc:identifier>
        <dc:identifier opf:scheme="uuid" id="uuid_id">4947081d-cda2-4f6e-bd0f-db1ade7c7f95</dc:identifier>
        <dc:title>Site Reliability Engineering</dc:title>
        <dc:creator opf:file-as="Betsy Beyer, Chris Jones, Jennifer Petoff &amp; Murphy, Niall Richard" opf:role="aut">Betsy Beyer, Chris Jones, Jennifer Petoff</dc:creator>
        <dc:creator opf:file-as="Betsy Beyer, Chris Jones, Jennifer Petoff &amp; Murphy, Niall Richard" opf:role="aut">Niall Richard Murphy</dc:creator>
        <dc:contributor opf:file-as="calibre" opf:role="bkp">calibre (5.43.0) [https://calibre-ebook.com]</dc:contributor>
        <dc:date>2016-03-21T15:03:26+00:00</dc:date>
        <dc:language>en</dc:language>
        <meta name="calibre:author_link_map" content="{&quot;Betsy Beyer, Chris Jones, Jennifer Petoff&quot;: &quot;&quot;, &quot;Niall Richard Murphy&quot;: &quot;&quot;}"/>
        <meta name="calibre:timestamp" content="2023-10-06T19:25:40.149374+00:00"/>
        <meta name="calibre:title_sort" content="Site Reliability Engineering"/>
    </metadata>
    <guide>
        <reference type="cover" title="Cover" href="cover.jpg"/>
    </guide>
</package>

