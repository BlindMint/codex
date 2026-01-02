# Things to add

I've decided to put asside the context selection for now. I am happy with the current behavior of the text selection and might return to the context selection later. For now, It's working as-is, so I think it might be safe to just leave (you can comment out mentions or imports to the context selection file, that might be safer just in case). I have updated ~/dev/codex/todo.md with some additional updates that I would like to pursue. We don't need to look at the "# Optional" features, but we can update that later.

## Bookmarks
Add Bookmark icon to in-book reader menu (Beside search, etc)
Add "Bookmark" option to long-press menu (Already done)
Have Bookmark side panel similar to chapter panel but to allow users to navigate through bookmarks they've saved (clicking on a bookmark instantly moves the book to that location)
Keep per-book memory/storage of bookmarks.
I am also considering creating a new tab for "Bookmarks" under the current "Chapters" item. Currently, hitting the "Chapters" icon opens a menu with the book's detected chapters. It seems to me that this panel could be structured with multiple tabs: Chapters and Bookmarks.
This keeps the in-app UI clean (1 less icon than adding a separate bookmark icon). Do you think this is preferable or would it be better UX to have a new button for each?

There should be an intuitive way to remove bookmarks from within the bookmarks menu (prompt user upon deletion)

## Add direct RGB and HEX color support for colors
The app comes with pre-set Light and Dark themes. Users can create their own themes by setting the background and font colors. Currently, users have to use sliders to precisely set RGB color values for each red, green, and blue. I want to make this easier for users. In the line with "Background color", I want to add a box that lets users manually enter a HEX color code. The sliders/colors should update with whatever the user puts in and the theme/colors should set accordingly. The same should go for Font color below.

Also, using the sliders for RGB is a little annoying when trying to get very precise. I want users to be able to tap on "Red" or its value right below, and have a dialog open or the current value box appear editable (whatever is Material 3 guidelines) so users can simply type in a value and have it set. Users should be able to do this for Red Green and Blue under Background colors and Font colors.

Make sure the in-book settings menu is updated as well as the primary Settings > Reader menu settings.

I think it would also be useful to add a "lock" icon to the left of the "delete" icon (with a little space to avoid accidental presses) that locks a custom theme - when locked the user cannot rename, modify, or delete the theme. They should be able to press the lock icon again to unlock, and there should be a confirmation asking if the user really wants to unlock. Please add a confirmation when locking as well.

I think these are good usability improvements. Can you examine the app and think of any other improvements in usability or similar? 


## Modify Import/Export order
Under Settings > Import/Export, please position the Import button first and Export button second.


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

