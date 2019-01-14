/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Based on crawler4j project by Yasser Ganjisaffar
 */
package com.nanocrawler.contentparser;

import com.nanocrawler.data.Content;
import com.nanocrawler.data.HtmlContent;
import com.nanocrawler.data.Page;
import com.nanocrawler.util.CrawlConfig;
import com.nanocrawler.urlmanipulation.URLCanonicalizer;
import com.nanocrawler.urlmanipulation.WebURL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import nu.validator.htmlparser.common.DoctypeExpectation;
import nu.validator.htmlparser.common.Heuristics;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.dom.HtmlDocumentBuilder;
import org.apache.commons.io.IOUtils;
import org.mozilla.universalchardet.UniversalDetector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// Parses HTML content (default content parser) using Validator.nu HTML parser
public class HtmlContentParser implements ContentParser {
       
    private final String BODY_ELEMENT = "body";
    private final String TITLE_ELEMENT = "title";
    private final String BASE_ELEMENT = "base";
    private final String LINK_ELEMENT = "link";
    private final String A_ELEMENT = "a";
    private final String IFRAME_ELEMENT = "iframe";
    private final String FRAME_ELEMENT = "frame";
    private final String EMBED_ELEMENT = "embed";
    private final String META_ELEMENT = "meta";
    
    private final String HREF_ATTRIB = "href";
    private final String SRC_ATTRIB = "src";
    
    private CrawlConfig config = null;
    
    // Constructor
    public HtmlContentParser(CrawlConfig config) {
        this.config = config;               
    }
    
    @Override
    // This is default content parser if no other content parser handles the parsing
    public boolean canParseContent(String mimeType) {
        return true;
    }

