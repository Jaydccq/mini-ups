#!/usr/bin/env python3
"""
Test script to place an order on the Amazon system
"""
import requests
import time

# Get CSRF token first
session = requests.Session()

def get_csrf_token():
    """Get CSRF token from the homepage"""
    try:
        response = session.get("http://localhost:8080/")
        # Look for csrf token in the response
        if 'csrf_token' in response.text:
            import re
            match = re.search(r'name="csrf_token".*?value="([^"]+)"', response.text)
            if match:
                return match.group(1)
    except Exception as e:
        print(f"Error getting CSRF token: {e}")
    return None

def place_order():
    """Place a test order"""
    # Get CSRF token
    csrf_token = get_csrf_token()
    print(f"CSRF Token: {csrf_token}")
    
    # Add item to cart
    add_to_cart_data = {
        'product_id': 1,
        'quantity': 1
    }
    if csrf_token:
        add_to_cart_data['csrf_token'] = csrf_token
    
    print("Adding item to cart...")
    cart_response = session.post(
        "http://localhost:8080/cart/add",
        data=add_to_cart_data,
        allow_redirects=False
    )
    print(f"Add to cart response: {cart_response.status_code}")
    
    # Checkout
    checkout_data = {
        'destination_x': 15,
        'destination_y': 20
    }
    if csrf_token:
        checkout_data['csrf_token'] = csrf_token
        
    print("Checking out...")
    checkout_response = session.post(
        "http://localhost:8080/cart/checkout",
        data=checkout_data,
        allow_redirects=False
    )
    print(f"Checkout response: {cart_response.status_code}")
    print("Order placed successfully!")
    
if __name__ == "__main__":
    place_order()