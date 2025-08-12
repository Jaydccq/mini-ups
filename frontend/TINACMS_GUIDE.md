# TinaCMS Integration Guide

This project now includes TinaCMS for content management, allowing non-technical team members to edit homepage content through a visual interface.

## Quick Start

### Development Mode
```bash
npm run tina:dev
```
This starts both the development server and TinaCMS admin interface.

### Access the CMS
1. Start the development server: `npm run tina:dev`
2. Open http://localhost:3000 in your browser
3. Navigate to http://localhost:3000/admin to access the CMS interface

## What You Can Edit

The TinaCMS interface allows you to edit:

### Hero Section
- Badge text above the headline
- Main headline
- Subheadline description
- Button text for primary and secondary CTAs

### Features Section
- Section title and description
- Individual features (title, description, icon)
- Add, remove, or reorder features

### Testimonials
- Section title and description
- Customer testimonials (name, role, content, rating)
- Add, remove, or reorder testimonials

### Call-to-Action Section
- CTA title and description
- Button text and link
- Background color (Tailwind CSS classes)

### SEO & Meta
- Page title and meta description
- Keywords and social sharing image

## Content Storage

- **Development**: Content is stored in `/content/homepage/homepage.json`
- **Production**: Content would be managed through TinaCMS Cloud (requires setup)

## Git Workflow

1. Content editors make changes through the TinaCMS interface
2. Changes are committed to a new branch automatically
3. Create a pull request for review
4. Merge to deploy changes

## Fallback System

If TinaCMS content fails to load, the application automatically uses hardcoded fallback content, ensuring the site never breaks.

## Technical Implementation

- **Hook**: `useTinaContent()` provides type-safe access to CMS content
- **Types**: Full TypeScript support with content validation
- **Performance**: Content is cached and optimized for fast loading
- **SEO**: Dynamic meta tags and structured data support

## Production Setup

For production deployment with TinaCMS Cloud:

1. Create a TinaCMS account at https://tina.io
2. Set up environment variables:
   ```
   TINA_CLIENT_ID=your_client_id
   TINA_TOKEN=your_token
   TINA_BRANCH=main
   ```
3. Deploy with `npm run build && npm run tina:build`

## Benefits

- ✅ **Non-technical editing**: Marketing team can update content without code changes
- ✅ **Version control**: All changes are tracked in Git
- ✅ **Preview**: See changes before publishing
- ✅ **Type safety**: TypeScript ensures content structure integrity
- ✅ **Performance**: Optimized loading with fallback support
- ✅ **SEO friendly**: Dynamic meta tags and structured data