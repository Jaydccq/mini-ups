#!/usr/bin/env python3
"""
Test script to:
1. Connect Amazon to world ID 1 (same as UPS)
2. Create an order through Amazon  
3. Verify UPS receives the order and processes it
"""

import requests
import time
import json

def login_amazon():
    """Login to Amazon service"""
    session = requests.Session()
    
    # Get login page to get CSRF token
    login_page = session.get('http://localhost:8080/login')
    print("Login page status:", login_page.status_code)
    
    # Login with admin credentials
    login_data = {
        'email': 'admin@example.com',
        'password': 'admin'
    }
    
    login_response = session.post('http://localhost:8080/login', data=login_data)
    print("Login response status:", login_response.status_code)
    
    if login_response.status_code == 200 and 'admin@example.com' in login_response.text:
        print("✅ Successfully logged into Amazon")
        return session
    else:
        print("❌ Failed to login to Amazon")
        return None

def connect_amazon_to_world(session, world_id=1):
    """Connect Amazon to specified world ID"""
    
    # First get the connect world page
    connect_page = session.get('http://localhost:8080/admin/connect-world')
    print("Connect page status:", connect_page.status_code)
    
    # Connect to world 1 (same as UPS)
    connect_data = {
        'action': 'connect_existing', 
        'world_id': str(world_id),
        'sim_speed': '1000'
    }
    
    connect_response = session.post('http://localhost:8080/admin/connect-world', data=connect_data)
    print("Connect response status:", connect_response.status_code)
    
    if connect_response.status_code == 200:
        if 'Connected to world simulator' in connect_response.text:
            print(f"✅ Successfully connected Amazon to world {world_id}")
            return True
        else:
            print(f"❌ Failed to connect Amazon to world {world_id}")
            return False
    else:
        print(f"❌ Connection request failed with status {connect_response.status_code}")
        return False

def create_order(session):
    """Create an order through Amazon"""
    
    # Add item to cart (assuming product ID 1 exists)
    cart_data = {
        'product_id': '1',
        'quantity': '1'
    }
    
    cart_response = session.post('http://localhost:8080/cart/add', data=cart_data)
    print("Add to cart status:", cart_response.status_code)
    
    # Proceed to checkout
    checkout_data = {
        'destination_x': '10',
        'destination_y': '10', 
        'ups_account': 'test_user@example.com'
    }
    
    checkout_response = session.post('http://localhost:8080/checkout', data=checkout_data)
    print("Checkout status:", checkout_response.status_code)
    
    if checkout_response.status_code == 200:
        print("✅ Successfully created order")
        return True
    else:
        print("❌ Failed to create order")
        return False

def check_ups_logs():
    """Check UPS logs for order processing"""
    import subprocess
    
    # Check UPS logs for recent activity
    result = subprocess.run(['docker', 'compose', 'logs', 'ups-backend', '--tail=10'], 
                          capture_output=True, text=True)
    
    print("\n🔍 Recent UPS logs:")
    print(result.stdout)
    
    # Look for signs of successful processing
    if 'ShipmentCreated' in result.stdout and 'tracking number' in result.stdout:
        print("✅ UPS successfully processed the order")
        return True
    else:
        print("❌ Order processing not detected in UPS logs")
        return False

def main():
    print("🚀 Testing complete order flow from Amazon to UPS")
    print("=" * 50)
    
    # Step 1: Login to Amazon
    session = login_amazon()
    if not session:
        return
    
    time.sleep(2)
    
    # Step 2: Connect Amazon to world 1  
    if not connect_amazon_to_world(session, 1):
        return
    
    time.sleep(2)
    
    # Step 3: Create an order
    if not create_order(session):
        return
        
    time.sleep(5)  # Wait for processing
    
    # Step 4: Check UPS logs for processing
    check_ups_logs()
    
    print("=" * 50) 
    print("✅ Order flow test completed")

if __name__ == '__main__':
    main()