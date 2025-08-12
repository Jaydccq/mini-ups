/**
 * TinaCMS Configuration
 * 
 * Git-based content management for Mini-UPS marketing content.
 * Provides visual editing for homepage content while maintaining
 * version control through git workflow.
 */

import { defineConfig } from 'tinacms';

export default defineConfig({
  branch: process.env.TINA_BRANCH || 'main',
  clientId: process.env.TINA_CLIENT_ID,
  token: process.env.TINA_TOKEN,

  build: {
    outputFolder: 'admin',
    publicFolder: 'public',
  },

  media: {
    tina: {
      mediaRoot: '/uploads',
      publicFolder: 'public',
    },
  },

  schema: {
    collections: [
      {
        name: 'homepage',
        label: 'Homepage Content',
        path: 'content/homepage',
        format: 'json',
        ui: {
          allowedActions: {
            create: false,
            delete: false,
          },
        },
        fields: [
          // Hero Section
          {
            type: 'object',
            name: 'hero',
            label: 'Hero Section',
            fields: [
              {
                type: 'string',
                name: 'badge_text',
                label: 'Badge Text',
                description: 'Small badge text above the main headline',
              },
              {
                type: 'string',
                name: 'headline',
                label: 'Main Headline',
                description: 'Primary hero headline',
              },
              {
                type: 'string',
                name: 'subheadline',
                label: 'Subheadline',
                description: 'Supporting text below the headline',
                ui: {
                  component: 'textarea',
                },
              },
              {
                type: 'string',
                name: 'cta_primary',
                label: 'Primary CTA Text',
                description: 'Main call-to-action button text',
              },
              {
                type: 'string',
                name: 'cta_secondary',
                label: 'Secondary CTA Text',
                description: 'Secondary button text',
              },
              {
                type: 'image',
                name: 'hero_image',
                label: 'Hero Image',
                description: 'Optional hero background image',
              },
            ],
          },

          // Features Section
          {
            type: 'object',
            name: 'features',
            label: 'Features Section',
            fields: [
              {
                type: 'string',
                name: 'section_title',
                label: 'Section Title',
              },
              {
                type: 'string',
                name: 'section_description',
                label: 'Section Description',
                ui: {
                  component: 'textarea',
                },
              },
              {
                type: 'object',
                name: 'feature_list',
                label: 'Features',
                list: true,
                ui: {
                  itemProps: (item) => ({
                    label: item?.title || 'Feature',
                  }),
                },
                fields: [
                  {
                    type: 'string',
                    name: 'title',
                    label: 'Feature Title',
                  },
                  {
                    type: 'string',
                    name: 'description',
                    label: 'Feature Description',
                    ui: {
                      component: 'textarea',
                    },
                  },
                  {
                    type: 'string',
                    name: 'icon',
                    label: 'Icon Name',
                    description: 'Lucide React icon name (e.g., "Truck", "Package")',
                  },
                ],
              },
            ],
          },

          // Testimonials Section
          {
            type: 'object',
            name: 'testimonials',
            label: 'Testimonials Section',
            fields: [
              {
                type: 'string',
                name: 'section_title',
                label: 'Section Title',
              },
              {
                type: 'string',
                name: 'section_description',
                label: 'Section Description',
                ui: {
                  component: 'textarea',
                },
              },
              {
                type: 'object',
                name: 'testimonial_list',
                label: 'Testimonials',
                list: true,
                ui: {
                  itemProps: (item) => ({
                    label: item?.name || 'Testimonial',
                  }),
                },
                fields: [
                  {
                    type: 'string',
                    name: 'name',
                    label: 'Customer Name',
                  },
                  {
                    type: 'string',
                    name: 'role',
                    label: 'Customer Role/Title',
                  },
                  {
                    type: 'string',
                    name: 'content',
                    label: 'Testimonial Content',
                    ui: {
                      component: 'textarea',
                    },
                  },
                  {
                    type: 'number',
                    name: 'rating',
                    label: 'Rating (1-5)',
                    ui: {
                      validate: (value) => {
                        if (value < 1 || value > 5) {
                          return 'Rating must be between 1 and 5';
                        }
                      },
                    },
                  },
                  {
                    type: 'image',
                    name: 'avatar',
                    label: 'Customer Avatar',
                  },
                ],
              },
            ],
          },

          // Call-to-Action Section
          {
            type: 'object',
            name: 'cta_section',
            label: 'Call-to-Action Section',
            fields: [
              {
                type: 'string',
                name: 'title',
                label: 'CTA Title',
              },
              {
                type: 'string',
                name: 'description',
                label: 'CTA Description',
                ui: {
                  component: 'textarea',
                },
              },
              {
                type: 'string',
                name: 'button_text',
                label: 'Button Text',
              },
              {
                type: 'string',
                name: 'button_link',
                label: 'Button Link',
                description: 'Internal route (e.g., "/register") or external URL',
              },
              {
                type: 'string',
                name: 'background_color',
                label: 'Background Color',
                description: 'Tailwind CSS class for background (e.g., "bg-primary")',
              },
            ],
          },

          // SEO & Meta
          {
            type: 'object',
            name: 'seo',
            label: 'SEO & Meta Information',
            fields: [
              {
                type: 'string',
                name: 'title',
                label: 'Page Title',
                description: 'Browser tab title and search engine title',
              },
              {
                type: 'string',
                name: 'description',
                label: 'Meta Description',
                description: 'Search engine description (160 characters max)',
                ui: {
                  component: 'textarea',
                },
              },
              {
                type: 'string',
                name: 'keywords',
                label: 'Meta Keywords',
                description: 'Comma-separated keywords for SEO',
                ui: {
                  component: 'textarea',
                },
              },
              {
                type: 'image',
                name: 'og_image',
                label: 'Social Share Image',
                description: 'Image for social media sharing (1200x630px recommended)',
              },
            ],
          },
        ],
      },
    ],
  },
});