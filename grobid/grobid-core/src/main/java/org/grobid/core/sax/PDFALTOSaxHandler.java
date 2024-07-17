package org.grobid.core.sax;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.analyzers.Analyzer;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.document.Document;
import org.grobid.core.layout.*;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * SAX parser for XML ALTO representation of fixed layout documents. Typographical and layout information 
 * are defined at token level, with line and block structures. 
 *
 * This parser supports:
 * - pdfalto output from PDF
 *
 * Expected coverage includes ALTO files generated by OCR (e.g. ABBYY) and ALTO files generated
 * from docx files. 
 *
 * TBD: character-level ALTO files
 * 
 */
public class PDFALTOSaxHandler extends DefaultHandler {
	public static final Logger LOGGER = LoggerFactory.getLogger(PDFALTOSaxHandler.class);

    private StringBuilder accumulator = new StringBuilder(); // Accumulate parsed text

	private String previousToken = null;
	private LayoutToken previousTok = null;
	private double currentX = 0.0;
	private double currentY = 0.0;
	private double currentWidth = 0.0;
	private double currentHeight = 0.0;
	private Block block = null; // current block
	private int nbTokens = 0; // nb tokens in the current block
	private List<GraphicObject> images = null;
    private HashMap<String, TextStyle> textStyles = new HashMap<String, TextStyle>();
    private boolean currentRotation = false;

	private List<LayoutToken> tokenizations = null;

	private Document doc = null;

    //starting page count from 1 since most of the PDF-related software count pages from 1
	private int currentPage = 0;
	private Page page = null; // the current page object
	private Analyzer analyzer = GrobidAnalyzer.getInstance(); // use the default one by default ;)

	private int currentOffset = 0;

