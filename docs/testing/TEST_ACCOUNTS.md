# Mini-UPS Test Accounts

This file contains the credentials for test accounts and authentication information.

## 🔐 **WORKING ADMIN ACCOUNT** (Authentication Fixed)

#### System Administrator ✅
- **Username:** `admin`
- **Email:** `admin@miniups.com`
- **Password:** `admin123`
- **Role:** `ADMIN`
- **Status:** ✅ **VERIFIED WORKING** - JWT role claims fixed
- **Access:** Full admin dashboard, system management
- **Description:** Primary system administrator account

#### Test Administrator
- **Username:** `testadmin`
- **Email:** `testadmin@miniups.com`
- **Password:** `adminpass`
- **Role:** `ADMIN`
- **Description:** Test admin account for development

### Operator User

#### UPS Operator
- **Username:** `operator`
- **Email:** `operator@miniups.com`
- **Password:** `operator123`
- **Role:** `OPERATOR`
- **Description:** UPS operations staff account

### Standard User

#### Test User
- **Username:** `testuser`
- **Email:** `testuser@miniups.com`
- **Password:** `testpassword`
- **Role:** `USER`
- **Description:** Standard user account for testing

## 🌐 Access URLs

### Docker Environment (Recommended)
- **Frontend:** http://localhost:3000
- **Admin Dashboard:** http://localhost:3000/admin
- **Backend API:** http://localhost:8081/api
- **Login Endpoint:** http://localhost:8081/api/auth/login

### 🔧 Authentication Status
- ✅ **JWT Token Generation:** Fixed - includes role claims
- ✅ **Spring Security Authorization:** Working with `@PreAuthorize("hasRole('ADMIN')")`
- ✅ **Admin Dashboard API:** Fully functional
- ✅ **Docker Deployment:** Authenticated and tested
- ✅ **Native Development:** Also working (port 8081)

### 📝 JWT Token Sample
Login response includes properly formatted JWT:
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiJ9...",
    "user": {
      "username": "admin", 
      "role": "ADMIN",
      "admin": true
    }
  }
}
```

## Usage Notes

- **Primary Account:** Use `admin` / `admin123` for admin access
- All accounts are automatically created during application startup if they don't exist  
- Passwords are BCrypt encrypted in the database
- These accounts are created by the `DataInitializer` component
- **Security:** Change default passwords in production environments

## Amazon Service (External)

### Default Admin
- **Email:** `admin@example.com`
- **Password:** `admin`
- **Note:** This is configured in the external Amazon service, not in our UPS system

## 🎯 Quick Test
```bash
# Test login API directly
curl -H "Content-Type: application/json" \
     -d '{"usernameOrEmail":"admin","password":"admin123"}' \
     http://localhost:8081/api/auth/login

# Test admin dashboard  
curl -H "Authorization: Bearer <JWT_TOKEN>" \
     http://localhost:8081/api/admin/dashboard/statistics
```

**Last Updated:** 2025-08-08 - Authentication issues completely resolved! 🎉