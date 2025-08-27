## Omni-Files

The goal of Omni-Files is to create a fully functional files app which can also view (and sometimes 
edit) almost every file that you have on your phone.

### Currently Supported File Types

#### Text-ish files
All text-ish files can be edited and saved

| Extension | Formatting | Highlighting |
|-----------|------------|--------------|
| .txt      | N/A        | N/A          |
| .md       | ❌          | N/A          |
| .rtf      | ❌          | N/A          |
| .json     | ✅          | ✅            |
| .py       | ✅          | ✅            |
| .java     | ✅          | ✅            |
| .c        | ✅          | ✅            |
| .cpp      | ✅          | ✅            |
| .kt       | ✅          | ✅            |
| .sh       | ✅          | ✅            |
| .js       | ✅          | ✅            |
| .php      | ✅          | ✅            |
| .go       | ✅          | ✅            |
| .swift    | ✅          | ✅            |
| .cs       | ✅          | ✅            |
| .rb       | ✅          | ✅            |

#### Audio
All audio file formats supported by Android can be opened and played

#### Video
All video file formats supported by Android can be opened and played

#### Image
All image file formats supported by Android can be opened and viewed

#### APKs
You can install apk files to your device

#### Calendar
iCal (.ics) files can be opened and viewed in in a human readable way, and the events can be added to your on-device calendar

wishlist: allow adding events from ics files to an existing calendar

#### Contacts
vCard (.vcf, .vcard) files can be opened and viewed in a human readable way, and the contacts can be added to your on-device contacts

#### Archives
Currently only zip archives can be extracted, but the contents cannot be viewed prior to extraction

| Extension | Exploring | Extracting |
|-----------|-----------|------------|
| .zip      | ❌         | ✅          |
| .tar      | ❌         | ❌          |
| .rar      | ❌         | ❌          |
| .7z       | ❌         | ❌          |
| .gz       | ❌         | ❌          |

Files and folders can also be selected and compressed to a .zip file

#### PDFs
PDFs are viewable, and text is selectable and interactable (you can click on links)

wishlist: searching for text

#### Spreadsheets
.csv, .tsv are both viewable in a spreadsheet view

.ods (OpenDocument Spreadsheet) files are also viewable in a spreadsheet view, and support many visual features

Lots of work still needs to be done here

#### Wishlist file types

After spreadsheets, I want to add support for word docs and presentations. 
I will first work on supporting the OpenDocument formats (.odt, .odp) before supporting Microsoft's files

I also want to open 3D model files using this app (.obj, .stl)