	public PDFALTOSaxHandler(Document d, List<GraphicObject> im) {
		doc = d;
		images = im;
		tokenizations = new ArrayList<>();
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public Analyzer getAnalyzer() {
		return this.analyzer;
	}

	private void addToken(LayoutToken layoutToken) {
		layoutToken.setOffset(currentOffset);
		currentOffset += layoutToken.getText().length();
		tokenizations.add(layoutToken);
		if (doc.getBlocks() == null) {
			layoutToken.setBlockPtr(0);
		} else {
			layoutToken.setBlockPtr(doc.getBlocks().size());
		}
		if (block == null) {
            LOGGER.info("addToken called with null block object: " + layoutToken.toString());
        } else {
            block.addToken(layoutToken);
        }
	}

	private void addBlock(Block block) {
		if (block == null)
			LOGGER.info("addBlock called with null block object");

		if (!block.isNull() && (block.getStartToken() != block.getEndToken())) {
			block.setPage(page);
			doc.addBlock(block);
			page.addBlock(block);
		}
	}

	public List<LayoutToken> getTokenization() {
		return tokenizations;
	}

	public void characters(char[] ch, int start, int length) {
		accumulator.append(ch, start, length);
	}

	public String trimAndNormaliseText(String content) {
		String res = content.trim();
		//res = res.replace("\u00A0", " "); // stdandard NO-BREAK SPACE are viewed
											// as space
		//res = res.replaceAll("\\p{javaSpaceChar}", " "); // replace all unicode space separators
		 												 // by a usual SPACE
		//res = res.replace("\t"," "); // case where tabulation are used as separator
									 // -> replace tabulation with a usual space

		res = UnicodeUtil.normaliseText(res);
		return res.trim();
	}

	public void endElement(String uri, String localName,
			String qName) throws SAXException {

		if (qName.equals("TextLine")) {
			LayoutToken token = new LayoutToken();
			token.setText("\n");
			token.setPage(currentPage);
			nbTokens++;
			accumulator.setLength(0);
			addToken(token);
		} else if (qName.equals("Description")) {
			accumulator.setLength(0);
		} else if (qName.equals("String")) {

            accumulator.setLength(0);
		} else if (qName.equals("Page")) {
			// page marker are useful to detect headers (same first line(s)
			// appearing on each page)
			if (block != null) {
				LayoutToken localTok = new LayoutToken("\n");
				localTok.setPage(currentPage);
				addToken(localTok);
				addBlock(block);
			}
			nbTokens = 0;
			doc.addPage(page);
		} else if (qName.equals("TextBlock")) {
			LayoutToken localTok = new LayoutToken("\n");
			localTok.setPage(currentPage);
			addToken(localTok);

			//PL
			//block.setWidth(currentX - block.getX() + currentWidth);
			//block.setHeight(currentY - block.getY() + currentHeight);

			addBlock(block);
			nbTokens = 0;
			block = null;
		} else if (qName.equals("Illustration")) {
			// this is normally the bitmap images and vector graphics
			// such vector graphics are applied to the whole page, so there is no x,y coordinates available
			// in the xml - to get them we will need to parse the .vec files
			if (block != null) {
				addBlock(block);
			}
			//block = new Block();
			//block.setStartToken(tokenizations.size());
			int imagePos = images.size()-1;
			/*if (doc.getBlocks() != null)
				images.get(imagePos).setBlockNumber(doc.getBlocks().size());
			else
				images.get(imagePos).setBlockNumber(0);*/
			/*int startPos = 0;
			if (tokenizations.size() > 0) {
				startPos = tokenizations.size();
				//startPos = tokenizations.size()-1;
			}
			int endPos = startPos;*/
			images.get(imagePos).setStartPosition(tokenizations.size());
			images.get(imagePos).setEndPosition(tokenizations.size());
			images.get(imagePos).setPage(currentPage);
			//addBlock(block);
			nbTokens = 0;
		}
	}

	public void endDocument(){
		doc.setImages(images);
	}

	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (qName.equals("Page")) {
			int length = atts.getLength();
			currentPage++;
			page = new Page(currentPage);

			// Process each attribute
			for (int i = 0; i < length; i++) {
				// Get names and values for each attribute
				String name = atts.getQName(i);
				String value = atts.getValue(i);
				if ((name != null) && (value != null)) {
					if (name.equals("WIDTH")) {
						double width = 0.0;
						try {
							width = Double.parseDouble(value);
						} catch(NumberFormatException e) {
	                        LOGGER.warn("Invalid WIDTH value: " + value);
	                    }
						page.setWidth(width);
					} else if (name.equals("HEIGHT")) {
						double height = 0.0;
						try {
                            height = Double.parseDouble(value);
                        } catch(NumberFormatException e) {
                        	LOGGER.warn("Invalid HEIGHT value: " + value);
                        }
						page.setHeight(height);
					}
				}
			}
		} else if (qName.equals("PrintSpace")) {
            int length = atts.getLength();
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) && (value != null)) {
                    switch (name) {
                        case "HPOS":
                        	double x = 0.0;
                        	try {
	                            x = Double.parseDouble(value);
	                        } catch(NumberFormatException e) {
	                        	LOGGER.warn("Invalid HPOS value: " + value);
	                        }
                            if (x != currentX && x != 0.0) {
                                currentX = Math.abs(x);
                            }
                            break;
                        case "VPOS":
                            double y = 0.0;
                            try {
	                            y = Double.parseDouble(value);
	                        } catch(NumberFormatException e) {
	                        	LOGGER.warn("Invalid VPOS value: " + value);
	                        }
                            if (y != currentY && y != 0.0) {
                                currentY = Math.abs(y);
                            }
                            break;
                        case "WIDTH":
                            double width = 0.0;
                            try {
	                            width = Double.parseDouble(value);
	                        } catch(NumberFormatException e) {
	                        	LOGGER.warn("Invalid WIDTH value: " + value);
	                        }
                            if (width != currentWidth && width != 0.0) {
                                currentWidth = Math.abs(width);
                            }
                            break;
                        case "HEIGHT":
                            double height = 0.0;
                            try {
	                            height = Double.parseDouble(value);
	                        } catch(NumberFormatException e) {
	                        	LOGGER.warn("Invalid HEIGHT value: " + value);
	                        }
                            if (height != currentHeight && height != 0.0) {
                                currentHeight = Math.abs(height);
                            }
                            break;
                    }
                }
            }
        } else if (qName.equals("TextBlock")) {
			block = new Block();
			nbTokens = 0;
			block.setStartToken(tokenizations.size());
		} else if (qName.equals("Illustration")) {
			int length = atts.getLength();
			GraphicObject image = new GraphicObject();
            double x = 0, y = 0, width = 0, height = 0;
			// Process each attribute
			for (int i = 0; i < length; i++) {
				// Get names and values for each attribute
				String name = atts.getQName(i);
				String value = atts.getValue(i);

				if ((name != null) && (value != null)) {
					switch (name) {
						case "FILEID":
							image.setFilePath(value);
							if (value.contains(".svg")) {
								image.setType(GraphicObjectType.VECTOR);
							} else {
								image.setType(GraphicObjectType.BITMAP);
							}
							break;
						case "HPOS":
							try {
	                            x = Double.parseDouble(value);
	                        } catch(NumberFormatException e) {
	                        	LOGGER.warn("Invalid HPOS value: " + value);
	                        }
							break;
                        case "VPOS":
                        	try {
	                            y = Double.parseDouble(value);
	                        } catch(NumberFormatException e) {
	                        	LOGGER.warn("Invalid VPOS value: " + value);
	                        }
                            break;
                        case "WIDTH":
                        	try {
	                            width = Double.parseDouble(value);
	                        } catch(NumberFormatException e) {
	                        	LOGGER.warn("Invalid WIDTH value: " + value);
	                        }
                            break;
                        case "HEIGHT":
                        	try {
	                            height = Double.parseDouble(value);
	                        } catch(NumberFormatException e) {
	                        	LOGGER.warn("Invalid HEIGHT value: " + value);
	                        }
                            break;
                        case "TYPE":
                        	if (value.equals("svg")) {
								image.setType(GraphicObjectType.VECTOR);
							} else {
								image.setType(GraphicObjectType.BITMAP);
							}
							break;
					}
				}
			}
			image.setBoundingBox(BoundingBox.fromPointAndDimensions(currentPage, x, y, width, height));
			image.setPage(currentPage);
			images.add(image);
		} else if (qName.equals("TextLine")) {
			int length = atts.getLength();

			// Process each attribute
			/*for (int i = 0; i < length; i++) {
				// Get names and values for each attribute
				String name = atts.getQName(i);
				String value = atts.getValue(i);

				if ((name != null) && (value != null)) {
					if (name.equals("id")) {
					} else if (name.equals("x")) {
					} else if (name.equals("y")) {
					} else if (name.equals("width")) {
					} else if (name.equals("height")) {
					}
				}
			}*/
		} else if (qName.equals("String")) {
			int length = atts.getLength();
			String content = null, fontId = null;
            TextStyle textStyle = null;

			// Process each attribute
			for (int i = 0; i < length; i++) {
				// Get names and values for each attribute
				String name = atts.getQName(i);
				String value = atts.getValue(i);

				if ((name != null) && (value != null)) {
					if (name.equals("ID")) {
						;
					} else if (name.equals("CONTENT")) {
                        content = value;
                    } else if (name.equals("STYLEREFS")) {
                        fontId = value;
                    }else if (name.equals("rotation")) {
						if (value.equals("0"))
							currentRotation = false;
						else
							currentRotation = true;
					} else if (name.equals("HPOS")) {
						double x = 0.0;
						try {
                            x = Double.parseDouble(value);
                        } catch(NumberFormatException e) {
                        	LOGGER.warn("Invalid HPOS value: " + value);
                        }
						if (x != currentX && x != 0.0) {
							currentX = Math.abs(x);
						}
					} else if (name.equals("VPOS")) {
						double y = 0.0;
						try {
                            y = Double.parseDouble(value);
                        } catch(NumberFormatException e) {
                        	LOGGER.warn("Invalid VPOS value: " + value);
                        }
						if (y != currentY && y != 0.0) {
							currentY = Math.abs(y);
						}
					} else if (name.equals("base")) {
						double base = 0.0;
						try {
							base = Double.parseDouble(value);
						} catch(NumberFormatException e) {
                        	LOGGER.warn("Invalid base value: " + value);
                        }
					} else if (name.equals("WIDTH")) {
						double width = 0.0;
						try {
                            width = Double.parseDouble(value);
                        } catch(NumberFormatException e) {
                        	LOGGER.warn("Invalid WIDTH value: " + value);
                        }
						if (width != currentWidth && width != 0.0) {
							currentWidth = Math.abs(width);
						}
					} else if (name.equals("HEIGHT")) {
						double height = 0.0;
						try {
                            height = Double.parseDouble(value);
                        } catch(NumberFormatException e) {
                        	LOGGER.warn("Invalid HEIGHT value: " + value);
                        }
						if (height != currentHeight && height != 0.0) {
							currentHeight = Math.abs(height);
						}
					}
				}
			}

            // process ligatures
            String tok0 = TextUtilities.clean(trimAndNormaliseText(content));
            textStyle = textStyles.get(fontId);

            if (block.getStartToken() == -1) {
                block.setStartToken(tokenizations.size());
            }

            if (tok0.length() > 0) {
                //StringTokenizer st = new StringTokenizer(tok0,
                //		TextUtilities.delimiters, true);
                List<String> subTokenizations = new ArrayList<>();
                try {
                    // TBD: pass a language object to the tokenize method call
                    subTokenizations = analyzer.tokenize(tok0);
                }
                catch(Exception e) {
                    LOGGER.debug("Sub-tokenization of pdfalto token has failed.");
                }

                if (subTokenizations.size() != 0) {
                    //{
                    // WARNING: ROUGH APPROXIMATION (but better than the same coords)

                    double totalLength = 0;
                    for (String t : subTokenizations) {
                        totalLength += t.length();
                    }
                    double prevSubWidth = 0;

                    for(String tok : subTokenizations) {

                        // WARNING: ROUGH APPROXIMATION (but better than the same coords)
                        // Here to get the right subTokWidth should use the content length.
                        double subTokWidth = (currentWidth * (tok.length() / totalLength));

                        double subTokX = currentX + prevSubWidth;
                        prevSubWidth += subTokWidth;

                        //String tok = st.nextToken();
                        if (tok.length() > 0) {

                            LayoutToken token = new LayoutToken();
                            token.setPage(currentPage);
                            token.setText(tok);
                            addToken(token);

                            if (currentRotation) {
                                // if the text is rotated, it appears that the font size is multiplied
                                // by 2? we should have a look at pdf2xml/pdfalto for this
                                textStyle.setFontSize(textStyle.getFontSize() / 2);
                            }

                            if (textStyle.getFontName() != null)
                                token.setFont(textStyle.getFontName().toLowerCase());
                            else
                                token.setFont("default");
                            token.setItalic(textStyle.isItalic());
                            token.setBold(textStyle.isBold());
                            token.setRotation(currentRotation);
                            token.setPage(currentPage);
                            token.setColorFont(textStyle.getFontColor());

                            token.setSubscript(textStyle.isSubscript());
                            token.setSuperscript(textStyle.isSuperscript());

                            token.setX(subTokX);
                            token.setY(currentY);
                            token.setWidth(subTokWidth);
                            token.setHeight(currentHeight);

//							token.setX(currentX);
//							token.setY(currentY);
//							token.setWidth(currentWidth);
//							token.setHeight(currentHeight);

                            token.setFontSize(textStyle.getFontSize());

//							if (!diaresis && !accent) {
//
//								block.addToken(token);
//							}

                            if (block.getFont() == null) {
                                if (textStyle.getFontName() != null)
                                    block.setFont(textStyle.getFontName().toLowerCase());
                                else
                                    token.setFont("default");
                            }
                            if (nbTokens == 0) {
                                block.setItalic(textStyle.isItalic());
                                block.setBold(textStyle.isBold());
                            }
                            if (block.getColorFont() == null)
                                block.setColorFont(textStyle.getFontColor());

							/*if (block.getX() == 0.0)
								block.setX(currentX);
							if (block.getY() == 0.0)
								block.setY(currentY);
							if (block.getWidth() == 0.0)
								block.setWidth(currentWidth);
							if (block.getHeight() == 0.0)
								block.setHeight(currentHeight);
							if (block.getFontSize() == 0.0)
								block.setFontSize(currentFontSize);*/

                                previousToken = tok;
                                previousTok = token;

                            nbTokens++;
                        }
                    }
                }
                if (tokenizations.size() > 0) {
                    String justBefore = tokenizations.get(tokenizations.size() - 1).t();
                    if (!justBefore.endsWith("-")) {
                        LayoutToken localTok = new LayoutToken(" ");
                        localTok.setPage(currentPage);
                        addToken(localTok);
                    }
                }
            }
            block.setEndToken(tokenizations.size());
		} else if (qName.equals("TextStyle")) {
            int length = atts.getLength();

            TextStyle textStyle = new TextStyle();
            String fontId = null;
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (isNotBlank(name) && isNotBlank(value)) {
                    if (name.equals("ID")) {
                        fontId = value;
                    } else if (name.equals("FONTFAMILY")) {
                        /*if (StringUtils.containsIgnoreCase(value, "bold") || StringUtils.endsWithIgnoreCase(value, "_bd")) {
                            textStyle.setBold(true);
                        }

                        if (StringUtils.containsIgnoreCase(value, "italic") || StringUtils.endsWithIgnoreCase(value, "_it")) {
                            textStyle.setItalic(true);
                        }*/

                        textStyle.setFontName(value);
                    } else if (name.equals("FONTSIZE")) {
                        double fontSize = 0.0;
                    	try {
                            fontSize = Double.parseDouble(value);
                        } catch(NumberFormatException e) {
                        	LOGGER.warn("Invalid FONTSIZE value: " + value);
                        }
                        textStyle.setFontSize(fontSize);
                    } else if (name.equals("FONTSTYLE")) {
                        // font properties, we are interested by subscript or superscript
                        if (StringUtils.containsIgnoreCase(value, "subscript")) {
                            textStyle.setSubscript(true);
                        }

                        if (StringUtils.containsIgnoreCase(value, "superscript")) {
                            textStyle.setSuperscript(true);
                        }

                        if (StringUtils.containsIgnoreCase(value, "bold")) {
                            textStyle.setBold(true);
                        }

                        if (StringUtils.containsIgnoreCase(value, "italic") || StringUtils.containsIgnoreCase(value, "italics")) {
                            textStyle.setItalic(true);
                        }

                    } else if (name.equals("FONTCOLOR")) {
                        textStyle.setFontColor(value);
                    }
                    else if (name.equals("FONTTYPE")) {
                    	// value can be empty or a sequence of font properties separated by space, out of these
                        /*if (value.equals("serif")) {
                            textStyle.setSerif(true);
                        } else {
                            textStyle.setSerif(false);
                        }*/
                    } 
//                    else if (name.equals("FONTWIDTH")) {
//                        if (value.equals("proportional")) {
//                            textStyle.setProportional(true);
//                        } else {
//                            textStyle.setProportional(false);
//                        }
//                    }
//
//                    else if (name.equals("rotation")) {
//                        if (value.equals("0"))
//                            textStyle.setRotation(false);
//                        else
//                            textStyle.setRotation(true);
//                    }
                }
            }
            if(fontId != null)
                textStyles.put(fontId, textStyle);
        }
	}

}

class TextStyle {

    private double fontSize = 0.0;
    private String fontName = null;
    private String fontColor = null;

    private boolean bold = false;
    private boolean italic = false;

    private boolean subscript = false;
    private boolean superscript = false;

    //not used attributes
    private boolean proportional = false;
    private boolean serif = false;

    //private boolean rotation = false;

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

//    public boolean isRotation() {
//        return rotation;
//    }
//
//    public void setRotation(boolean rotation) {
//        this.rotation = rotation;
//    }

    public boolean isProportional() {
        return proportional;
    }

    public void setProportional(boolean proportional) {
        this.proportional = proportional;
    }

    public boolean isSerif() {
        return serif;
    }

    public void setSerif(boolean serif) {
        this.serif = serif;
    }

    public boolean isSubscript() {
        return subscript;
    }

    public void setSubscript(boolean script) {
        this.subscript = script;
    }

    public boolean isSuperscript() {
        return superscript;
    }

    public void setSuperscript(boolean script) {
        this.superscript = script;
    }
}