Submission Template for ACM Papers

This is Short Title of the paper, used in page headers

This is the subtitle of the paper, this document both explains and embodies the submission format for authors using Word

First Author's Name[^1]\*

[^1]: \* Place the footnote text for the author (if applicable) here.

First author's affiliation, an Institution with a very long name, xxxx@gmail.com

Second Author's Name

Second author's affiliation, possibly the same institution, xxxx@gmail.com

Third Author's Name

Third author's affiliation, possibly the same institution, xxxx@gmail.com

Although there is no distinctive header, this is the abstract. This submission template allows authors to submit their papers for review to an ACM Conference or Journal without any output design specifications incorporated at this point in the process. The ACM manuscript template is a single column document that allows authors to type their content into the pre-existing set of paragraph formatting styles applied to the sample placeholder text here. Throughout the document you will find further instructions on how to format your text. If your conference’s review process will be double-blind: The submitted document should not include author information and should not include acknowledgments, citations or discussion of related work that would make the authorship apparent. Submissions containing author identifying information may be subject to rejection without review. Upon acceptance, the author and affiliation information must be added to your paper.

**CCS CONCEPTS** • Insert your first CCS term here • Insert your second CCS term here • Insert your third CCS term here

Additional Keywords and Phrases: Insert comma delimited author-supplied keyword list, Keyword number 2, Keyword number 3, Keyword number 4

ACM Reference Format:

First Author’s Name, Initials, and Last Name, Second Author’s Name, Initials, and Last Name, and Third Author’s Name, Initials, and Last Name. 2018. The Title of the Paper: ACM Conference Proceedings Manuscript Submission Template: This is the subtitle of the paper, this document both explains and embodies the submission format for authors using Word. In Woodstock ’18: ACM Symposium on Neural Gaze Detection, June 03–05, 2018, Woodstock, NY. ACM, New York, NY, USA, 10 pages. NOTE: This block will be automatically generated when manuscripts are processed after acceptance.

1.  Introduction

ACM's new manuscript submission template aims to provide consistent styles for use across ACM publications, and incorporates accessibility and metadata-extraction functionality necessary for future Digital Library endeavors. Numerous ACM and SIG-specific templates have been examined, and their unique features incorporated into this single new template. If you are new to publishing with ACM, this document is a valuable guide to the process of preparing your work for publication. If you have published with ACM before, this document provides insight and instruction into the current process for preparing\` your manuscript.

This submission template allows authors to submit their papers for review to an ACM Conference or Journal without any output design specifications incorporated at this point in the process. The ACM “Submission Template” is a single column MS-Word document that allows authors to type their content into the pre-existing set of paragraph formatting styles applied to the sample placeholder text here, or copy-and-paste their text and then apply the respective paragraph styles (**Windows**: you can open the Styles task pane from the **Home** tab [it can also be opened with the keyboard shortcut Alt+Ctrl+Shift+S]; **MAC16**: you can access the Styles pane at the right of the **Home** toolbar.) Highlight a section that you want to designate with a certain style, and then select the appropriate style from the list. To view which style is being used in any part of this document, place your cursor on your text and look at the “Current style” field in the Styles pane.

