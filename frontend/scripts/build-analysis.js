#!/usr/bin/env node

/**
 * Build Analysis Script
 * 
 * Analyzes the production build and provides optimization recommendations.
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Colors for console output
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m'
};

const log = {
  info: (msg) => console.log(`${colors.blue}ℹ${colors.reset} ${msg}`),
  success: (msg) => console.log(`${colors.green}✓${colors.reset} ${msg}`),
  warning: (msg) => console.log(`${colors.yellow}⚠${colors.reset} ${msg}`),
  error: (msg) => console.log(`${colors.red}✗${colors.reset} ${msg}`),
  title: (msg) => console.log(`\n${colors.bright}${colors.cyan}${msg}${colors.reset}`),
};

// Configuration
const DIST_DIR = path.join(process.cwd(), 'dist');
const PACKAGE_JSON = path.join(process.cwd(), 'package.json');
const SIZE_LIMITS = {
  js: 500 * 1024,      // 500KB for JS chunks
  css: 100 * 1024,     // 100KB for CSS
  images: 1024 * 1024, // 1MB for images
  total: 2 * 1024 * 1024, // 2MB total
};

/**
 * Get file size in bytes
 */
function getFileSize(filePath) {
  try {
    return fs.statSync(filePath).size;
  } catch (error) {
    return 0;
  }
}

/**
 * Format file size
 */
function formatSize(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * Get all files in directory recursively
 */
function getAllFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir);
  
  files.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    
    if (stat.isDirectory()) {
      getAllFiles(filePath, fileList);
    } else {
      fileList.push(filePath);
    }
  });
  
  return fileList;
}

/**
 * Analyze bundle size
 */
function analyzeBundleSize() {
  log.title('📦 Bundle Size Analysis');
  
  if (!fs.existsSync(DIST_DIR)) {
    log.error('Dist directory not found. Run build first.');
    process.exit(1);
  }
  
  const files = getAllFiles(DIST_DIR);
  const analysis = {
    js: [],
    css: [],
    images: [],
    fonts: [],
    other: [],
    total: 0
  };
  
  files.forEach(file => {
    const relativePath = path.relative(DIST_DIR, file);
    const size = getFileSize(file);
    const ext = path.extname(file).toLowerCase();
    
    analysis.total += size;
    
    const fileInfo = { path: relativePath, size, formattedSize: formatSize(size) };
    
    if (['.js', '.mjs'].includes(ext)) {
      analysis.js.push(fileInfo);
    } else if (['.css'].includes(ext)) {
      analysis.css.push(fileInfo);
    } else if (['.png', '.jpg', '.jpeg', '.gif', '.svg', '.webp', '.ico'].includes(ext)) {
      analysis.images.push(fileInfo);
    } else if (['.woff', '.woff2', '.ttf', '.eot', '.otf'].includes(ext)) {
      analysis.fonts.push(fileInfo);
    } else {
      analysis.other.push(fileInfo);
    }
  });
  
  // Sort by size (largest first)
  Object.keys(analysis).forEach(key => {
    if (Array.isArray(analysis[key])) {
      analysis[key].sort((a, b) => b.size - a.size);
    }
  });
  
  return analysis;
}

/**
 * Display size analysis
 */
function displaySizeAnalysis(analysis) {
  log.info(`Total bundle size: ${formatSize(analysis.total)}`);
  
  // Check total size limit
  if (analysis.total > SIZE_LIMITS.total) {
    log.warning(`Bundle size exceeds recommended limit of ${formatSize(SIZE_LIMITS.total)}`);
  } else {
    log.success(`Bundle size is within recommended limits`);
  }
  
  console.log('\n📊 File Type Breakdown:');
  
  // JavaScript files
  if (analysis.js.length > 0) {
    const totalJsSize = analysis.js.reduce((sum, file) => sum + file.size, 0);
    console.log(`\n  JavaScript: ${formatSize(totalJsSize)}`);
    
    analysis.js.forEach(file => {
      const status = file.size > SIZE_LIMITS.js ? '🔴' : '🟢';
      console.log(`    ${status} ${file.path} - ${file.formattedSize}`);
    });
    
    if (totalJsSize > SIZE_LIMITS.js * 2) {
      log.warning('Consider code splitting for large JavaScript bundles');
    }
  }
  
  // CSS files
  if (analysis.css.length > 0) {
    const totalCssSize = analysis.css.reduce((sum, file) => sum + file.size, 0);
    console.log(`\n  CSS: ${formatSize(totalCssSize)}`);
    
    analysis.css.forEach(file => {
      const status = file.size > SIZE_LIMITS.css ? '🔴' : '🟢';
      console.log(`    ${status} ${file.path} - ${file.formattedSize}`);
    });
  }
  
  // Images
  if (analysis.images.length > 0) {
    const totalImageSize = analysis.images.reduce((sum, file) => sum + file.size, 0);
    console.log(`\n  Images: ${formatSize(totalImageSize)}`);
    
    analysis.images.forEach(file => {
      const status = file.size > SIZE_LIMITS.images ? '🔴' : '🟢';
      console.log(`    ${status} ${file.path} - ${file.formattedSize}`);
    });
    
    const largeImages = analysis.images.filter(img => img.size > SIZE_LIMITS.images);
    if (largeImages.length > 0) {
      log.warning('Consider optimizing large images with WebP or compression');
    }
  }
  
  // Other files
  if (analysis.fonts.length > 0) {
    const totalFontSize = analysis.fonts.reduce((sum, file) => sum + file.size, 0);
    console.log(`\n  Fonts: ${formatSize(totalFontSize)}`);
  }
  
  if (analysis.other.length > 0) {
    const totalOtherSize = analysis.other.reduce((sum, file) => sum + file.size, 0);
    console.log(`\n  Other: ${formatSize(totalOtherSize)}`);
  }
}

