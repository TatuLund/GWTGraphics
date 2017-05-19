/*
 * Copyright 2011 Henri Kerola
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaadin.addon.gwtgraphics.client;

import java.util.LinkedHashMap;
import java.util.Map;

import org.vaadin.addon.gwtgraphics.client.fill.Fill;
import org.vaadin.addon.gwtgraphics.client.gradient.Gradient;
import org.vaadin.addon.gwtgraphics.client.impl.SVGImpl;
import org.vaadin.addon.gwtgraphics.client.stroke.Stroke;
import org.vaadin.addon.gwtgraphics.client.transform.MatrixTransform;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

/**
 * An abstract base class for drawing objects the DrawingArea can contain.
 * 
 * @author Henri Kerola
 */
public abstract class VectorObject extends Widget implements HasClickHandlers,
		HasAllMouseHandlers, HasDoubleClickHandlers {
	
	private static final SVGImpl impl = GWT.create(SVGImpl.class);
	
	private static enum FillType {
		SOLID, GRADIENT
	}
	
	private Widget parent;
	
	private Map<String, String> properties;
	
	private Stroke stroke;
	private Fill fill;
	private FillType fillType;
	private Gradient fillGradient;
	private MatrixTransform transform;

	private double width, height;
	private double posX, posY;
	private double scaleX, scaleY;
	private double rotation;
	private boolean transformDirty;

	public VectorObject() {
		setElement(impl.createElement(getType()));
		properties = new LinkedHashMap<String, String>();
		
		stroke = new Stroke("black");
		fill = new Fill("white");
		fillType = FillType.SOLID;
		fillGradient = null;
		
		posX = 0;
		posY = 0;
		scaleX = 1;
		scaleY = 1;
		width = -1;
		height = -1;
		transformDirty = true;
	}
	
	private MatrixTransform getTransform() {
		if(transformDirty) {
			
			double rot = Math.toRadians(rotation);
	        double ca = Math.cos(rot);
			double sa = Math.sin(rot);
			double a = (scaleX * ca);
			double b = -(scaleY * sa);
			double c = (scaleX * sa);
			double d = (scaleY * ca);
			double tx = posX;
			double ty = posY;
			
			transform.set(a, b, c, d, tx, ty);
			transformDirty = false;
		}
		return transform;
	}

	protected SVGImpl getImpl() {
		return impl;
	}

	protected abstract Class<? extends VectorObject> getType();
	
	/**
	 * Get SVG name of the element that this VectorObject represents
	 *
	 * @return a string like 'line' or 'rect' or whatever
	 */
	protected abstract String getSVGElementName();
	
	public void setStroke(Stroke s) {
		if(s != null) {
			stroke = s;
		}
	}
	
	public Stroke getStroke() {
		return stroke;
	}
	
	public void setFill(Fill f) {
		fillType = FillType.SOLID;
		fill = f;
		fillGradient = null;
	}
	
	public void setFill(Gradient g) {
		fillType = FillType.GRADIENT;
		fillGradient = g;
		fill = null;
	}
	
	public FillType getFillType() {
		return fillType;
	}
	
	public Gradient getGradientFill() {
		return fillGradient;
	}
	
	public Fill getFill() {
		return fill;
	}
	
	/**
	 * Re-create the attributes of this VectorObject
	 * 
	 * TODO: make sure this is used everywhere it can be
	 */
	public void redraw() {
		Element e = getElement();
		MatrixTransform transform = getTransform();
		
		for(String property : properties.keySet()) {
			String value = properties.get(property);
			e.setAttribute(property, value);
		}
		
		if(fillType == FillType.SOLID) {
			String[] fillAttrs = fill.toSVGString().split(" ");
			for(String a : fillAttrs) {
				String[] parts = a.split("=\"");
				String name = parts[0];
				String value = parts[1].substring(0, parts[1].indexOf("\""));
				e.setAttribute(name, value);
			}
		} else if(fillType == FillType.GRADIENT) {
			e.setAttribute("fill", "url(#" + fillGradient.getId() + ")");
		} else {
			// wtf?!
			assert false : "this should not happen";
		}
		
		if(width > -1) {
			e.setAttribute("width", "" + width);
		}
		if(height > -1) {
			e.setAttribute("height", "" + height);
		}
		
		e.setAttribute("transform", transform.toSVGString());
	}

	public String getProperty(String pname) {
		return getProperty(pname, null);
	}
	
	public double getPropertyDouble(String pname) {
		return getPropertyDouble(pname, 0.0);
	}
	
	public double getPropertyDouble(String pname, double defaultValue) {
		String p = getProperty(pname, null);
		if(p != null) {
			return Double.parseDouble(p);
		}
		return defaultValue;
	}
	
	public String getProperty(String pname, String defaultValue) {
		String value = properties.get(pname);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}

	public void setProperty(String pname, String pvalue) {
		if (pvalue == null || pvalue.isEmpty()) {
			properties.remove(pname);
		} else {
			properties.put(pname, pvalue);
		}
	}
	
	public void setProperty(String pname, double pvalue) {
		setPropertyDouble(pname,pvalue);
	}
	
	public void setPropertyDouble(String pname, double pvalue) {
		setProperty(pname, "" + pvalue);
	}
	
	public void setSize(double width, double height) {
		this.width = width;
		this.height = height;
	}
	
	public double getWidth() {
		return width;
	}
	
	public double getHeight() {
		return height;
	}
	
	public void setX(double x) {
		posX = x;
	}
	
	public void setY(double y) {
		posY = y;
	}
	
	public void setPosition(double x, double y) {
		posX = x;
		posY = y;
		transformDirty = true;
	}
	
	public double getX() {
		return posX;
	}
	
	public double getY() {
		return posY;
	}
	
	public void setScale(double sx, double sy) {
		scaleX = sx;
		scaleY = sy;
		transformDirty = true;
	}
	
	public double getScaleX() {
		return scaleX;
	}
	
	public double getScaleY() {
		return scaleY;
	}
	
	public double getRotation() {
		return rotation;
	}

	public void setRotation(double degree) {
		this.rotation = degree;
		transformDirty = true;
	}

	public Widget getParent() {
		return parent;
	}

	// TODO, XXX: override failure - someone please investigate
	public void setParent(Widget parent) {
		Widget oldParent = this.parent;
		if (parent == null) {
			if (oldParent != null && oldParent.isAttached()) {
				onDetach();
				assert !isAttached() : "Failure of "
						+ this.getClass().getName()
						+ " to call super.onDetach()";
			}
			this.parent = null;
		} else {
			if (oldParent != null) {
				throw new IllegalStateException(
						"Cannot set a new parent without first clearing the old parent");
			}
			this.parent = parent;
			if (parent.isAttached()) {
				onAttach();
				assert isAttached() : "Failure of " + this.getClass().getName()
						+ " to call super.onAttach()";
			}
		}
	}

	@Override
	public void setStyleName(String style) {
		getImpl().setStyleName(getElement(), style);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.gwt.event.dom.client.HasClickHandlers#addClickHandler(com.
	 * google.gwt.event.dom.client.ClickHandler)
	 */
	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return addDomHandler(handler, ClickEvent.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.gwt.event.dom.client.HasDoubleClickHandlers#addDoubleClickHandler
	 * (com.google.gwt.event.dom.client.DoubleClickHandler)
	 */
	public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler) {
		return addDomHandler(handler, DoubleClickEvent.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.gwt.event.dom.client.HasMouseDownHandlers#addMouseDownHandler
	 * (com.google.gwt.event.dom.client.MouseDownHandler)
	 */
	public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
		return addDomHandler(handler, MouseDownEvent.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.gwt.event.dom.client.HasMouseUpHandlers#addMouseUpHandler(
	 * com.google.gwt.event.dom.client.MouseUpHandler)
	 */
	public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
		return addDomHandler(handler, MouseUpEvent.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.gwt.event.dom.client.HasMouseOutHandlers#addMouseOutHandler
	 * (com.google.gwt.event.dom.client.MouseOutHandler)
	 */
	public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
		return addDomHandler(handler, MouseOutEvent.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.gwt.event.dom.client.HasMouseOverHandlers#addMouseOverHandler
	 * (com.google.gwt.event.dom.client.MouseOverHandler)
	 */
	public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
		return addDomHandler(handler, MouseOverEvent.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.gwt.event.dom.client.HasMouseMoveHandlers#addMouseMoveHandler
	 * (com.google.gwt.event.dom.client.MouseMoveHandler)
	 */
	public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
		return addDomHandler(handler, MouseMoveEvent.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.gwt.event.dom.client.HasMouseWheelHandlers#addMouseWheelHandler
	 * (com.google.gwt.event.dom.client.MouseWheelHandler)
	 */
	public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
		return addDomHandler(handler, MouseWheelEvent.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.gwt.user.client.ui.Widget#onAttach()
	 */
	@Override
	protected void onAttach() {
		super.onAttach();
		getImpl().onAttach(getElement(), isAttached());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.gwt.user.client.ui.Widget#onDetach()
	 */
	@Override
	protected void onDetach() {
		super.onDetach();
	}
	
}