It is beneficial to create your document in draft mode with the style panel open in the left-side panel. If the panel is not immediately visible when the Submission Template is opened, you will need to open the panel manually—for Windows: click on the following from the main ribbon above: File \> Options \> Advanced \> Display \> Style area pane width in Draft and Outline views. Set the style area width (1–1.5" is a good starting value.); for MAC: go to the “**View**” menu and select “**Draft**”; then go to the “**Word**” menu and select “**Preferences**” and then “**View**,” under the “**Window**” section insert “1.5” inches under the style area width.

All style elements are specified in this template to facilitate the production of your paper and to have the styles consistent throughout. The paragraph styles are built-in and examples of the styles are provided throughout this document. Save as you go and backup your work regularly!

1.  Accessibility

Following the guidelines throughout this template will also improve the accessibility of your manuscript and increase the audience for your work. Ensure that heading styles are applied as instructed, tables are created using Word’s table feature (rather than an image), figures have a text equivalent, and list styles are applied as instructed.

To increase the accessibility of your manuscript, you should set the title and language metadata. On Word for Windows, open the File tab and click on Info. On Word for Mac, click the File Menu and select Properties, then click the Summary tab. Fill in the title of your document. For anonymous review, clear the ‘author’ field.

To set the document language, click the Review tab in the Ribbon. On Word for Windows: Click the Language button and select “Set Proofing Language.” Verify the language is set correctly. On Word for Mac: Click the Language button and select the document language from the pop-up.

1.  More about the submission template

This submission version of your paper should not have headers or footers, these will be added when your manuscript is processed after acceptance. It should remain in a one-column format—please do not alter any of the styles or margins.

*If a paper is accepted for publication*, authors will be instructed on the next steps. Authors must then follow the submission instructions found on their respective publication’s web page. Once your submission is received, your paper will be processed to produce the formatted Word, PDF, and HTML5 output formats, which will be provided to you for review, revision/resubmission (if applicable), and approval.

1.  Inserting CCS concepts

The new template enables you to import required indexing concepts for your article from the [ACM Computing Classification System (CCS)](http://www.acm.org/publications/class-2012) using an [indexing support tool](http://dl.acm.org/ccs/ccs.cfm?) found in the ACM Digital Library (DL). The tool generates formatted text after you have selected your terms. To insert CCS terms into your document, copy and paste the formatted text from the CCS tool using the “<https://dl.acm.org/ccs/ccs.cfm>” link into the “CCS CONCEPTS” section.

An additional step is necessary to ensure that the proper CCS terms are added to the Digital Library citation page: from the “view CCS TeX Code” listing, click on “Show the XML Only.” Highlight and copy the XML code from the window. You must insert the XML code into your Word document’s properties: from your Word document, click on “**File**”, then click on the “**Info**” tab on the left-hand side panel, then click “**Properties**” and select “**Show All Properties.**” Click within the “Comments” metadata field and paste the XML data.

2.  Inserting Content Elements

The next subsections provide instructions on how to insert figures, tables, and equations in your document.

1.  Tables

Tables are “float elements” which should be inserted after their first text reference and have specific styles for identification. Do not use images to present tables, or they will be inaccessible to readers using assistive technologies.

Authors can insert tables by using the MS Word option (INSERT -\>Table) and providing the required row and column size. Every table must have a caption (title) above it, which must have the **“TableCaption**” style applied. Please note that tables **should not** be supplied as image files, but if they are images they must have the “Image” style applied. As an example, Table 1 shows all the styles available in this template, to be applied to the respective element of your text.

Table 1: Styles available in the Word template

| Style Tag                      | Definition                                                                                    | Style Tag                  | Definition                                |
|--------------------------------|-----------------------------------------------------------------------------------------------|----------------------------|-------------------------------------------|
| Title_document                 | main title of article                                                                         | ListParagraph              | list items                                |
| Subtitle                       | subtitle of article                                                                           | Statements                 | math statements                           |
| Authors                        | author name                                                                                   | Extract                    | block quotations                          |
| Affiliation                    | author affiliation information                                                                | Algorithm Caption          | caption for algorithm                     |
| AuthNotes                      | footnote to author(s)                                                                         | AckHead                    | heading for acknowledgements              |
| Abstract                       | abstract text                                                                                 | AckPara                    | acknowledgements text                     |
| CCSHead                        | heading for CSS Concepts                                                                      | GrantSponsor               | sponsor of grant                          |
| CCSDescription                 | CSS terms                                                                                     | GrantNumber                | number for the grant                      |
| KeyWordHead                    | heading for keywords                                                                          | ReferenceHead              | heading for references                    |
| Keywords                       | keywords text                                                                                 | Bib_entry                  | references                                |
| ORCID                          | author's ORCHID \#                                                                            | AppendixH1                 | appendix heading level 1                  |
| Head1                          | heading level 1                                                                               | AppendixH2                 | appendix heading level 2                  |
| Head2                          | heading level 2                                                                               | AppendixH3                 | appendix heading level 3                  |
| Head3                          | heading level 3                                                                               | TableCaption               | title of table                            |
| PostHeadPara                   | first paragraph after a heading                                                               | TableHead<br>TableFootnote | column head of table<br>footnote to table |
| Para                           | Subsequent paragraphs of general text                                                         | Image                      | figures                                   |
| ParaContinue<br>DisplayFormula | flush left text after display items like math equations, lists etc.<br>numbered math equation | DOI                        | Digital object identifier                 |
| DisplayFormulaUnnum            | unnumbered equations                                                                          | Label                      | labela                                    |
| ComputerCode                   | Display Computer codes                                                                        | In-text code               | intext computer code                      |
| Short Title                    | Short title of article                                                                        | History                    | Dates of article                          |

a This is example of table footnote.

Tables can be very difficult for people using screen reader technology to understand unless they include markup that explicitly defines the relationships between all the parts (i.e.: headers and data cells). *A key to making data tables accessible to screen reader users is to clearly identify column and row headers.* In Word, authors should identify which row or rows contain column headers. Below are the steps to do this:

Select that table’s row, then right-click the row and select “Table Properties”;

In the *Table Properties* window, click the *Row* tab and select the box that says “Repeat as header row at the top of each page.”

Or

Apply the “table head” style by highlighting the respective row and applying the “**TableHead**” style found in the “Body Element” section of the ACM Master Article Template.

1.  Figures

Figures are “float elements” which should be inserted after their first text reference, and have specific styles for identification. Insert a figure and apply the “**Image**” paragraph style to it. For the figure caption, apply the style “**FigureCaption.**”

To accommodate readers with color vision differences, figures should still be usable when printed in grayscale. Refer to elements of the figure with non-color terms, for example “indicated as squares” instead of “indicated in blue”. Use different patterns in bar charts, different line patterns in graphs, and different shapes in plots to distinguish groups of elements and reinforce color differences.

1.  Half Width Figures.

Figure 1 is an example of a figure and caption spanning the half-page width (one column in a two column format) with the styles applied. If your figure contains third-party material, you must clearly identify it as such, as shown in the example below.

![Figure 1: A woman and a girl in white dresses sit in an open car. The image is centered and occupies 1/3 of the page width.](media/f1181da200d46c167abc0e8e46a0a4c8.png)

Figure 1: 1907 Franklin Model D roadster. Photograph by Harris & Ewing, Inc. [Public domain], via Wikimedia Commons. (https://goo.gl/VLCRBB)

1.  Full Width Figures.

Figure 2 is an example of a figure and caption spanning the full-page width with the styles applied. If your figure contains third-party material, you must clearly identify it as such, as shown in the examples.

![Figure 2. A woman stands to the left of the Bombe Machine, which is roughly 2.1 meters wide and 2 meters tall.  There are nine rows of rotating drums, each holding twelve drums.](media/35b4be0179571109517f9519914d5fa1.jpg)

Figure 2: Mockup of a bombe machine at Bletchley Part. Photograph by Sarah Hartwell. [Public domain], via Wikimedia Commons. (<https://commons.wikimedia.org/wiki/File:TuringBombeBletchleyPark.jpg>)

1.  Multi-part figure.

Authors can also insert a multi-part figure above a single caption. Every inserted figure must have the “Image” style applied. Below are instructions regarding how to insert a multi-part figure in your paper.

-   If the author wants to insert two multi-part images, they must draw a one row and one column table and insert the images one-by-one in the cells.
-   If the author wants to insert three multi-part images, they must draw a one-row and three-column table and insert the images one by one in all three cells.
-   If the author wants to insert four multi-part images, they must draw a two-row and two-column table and insert the images one-by-one in all four cells. (see the following example):

| ![Figure 2: The layout of multipart images should be as per the above example within the table in image 1.](media/4cc98347fe93e3a8feaa130a9dcb9ed0.jpeg) | ![Figure 2: The layout of multipart images should be as per the above example within the table in image 2.](media/ccd75604ad02437a284be49be0be6c4f.jpeg) |
|----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![Figure 2: The layout of multipart images should be as per the above example within the table in image 3.](media/4ede6f5c913290cffb72d8c76216f87d.jpeg) | ![Figure 2: The layout of multipart images should be as per the above example within the table in image 4.](media/7f32180cb29ed342c60d103b8e218ba9.jpeg) |

Figure 3: The layout of multipart images should be as per the above example within the table. All images must have the “Image” style applied.

1.  Figure Descriptions.

Every figure should have a figure description unless it is purely decorative. These descriptions convey what’s in the image to someone who cannot see it. They are also used by search engine crawlers for indexing images, and when images cannot be loaded.

A figure description must be unformatted plain text less than xxx characters long. Figure descriptions should not repeat the figure caption – their purpose is to capture important information that is not already provided in the caption or the main text of the paper. For figures that convey important and complex new information, a short plain text description may not be adequate. More complex alternative descriptions can be placed in an appendix and referenced in a short figure description. For example, provide a data table capturing the information in a bar chart, or a structured list representing a graph. For additional information regarding how best to write figure descriptions and why doing this is so important, please see [https://www.acm.org/accessibility.](https://www.acm.org/accessibility)

The instructions below describe the required steps authors need to follow in order to insert descriptive text for figures (alt-txt value) in **MS Word 2019 on Windows or Word 2016 and later on Mac**:

1.  Insert a picture in the document.
2.  Right-click the image and select “Edit Alt Text”.
3.  In the “alt text” section, provide your text description of the image.

Below are the steps to insert figure descriptions in **MS Word 2013 and 2016**:

1.  Insert a picture in the document.
2.  Right click on the inserted picture and select the **Format Picture** option.
3.  In the settings at the right side of the window, click on the “Layout & Properties” icon (3rd option).
4.  Expand **Alt Txt** option.
5.  In the “Title” and “Description” text boxes, type the text you want to represent the figure, and then click “Close.”

Below are steps to insert the alt-txt value in **MS Word 2010/2011 for Windows\***:

1.  Insert a picture in the document.
2.  Right click on the inserted picture and select the **Format Picture** option.
3.  Select the **Alt Txt** option from the left-side panel options.
4.  In the “Title” and “Description” text boxes, type the text you want to represent the picture, and then click “Close.”  
    \* The Mac 2011 version 14.0.0 and later allows the option for inserting “alt-text.” In the MAC version of Word 2016, right-click on the image and select “Edit Alt Text” from the pop-up menu and then enter the description for the alt text.
    1.  Quotations and Extracts

There are styles for block quotations, which should be used for quotes that are separated from in-line text. Below is an example.

“Microsoft tried to revive the idea of an assistant with Clippy, who began popping up in Microsoft Office in 1997. Its creator, Kevan Atteberry, was actually contracted by Microsoft to design Clippy, which, funnily enough, he did on a Mac … Sure, people could disable Clippy, but the fact he was on by default angered people.” [10]

1.  Equations

There are two types of math equations: the *numbered display math equation* and the *un-numbered display math equation*. Below are examples of both.

1.  **DisplayFormula.**

*The* **DisplayFormula** *style is applied in the numbered math equation. A numbered display equation always has an equation number (label) on the right.*

$$\frac{x = - b \pm \sqrt{b^{2} - 4 a c}}{2 a}$$ (1)

1.  DisplayFormula.Unnum**.**

*The* **DisplayFormulaUnnum** *style is applied only in unnumbered equations. An unnumbered display equation never contains an equation number Bertot and Grimes (2012) on the right—this element distinguishes it from the numbered equation.*

$$
\frac{x = - b \pm \sqrt{b^{2} - 4 a c}}{2 a}
$$

*Please note: the subsequent text after the* **DisplayFormula** *(numbered equation) or* **DisplayFormulaUnnum** *(unnumbered equation) must have the paragraph style* **ParaContinue** *applied.*

1.  Math statements

Math statements should have the “Statement” style applied.

**Theorem/Proof/Lemma.** Math statements should have the “**Statement**” style applied. This paragraph is an example of the “**Statement**” style.

1.  Algorithms

Algorithms use the styles “AlgorithmCaption” and “Algorithm”.

ALGORITHM 1: Iterative Algorithm

current_position center

current_direction up

current_position is inside circle

while current_position is inside circle, do

neighborhood all grid hexes within two hexes from current_position

for each hex in neighborhood, do

for each neuron in hex do

convert neuron_orientation to vector

scale vector by neuron_excitation

vector_sum vector_sum + vector

end

end

normalize vector_sum

end

3.  COMPUTER CODE

Display Computer codes can be inserted using “ComputerCode” style.

CHAT Start

SAY Welcome to my world

WAIT 1.2

SAY Thanks for Visiting

ASK Do you want to play a game?

OPT Sure

OPT No Thanks

Similary, this is an example of intext code text.

Similary, this is an example of intext code text.

4.  MULTI-LANGUAGE PAPERS

Papers may be written in languages other than English or include titles, subtitles, keywords and abstracts in different languages (as a rule, a paper in a language other than English should include an English title and an English abstract). “Select Main Language” from ACM template menu for the main language of the paper, English is the default language and there is no requirement to select it. For example, please select “French” as the main language for a French paper with additional titles and abstracts in English and German.

5.  Citing Related Work

This section cites a variety of journal [5, 15], conference [1, 6, 8, 12, 13], and magazine [3] articles to illustrate how they appear in the references section. It also cites books [9, 10], a technical report [7], a PhD dissertation [4], an online reference [14], a software artifact [11], and a dataset [2].

As you build your article, you should note where you will be placing citations. If you are using numbered citations and references, the reference number - "...as shown in [5]..." is sufficient. If you are using the "author year" style, a reasonable placeholder is the primary author's last name and the year of publication - "...as shown in [Harel 1978]..." - we will be updating this placeholder later in the process with the citation label as generated by the Word macros in the "master template.

ACKNOWLEDGMENTS

Acknowledgments are placed before the references. Add information about grants, awards, or other types of funding that you have received to support your research. Author can capture the **grant sponsor information**, by selecting the grant sponsor text and apply style ‘GrantSponsor’. After this, select grant no and apply ‘GrantNumber’ from style panel. Example of Grant sponsor: Competitive Research Programme and example of Grant no: CRP 10-2012-03.

6.  HISTORY DATES

In case of submissions being prepared for Journals or PACMs, please add history dates after References as (*please note revised date is optional*):

Received November 2019; revised August 2020; accepted December 2020

REFERENCES

1.  Atul Adya, Paramvir Bahl, Jitendra Padhye, Alec Wolman, and Lidong Zhou. 2004. A multi-radio unification protocol for IEEE 802.11 wireless networks. In Proceedings of the IEEE 1st International Conference on Broadnets Networks (BroadNets’04) . IEEE, Los Alamitos, CA, 210–217. https://doi.org/10.1109/BROADNETS.2004.8
2.  Sam Anzaroot and Andrew McCallum. 2013. UMass Citation Field Extraction Dataset. Retrieved May 27, 2019 from <http://www.iesl.cs.umass.edu/data/data-umasscitationfield>
3.  Martin A. Fischler and Robert C. Bolles. 1981. Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography. Commun. ACM 24, 6 (June 1981), 381–395. https://doi.org/10.1145/358669.358692
4.  Chelsea Finn. 2018. Learning to Learn with Gradients. PhD Thesis, EECS Department, University of Berkeley.
5.  Jon M. Kleinberg. 1999. Authoritative sources in a hyperlinked environment. J. ACM 46, 5 (September 1999), 604–632. https://doi.org/10.1145/324133.324140
6.  Matthew Van Gundy, Davide Balzarotti, and Giovanni Vigna. 2007. Catch me, if you can: Evading network signatures with web-based polymorphic worms. In Proceedings of the first USENIX workshop on Offensive Technologies (WOOT ’07) . USENIX Association, Berkley, CA, Article 7, 9 pages.
7.  James W. Demmel, Yozo Hida, William Kahan, Xiaoye S. Li, Soni Mukherjee, and Jason Riedy. 2005. Error Bounds from Extra Precise Iterative Refinement. Technical Report No. UCB/CSD-04-1344. University of California, Berkeley.
8.  David Harel. 1979. First-Order Dynamic Logic. Lecture Notes in Computer Science, Vol. 68. Springer-Verlag, New York, NY. <https://doi.org/10.1007/3-540-09237-4>
9.  Jason Jerald. 2015. The VR Book: Human-Centered Design for Virtual Reality. Association for Computing Machinery and Morgan & Claypool.
10. Prokop, Emily. 2018. The Story Behind. Mango Publishing Group. Florida, USA.
11. R Core Team. 2019. R: A Language and Environment for Statistical Computing. R Foundation for Statistical Computing, Vienna, Austria. https://www.R-project.org/
12. Brian K. Reid. 1980. A high-level approach to computer document formatting. In Proceedings of the 7th Annual Symposium on Principles of Programming Languages. ACM, New York, 24–31. <https://doi.org/10.1145/567446.567449>
13. John R. Smith and Shih-Fu Chang. 1997. Visual Seek: a fully automated content-based image query system. In Proceedings of the fourth ACM international conference on Multimedia (MULTIMEDIA ’96). Association for Computing Machinery, New York, NY, USA, 87–98. https://doi.org/10.1145/244130.244151
14. TUG 2017. Institutional members of the LaTeX Users Group. Retrieved May 27, 2017 from <http://wwtug.org/instmem.html>
15. Alper Yilmaz, Omar Javed, and Mubarak Shah. 2006. Object tracking: A survey. ACM Comput. Surv. 38, 4 (December 2006), 13–es. https://doi.org/10.1145/1177352.1177355

Patricia S. Abril and Robert Plant. 2007. The patent holder's dilemma: Buy, sell, or troll? Commun. ACM 50, 1 (Jan. 2007), 36-44. DOI: <https://doi.org/10.1145/1188913.1188915>

Sarah Cohen, Werner Nutt, and Yehoshua Sagic. 2007. Deciding equivalences among conjunctive aggregate queries. J. ACM 54, 2, Article 5 (April 2007), 50 pages. DOI: https://doi.org/10.1145/1219092.1219093

David Kosiur. 2001. Understanding Policy-Based Networking (2nd. ed.). Wiley, New York, NY.

Ian Editor (Ed.). 2007. The title of book one (1st. ed.). The name of the series one, Vol. 9. University of Chicago Press, Chicago. DOI: https://doi.org/10.1007/3-540-09237-4

Donald E. Knuth. 1997. The Art of Computer Programming, Vol. 1: Fundamental Algorithms (3rd. ed.). Addison Wesley Longman Publishing Co., Inc.

Sten Andler. 1979. Predicate path expressions. In Proceedings of the 6th. ACM SIGACT-SIGPLAN Symposium on Principles of Programming Languages (POPL '79), January 29 - 31, 1979, San Antonio, Texas. ACM Inc., New York, NY, 226-236. <https://doi.org/10.1145/567752.567774>

Joseph Scientist. 2009. The fountain of youth. (Aug. 2009). Patent No. 12345, Filed July 1st., 2008, Issued Aug. 9th., 2009.

David Harel. 1978. LOGICS of Programs: AXIOMATICS and DESCRIPTIVE POWER. MIT Research Lab Technical Report TR-200. Massachusetts Institute of Technology, Cambridge, MA.

Kenneth L. Clarkson. 1985. Algorithms for Closest-Point Problems (Computational Geometry). Ph.D. Dissertation. Stanford University, Palo Alto, CA. UMI Order Number: AAT 8506171.

David A. Anisi. 2003. Optimal Motion Control of a Ground Vehicle. Master's thesis. Royal Institute of Technology (KTH), Stockholm, Sweden.

Harry Thornburg. 2001. Introduction to Bayesian Statistics. (March 2001). Retrieved March 2, 2005 from http://ccrma.stanford.edu/\~jos/bayes/bayes.html

ACM. Association for Computing Machinery: Advancing Computing as a Science & Profession. Retrieved from http://www.acm.org/.

Wikipedia. 2017. Wikipedia: the Free Encyclopedia. Retrieved from https://www.wikipedia.org/.

Dave Novak. 2003. Solder man. Video. In ACM SIGGRAPH 2003 Video Review on Animation theater Program: Part I - Vol. 145 (July 27-27, 2003). ACM Press, New York, NY, 4. DOI: https://doi.org/99.9999/woot07-S422

Barack Obama. 2008. A more perfect union. Video. (5 March 2008). Retrieved March 21, 2008 from http://video.google.com/videoplay?docid=6528042696351994555

Martha Constantinou. 2016. New physics searches from nucleon matrix elements in lattice QCD. arXiv:1701.00133. Retrieved from <https://arxiv.org/abs/1701.00133>

A APPENDICES

In the appendix section, three levels of Appendix headings are available.

A.1 General Guidelines (AppendixH2)

Save as you go and backup your file regularly.

Do not work on files that are saved in a cloud directory. To avoid problems such as MS Word crashing, please only work on files that are saved locally on your machine.

Equations should be created with the built-in Microsoft® Equation Editor included with your version of Word. (Please check the compatibility at <http://tinyurl.com/lzny753> for using MathType.)

Please save all files in DOCX format, as the DOC format is only supported for the Mac 2011 version.

Tables should be created with Word’s “Insert Table” tool and placed within your document. (Tables created with spaces or tabs will have problems being properly typeset. To ensure your table is published correctly, Word’s table tool must be used.)

Do not copy-and-paste elements into the submission document from Excel such as charts and tables.

Footnotes should be inserted using Word’s “Insert Footnote” feature.

Do not use Word’s “Insert Shape” function to create diagrams, etc.

Do not have references appear in a table/cells format as it will produce an error during the layout generation process.

MS Word does not consistently allow the original formatting to be modified in the text. In these cases, it is best to copy all the document’s text from the specific file and paste into a new MS Word document and then save it.

At times there are font problems such as “odd” stuff/junk characters that appear in the text, usually in the references. This can be caused by a variety of reasons such as copying-and-pasting from another file, file transfers, etc. Please review your text prior to submission to make sure it reads correctly.

A.1.1 Preparing Graphics (AppendixH3)

1.  Accepted image file formats: TIFF (.tif), JPEG (.jpg).
2.  Scalable vector formats (i.e., SVG, EPS and PS) are greatly preferred.
3.  Application files (e.g., Corel Draw, MS Word, MS Excel, PPT, etc.) are NOT recommended.
4.  Images created in Microsoft Word using text-box, shapes, clip-art are NOT recommended.
5.  IMPORTANT: All fonts must be embedded in your figure files.
6.  Set the correct orientation for each graphics file.

A.2 Placeholder Text

Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Vulputate sapien nec sagittis aliquam. Malesuada fames ac turpis egestas sed tempus urna. Posuere sollicitudin aliquam ultrices sagittis orci. Consequat id porta nibh venenatis cras sed felis eget. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum odio. Tincidunt nunc pulvinar sapien et ligula ullamcorper malesuada proin. Tincidunt lobortis feugiat vivamus at augue. Eget nunc lobortis mattis aliquam faucibus. Egestas diam in arcu cursus euismod quis.

Erat pellentesque adipiscing commodo elit at imperdiet. In hac habitasse platea dictumst quisque sagittis purus. At lectus urna duis convallis. Eu mi bibendum neque egestas congue. Est ullamcorper eget nulla facilisi etiam dignissim diam. Sed ullamcorper morbi tincidunt ornare massa eget. Aenean vel elit scelerisque mauris pellentesque. Ullamcorper dignissim cras tincidunt lobortis feugiat vivamus. Cras fermentum odio eu feugiat pretium nibh. Congue eu consequat ac felis donec et odio pellentesque diam. Velit sed ullamcorper morbi tincidunt ornare massa eget egestas. In metus vulputate eu scelerisque felis imperdiet proin fermentum leo. Nulla malesuada pellentesque elit eget gravida cum.

Nullam ac tortor vitae purus faucibus ornare suspendisse. Libero enim sed faucibus turpis in eu mi bibendum neque. Sodales ut etiam sit amet nisl purus. Egestas diam in arcu cursus. Aliquet porttitor lacus luctus accumsan tortor. Pharetra magna ac placerat vestibulum lectus. Sit amet mauris commodo quis imperdiet massa tincidunt. In nisl nisi scelerisque eu ultrices vitae auctor. Nisi vitae suscipit tellus mauris a diam. Dui vivamus arcu felis bibendum ut tristique. Laoreet suspendisse interdum consectetur libero id.

Enim eu turpis egestas pretium. Nulla aliquet enim tortor at auctor urna. Id aliquet risus feugiat in. Non enim praesent elementum facilisis leo. Integer feugiat scelerisque varius morbi enim nunc faucibus. Egestas dui id ornare arcu odio ut sem nulla pharetra. Montes nascetur ridiculus mus mauris. Orci dapibus ultrices in iaculis. Enim sed faucibus turpis in eu mi bibendum neque. Faucibus pulvinar elementum integer enim neque volutpat ac tincidunt vitae. Et ultrices neque ornare aenean euismod elementum. Et pharetra pharetra massa massa ultricies mi quis hendrerit dolor. Tempus iaculis urna id volutpat lacus laoreet non curabitur gravida. Est velit egestas dui id ornare arcu odio. Eu facilisis sed odio morbi quis commodo odio. Lectus vestibulum mattis ullamcorper velit sed ullamcorper morbi tincidunt.

Eu non diam phasellus vestibulum lorem sed risus ultricies. Convallis aenean et tortor at risus viverra adipiscing at. Mauris pellentesque pulvinar pellentesque habitant morbi. Elementum sagittis vitae et leo duis. Massa enim nec dui nunc. Nisl tincidunt eget nullam non nisi est sit amet. Amet nisl purus in mollis nunc sed id semper. Fermentum leo vel orci porta non pulvinar neque laoreet suspendisse. Diam vel quam elementum pulvinar etiam non quam. Sagittis orci a scelerisque purus semper eget. Aliquet porttitor lacus luctus accumsan tortor. Integer vitae justo eget magna fermentum iaculis eu non diam. Egestas pretium aenean pharetra magna ac. Cursus metus aliquam eleifend mi in nulla. Cursus mattis molestie a iaculis at erat pellentesque adipiscing. Pulvinar pellentesque habitant morbi tristique senectus. Gravida cum sociis natoque penatibus et magnis dis parturient montes. In aliquam sem fringilla ut. Ut consequat semper viverra nam libero justo laoreet. Pellentesque diam volutpat commodo sed egestas.

Ornare arcu odio ut sem nulla pharetra diam. Ut enim blandit volutpat maecenas volutpat blandit aliquam. Tempus iaculis urna id volutpat lacus. Nascetur ridiculus mus mauris vitae. Venenatis cras sed felis eget velit aliquet sagittis id. Laoreet non curabitur gravida arcu ac tortor dignissim convallis aenean. Maecenas ultricies mi eget mauris pharetra et ultrices neque ornare. Egestas purus viverra accumsan in nisl nisi scelerisque eu ultrices. Tempus urna et pharetra pharetra massa massa. Pulvinar neque laoreet suspendisse interdum consectetur libero id.

Nisl rhoncus mattis rhoncus urna neque viverra justo nec ultrices. Morbi quis commodo odio aenean sed adipiscing diam donec. Neque gravida in fermentum et. Scelerisque purus semper eget duis at tellus. Volutpat blandit aliquam etiam erat velit scelerisque in dictum non. Odio ut sem nulla pharetra diam sit. Sed pulvinar proin gravida hendrerit lectus a. Diam ut venenatis tellus in metus vulputate eu scelerisque. Id semper risus in hendrerit. Vel quam elementum pulvinar etiam. Amet aliquam id diam maecenas ultricies mi. Auctor elit sed vulputate mi sit amet. Orci dapibus ultrices in iaculis nunc. Sed vulputate odio ut enim blandit volutpat maecenas volutpat. Auctor urna nunc id cursus metus. Integer enim neque volutpat ac tincidunt vitae.

Scelerisque in dictum non consectetur a erat. Vel risus commodo viverra maecenas accumsan lacus vel facilisis volutpat. Dignissim sodales ut eu sem integer vitae justo eget magna. Nunc non blandit massa enim nec dui nunc mattis enim. Sed vulputate odio ut enim blandit volutpat maecenas. Ante in nibh mauris cursus. Donec pretium vulputate sapien nec sagittis aliquam malesuada. Eu volutpat odio facilisis mauris sit amet massa. Blandit turpis cursus in hac habitasse platea dictumst quisque. Donec enim diam vulputate ut pharetra sit.

Magna fringilla urna porttitor rhoncus dolor purus non. Fames ac turpis egestas integer eget. Mattis rhoncus urna neque viverra. Laoreet sit amet cursus sit amet dictum sit amet. Vel pretium lectus quam id leo in vitae turpis massa. Euismod lacinia at quis risus sed vulputate odio ut. Lorem dolor sed viverra ipsum. Viverra justo nec ultrices dui sapien. Aliquam nulla facilisi cras fermentum odio eu feugiat pretium. Adipiscing commodo elit at imperdiet dui accumsan sit amet nulla. Morbi leo urna molestie at elementum eu facilisis sed. Habitant morbi tristique senectus et netus et malesuada. Viverra ipsum nunc aliquet bibendum enim. Integer vitae justo eget magna fermentum. Tincidunt id aliquet risus feugiat. Mauris ultrices eros in cursus turpis. Amet venenatis urna cursus eget nunc. Nisl nisi scelerisque eu ultrices vitae.

Non pulvinar neque laoreet suspendisse interdum consectetur libero. Facilisis leo vel fringilla est ullamcorper eget nulla facilisi. Ipsum dolor sit amet consectetur adipiscing elit pellentesque. Risus quis varius quam quisque id. Bibendum arcu vitae elementum curabitur vitae. Vitae et leo duis ut diam quam nulla. Orci eu lobortis elementum nibh tellus molestie nunc non blandit. Arcu odio ut sem nulla pharetra diam sit amet. Quis vel eros donec ac odio. Est lorem ipsum dolor sit amet consectetur adipiscing
