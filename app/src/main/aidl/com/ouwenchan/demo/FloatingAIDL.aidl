// FloatingAIDL.aidl
package com.ouwenchan.demo;

// Declare any non-default types here with import statements

interface FloatingAIDL {
     	int setDragIcon();
       	void setFloatingVisibility(boolean value);
       	int getFloatingVisibility();
       	float setSize();
       	boolean getAnimation();
       	void touched();
       	void doubleTouched();
       	void longTouched();
}