/**
 * Provide optimization recommendations
 */
function provideRecommendations(analysis) {
  log.title('💡 Optimization Recommendations');
  
  const recommendations = [];
  
  // Large JavaScript bundles
  const largeJsFiles = analysis.js.filter(file => file.size > SIZE_LIMITS.js);
  if (largeJsFiles.length > 0) {
    recommendations.push({
      priority: 'high',
      message: `${largeJsFiles.length} JavaScript file(s) exceed size limits`,
      actions: [
        'Implement code splitting with React.lazy()',
        'Use dynamic imports for heavy dependencies',
        'Consider removing unused dependencies',
        'Enable tree shaking optimization'
      ]
    });
  }
  
  // Large images
  const largeImages = analysis.images.filter(img => img.size > SIZE_LIMITS.images);
  if (largeImages.length > 0) {
    recommendations.push({
      priority: 'medium',
      message: `${largeImages.length} image(s) are larger than recommended`,
      actions: [
        'Convert to WebP format',
        'Implement responsive images',
        'Use image compression tools',
        'Consider lazy loading for images'
      ]
    });
  }
  
  // Total bundle size
  if (analysis.total > SIZE_LIMITS.total) {
    recommendations.push({
      priority: 'high',
      message: 'Total bundle size exceeds recommended limits',
      actions: [
        'Implement Progressive Web App (PWA) caching',
        'Use CDN for static assets',
        'Enable Brotli/Gzip compression',
        'Analyze and remove unused code'
      ]
    });
  }
  
  // Display recommendations
  if (recommendations.length === 0) {
    log.success('No optimization recommendations. Bundle is well optimized!');
    return;
  }
  
  recommendations.forEach((rec, index) => {
    const priority = rec.priority === 'high' ? '🔴 HIGH' : '🟡 MEDIUM';
    console.log(`\n${index + 1}. ${priority}: ${rec.message}`);
    rec.actions.forEach(action => {
      console.log(`   • ${action}`);
    });
  });
}

/**
 * Generate build report
 */
function generateBuildReport(analysis) {
  const report = {
    timestamp: new Date().toISOString(),
    total_size: analysis.total,
    total_size_formatted: formatSize(analysis.total),
    files: {
      javascript: {
        count: analysis.js.length,
        total_size: analysis.js.reduce((sum, file) => sum + file.size, 0),
        files: analysis.js
      },
      css: {
        count: analysis.css.length,
        total_size: analysis.css.reduce((sum, file) => sum + file.size, 0),
        files: analysis.css
      },
      images: {
        count: analysis.images.length,
        total_size: analysis.images.reduce((sum, file) => sum + file.size, 0),
        files: analysis.images
      }
    },
    size_limits: SIZE_LIMITS,
    within_limits: analysis.total <= SIZE_LIMITS.total
  };
  
  const reportPath = path.join(DIST_DIR, 'build-report.json');
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  log.success(`Build report saved to ${reportPath}`);
}

/**
 * Main analysis function
 */
function main() {
  console.log(`${colors.bright}${colors.magenta}🔍 Mini-UPS Frontend Build Analysis${colors.reset}\n`);
  
  try {
    const analysis = analyzeBundleSize();
    displaySizeAnalysis(analysis);
    provideRecommendations(analysis);
    generateBuildReport(analysis);
    
    log.title('🎯 Next Steps');
    console.log('1. Review the recommendations above');
    console.log('2. Check dist/stats.html for detailed bundle analysis');
    console.log('3. Run lighthouse audit for performance metrics');
    console.log('4. Test the build with `npm run preview`');
    
  } catch (error) {
    log.error(`Analysis failed: ${error.message}`);
    process.exit(1);
  }
}

// Run analysis
main();