    // Parses the web page using Validator.nu parser
    private Document parseHtml(Page page, HtmlContent c) throws SAXException, IOException {
        String html;
        
        // Handle char type conversions based on the byte stream, not based on what the server says
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(page.getContentData(), 0, page.getContentData().length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
               
        if (encoding != null) {
            page.setContentCharset(encoding);
            try {
                html = new String(page.getContentData(), encoding);
            } catch (Exception ex) {
                html = new String(page.getContentData());
            }
        } else {
            html = new String(page.getContentData());
        }
        
        html = html.trim();
        c.setHtml(html);
                
        HtmlDocumentBuilder builder = new HtmlDocumentBuilder();
        builder.setCommentPolicy(XmlViolationPolicy.ALTER_INFOSET);
        builder.setContentNonXmlCharPolicy(XmlViolationPolicy.ALTER_INFOSET);
        builder.setContentSpacePolicy(XmlViolationPolicy.ALTER_INFOSET);        
        builder.setNamePolicy(XmlViolationPolicy.ALTER_INFOSET);
        builder.setStreamabilityViolationPolicy(XmlViolationPolicy.ALTER_INFOSET);
        builder.setXmlnsPolicy(XmlViolationPolicy.ALTER_INFOSET);
        builder.setMappingLangToXmlLang(true);
        builder.setHtml4ModeCompatibleWithXhtml1Schemata(true);
        builder.setHeuristics(Heuristics.ALL);
        builder.setCheckingNormalization(false);
        builder.setDoctypeExpectation(DoctypeExpectation.NO_DOCTYPE_ERRORS);
        builder.setIgnoringComments(true);
        builder.setScriptingEnabled(true);
        builder.setXmlPolicy(XmlViolationPolicy.ALTER_INFOSET);
        
        Document doc = builder.parse(IOUtils.toInputStream(html));        
        if (doc.getElementsByTagName(BODY_ELEMENT).getLength() == 0) {
            throw new RuntimeException("Problem parsing document - invalid HTML, no body element found");
        }        
        
        return doc;
    }
    
    // Returns <base> item if there is one
    private String getBaseUrl(Document doc) {
        String baseUrl = null;
        try {
            if (doc.getElementsByTagName(BASE_ELEMENT).getLength() > 0 && doc.getElementsByTagName(BASE_ELEMENT).item(0).hasAttributes()) {
                baseUrl = doc.getElementsByTagName(BASE_ELEMENT).item(0).getAttributes().getNamedItem(HREF_ATTRIB).getNodeValue().trim();
            }
        } catch (Exception ex) {
            baseUrl = null;
        }
        return baseUrl;
    }
    
    // Parses links of all element - attribute combos (e.g. <a> & "href") 
    private void getLinks(List<ExtractedUrlAnchorPair> outgoingUrls, Document doc, String elementName, String attribName, boolean getAnchorText) {
        if (doc.getElementsByTagName(elementName).getLength() > 0) {
            NodeList nl = doc.getElementsByTagName(elementName);
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.hasAttributes() && n.getAttributes().getNamedItem(attribName) != null) {
                    ExtractedUrlAnchorPair newUrl = new ExtractedUrlAnchorPair();
                    newUrl.setHref(n.getAttributes().getNamedItem(attribName).getNodeValue().trim());
                    if (getAnchorText) {
                        newUrl.setAnchor(n.getTextContent().trim());
                    } else {
                        newUrl.setAnchor("");
                    }
                    
                    if (newUrl.getHref().trim().length() > 0) {
                        outgoingUrls.add(newUrl);
                    }
                }
            }
        }
    }
    
    // Parses various outgoing URLs 
    public List<ExtractedUrlAnchorPair> getOutgoingUrls(Document doc) {
        List<ExtractedUrlAnchorPair> outgoingUrls = new ArrayList<>();
        
        getLinks(outgoingUrls, doc, LINK_ELEMENT, HREF_ATTRIB, false);
        getLinks(outgoingUrls, doc, A_ELEMENT, HREF_ATTRIB, true);
        getLinks(outgoingUrls, doc, IFRAME_ELEMENT, SRC_ATTRIB, false);
        getLinks(outgoingUrls, doc, FRAME_ELEMENT, SRC_ATTRIB, false);
        getLinks(outgoingUrls, doc, EMBED_ELEMENT, SRC_ATTRIB, false);
        
        NodeList metaNodes = doc.getElementsByTagName(META_ELEMENT);
        if (metaNodes != null) {
            for (int i = 0; i < metaNodes.getLength(); i++) {
                Node n = metaNodes.item(i);
                if (n.hasAttributes()) {
                    String equiv = n.getAttributes().getNamedItem("http-equiv") != null ? n.getAttributes().getNamedItem("http-equiv").getNodeValue().trim() : null;
                    String content = n.getAttributes().getNamedItem("content") != null ? n.getAttributes().getNamedItem("content").getNodeValue().trim() : null;
                    if (equiv != null && content != null) {
                        equiv = equiv.toLowerCase();
                        
                        // http-equiv="refresh" content="0;URL=http://foo.bar/..."
                        if (equiv.equalsIgnoreCase("refresh")) {
                            String metaRefresh = "";
                            int pos = content.toLowerCase().indexOf("url=");
                            if (pos != -1) {
                                metaRefresh = content.substring(pos + 4);
                            }
                            
                            if (metaRefresh.length() > 0) {
                                ExtractedUrlAnchorPair newUrl = new ExtractedUrlAnchorPair();
                                newUrl.setHref(metaRefresh);
                                newUrl.setAnchor("");
                                outgoingUrls.add(newUrl);
                            }
                        }            
                        
                        // http-equiv="location" content="http://foo.bar/..."
                        if (equiv.equalsIgnoreCase("location")) {
                            if (content.length() > 0) {
                                ExtractedUrlAnchorPair newUrl = new ExtractedUrlAnchorPair();
                                newUrl.setHref(content);
                                outgoingUrls.add(newUrl);
                            }
                        }
                    }
                }                                
            }
        }
                
        return outgoingUrls;
    }
        
    @Override
    // Runs the whole parsing, extracts links from the page 
    public Content parseContent(Page page, String contextUrl) {
        HtmlContent c = new HtmlContent();
        try {
            Document doc = parseHtml(page, c);  
            
            c.setText(doc.getElementsByTagName(BODY_ELEMENT).item(0).getTextContent().trim());
            if (doc.getElementsByTagName(TITLE_ELEMENT).getLength() > 0) {
                c.setTitle(doc.getElementsByTagName(TITLE_ELEMENT).item(0).getTextContent().trim());
            }         
            
            String baseUrl = getBaseUrl(doc);
            if (baseUrl != null) {
                contextUrl = baseUrl;
            }

            List<ExtractedUrlAnchorPair> extractedUrls = getOutgoingUrls(doc);
            List<WebURL> outgoingUrls = new ArrayList<>();
            int urlCount = 0;
            for (ExtractedUrlAnchorPair urlAnchorPair : extractedUrls) {
                String href = urlAnchorPair.getHref();
                href = href.trim();
                if (href.length() == 0) {
                    continue;
                }
            
                String hrefWithoutProtocol = href.toLowerCase();
                if (href.startsWith("http://")) {
                    hrefWithoutProtocol = href.substring(7);
                }

                if (!hrefWithoutProtocol.contains("javascript:") && !hrefWithoutProtocol.contains("mailto:") && !hrefWithoutProtocol.contains("@")) {
                    String url = URLCanonicalizer.getCanonicalURL(href, contextUrl);
                    if (url != null) {
                        WebURL webURL = new WebURL();
                        webURL.setURL(url);
                        webURL.setAnchor(urlAnchorPair.getAnchor());
                        outgoingUrls.add(webURL);
                        urlCount++;
                        if (urlCount > config.getMaxOutgoingLinksToFollow()) {
                            break;
                        }
                    }
                }
            }
             
            c.setOutgoingUrls(outgoingUrls);        
        } catch (Exception ex) {
            ex.printStackTrace();
        }
                
        return c;
    }    
}
