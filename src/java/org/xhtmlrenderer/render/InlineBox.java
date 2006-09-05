/*
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package org.xhtmlrenderer.render;

import org.w3c.dom.Element;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.extend.ContentFunction;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.layout.Styleable;
import org.xhtmlrenderer.layout.WhitespaceStripper2;

public class InlineBox implements Styleable
{
    private Element _element;
    
    private String _text;
    private boolean _removableWhitespace;
    private boolean _startsHere;
    private boolean _endsHere;
    
    private CalculatedStyle _style;
    
    private ContentFunction _contentFunction;
    
    private boolean _minMaxCalculated;
    private int _maxWidth;
    private int _minWidth;
    
    private int _firstLineWidth;
    
    
    public InlineBox(String text) {
        _text = text;
    }
    
    public String getText() {
        return _text;
    }
    
    public void setText(String text) {
        _text = text;
    }

    public boolean isRemovableWhitespace() {
        return _removableWhitespace;
    }

    public void setRemovableWhitespace(boolean removeableWhitespace) {
        _removableWhitespace = removeableWhitespace;
    }

    public boolean isEndsHere() {
        return _endsHere;
    }

    public void setEndsHere(boolean endsHere) {
        _endsHere = endsHere;
    }

    public boolean isStartsHere() {
        return _startsHere;
    }

    public void setStartsHere(boolean startsHere) {
        _startsHere = startsHere;
    }

    public CalculatedStyle getStyle() {
        return _style;
    }

    public void setStyle(CalculatedStyle style) {
        _style = style;
    }

    public Element getElement() {
        return _element;
    }

    public void setElement(Element element) {
        _element = element;
    }

    public ContentFunction getContentFunction() {
        return _contentFunction;
    }

    public void setContentFunction(ContentFunction contentFunction) {
        _contentFunction = contentFunction;
    }

    public boolean isDynamicFunction() {
        return _contentFunction != null;
    }
    
    private int getTextWidth(LayoutContext c, String s) {
        return c.getTextRenderer().getWidth(
                c.getFontContext(), 
                c.getFont(getStyle().getFont(c)), 
                s);
    }
    
    private void calcMaxWidthFromLineLength(LayoutContext c, boolean trim) {
        int last = 0;
        int current = 0;
        
        while ( (current = _text.indexOf(WhitespaceStripper2.EOL, last)) != -1) {
            String target = _text.substring(last, current);
            if (trim) {
                target = target.trim();
            }
            int length = getTextWidth(c, target);
            if (length > _maxWidth) {
                _maxWidth = length;
            }
            if (_firstLineWidth == 0 && length > 0) {
                _firstLineWidth = length;
            }
            last = current + 1;
        }
        
        String target = _text.substring(last);
        if (trim) {
            target = target.trim();
        }
        int length = getTextWidth(c, target);
        if (length > _maxWidth) {
            _maxWidth = length;
        }
        if (last == 0) {
            _firstLineWidth = length;
        }        
    }
    
    public int getSpaceWidth(LayoutContext c) {
        return c.getTextRenderer().getWidth(
                c.getFontContext(), 
                getStyle().getFSFont(c), 
                WhitespaceStripper2.SPACE);
        
    }
    
    public int getTrailingSpaceWidth(LayoutContext c) {
        if (_text.length() > 0 && _text.charAt(_text.length()-1) == ' ') {
            return getSpaceWidth(c);
        } else {
            return 0;
        }
    }
    
    private int calcMinWidthFromWordLength(
            LayoutContext c, boolean trimLeadingSpace, boolean includeWS) {
        int spaceWidth = getSpaceWidth(c);
        
        int last = 0;
        int current = 0;
        int maxWidth = 0;
        int spaceCount = 0;
        
        String text = getText(trimLeadingSpace);
        
        while ( (current = text.indexOf(WhitespaceStripper2.SPACE, last)) != -1) {
            int length = getTextWidth(c, text.substring(last, current));
            if (spaceCount > 0) {
                if (includeWS) {
                    for (int i = 0; i < spaceCount; i++) {
                        length += spaceWidth;
                    }
                }
                spaceCount = 0;
            }
            if (length > _minWidth) {
                _minWidth = length;
            }
            maxWidth += length;
            maxWidth += spaceWidth;
            
            last = current;
            for (int i = current; i < text.length(); i++) {
                if (text.charAt(i) == ' ') {
                    spaceCount++;
                    last++;
                } else {
                    break;
                }
            }
        }
        
        int length = getTextWidth(c, text.substring(last));
        if (spaceCount > 0) {
            if (includeWS) {
                for (int i = 0; i < spaceCount; i++) {
                    length += spaceWidth;
                }
            }
            spaceCount = 0;
        }        
        if (length > _minWidth) {
            _minWidth = length;
        }
        maxWidth += length;
        
        return maxWidth;
    }
    
    private String getText(boolean trimLeadingSpace) {
        if (! trimLeadingSpace) {
            return getText();
        } else {
            if (_text.length() > 0 && _text.charAt(0) == ' ') {
                return _text.substring(1);
            } else {
                return _text;
            }
        }
    }
    
    public void calcMinMaxWidth(LayoutContext c, boolean trimLeadingSpace) {
        if (! _minMaxCalculated) {
            IdentValue whitespace = getStyle().getWhitespace();
            if (whitespace == IdentValue.NOWRAP) {
                _minWidth = _maxWidth = getTextWidth(c, getText(trimLeadingSpace));
            } else if (whitespace == IdentValue.PRE) {
                calcMaxWidthFromLineLength(c, false);
                _minWidth = _maxWidth;
            } else if (whitespace == IdentValue.PRE_WRAP) {
                calcMinWidthFromWordLength(c, false, true);
                calcMaxWidthFromLineLength(c, false);
            } else if (whitespace == IdentValue.PRE_LINE) {
                calcMinWidthFromWordLength(c, trimLeadingSpace, false);
                calcMaxWidthFromLineLength(c, true);
            } else /* if (whitespace == IdentValue.NORMAL) */ {
                _maxWidth = calcMinWidthFromWordLength(c, trimLeadingSpace, false);
            }
            
            _minMaxCalculated = true;
        }
    }
    
    public int getMaxWidth() {
        return _maxWidth;
    }
    
    public int getMinWidth() {
        return _minWidth;
    }
    
    public int getFirstLineWidth() {
        return _firstLineWidth;
    }
}
