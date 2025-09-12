#!/usr/bin/env node

/**
 * MCP Server simple test script
 * Verifies basic MCP Server functionality
 */

import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';

const colors = {
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  reset: '\x1b[0m'
};

function log(level, message) {
  const timestamp = new Date().toISOString();
  const color = colors[level] || colors.reset;
  console.log(`${color}[${timestamp}] ${level.toUpperCase()}: ${message}${colors.reset}`);
}

async function testMcpServer() {
  log('blue', '=== MCP Server Functional Test ===');

  // Check environment
  log('blue', 'Checking environment...');
  
  if (!fs.existsSync('.env')) {
    log('red', '❌ .env file does not exist');
    return false;
  }
  
  if (!fs.existsSync('dist/index.js')) {
    log('yellow', 'Building project...');
    try {
      await runCommand('npm', ['run', 'build']);
      log('green', '✅ Build completed');
    } catch (error) {
      log('red', `❌ Build failed: ${error.message}`);
      return false;
    }
  }

  // Start MCP Server
  log('blue', 'Starting MCP Server...');
  
  const server = spawn('node', ['dist/index.js'], {
    stdio: ['pipe', 'pipe', 'pipe']
  });

  let serverOutput = '';
  let serverReady = false;

  server.stdout.on('data', (data) => {
    serverOutput += data.toString();
    if (data.toString().includes('NLQ MCP Server started successfully')) {
      serverReady = true;
    }
  });

  server.stderr.on('data', (data) => {
    log('yellow', `Server stderr: ${data}`);
  });

  // Wait for server startup
  await new Promise(resolve => setTimeout(resolve, 2000));

  if (!serverReady) {
    log('yellow', '⚠️ Server may not be fully started; continuing tests...');
  }

  // Test 1: List Tools
  log('blue', 'Test 1: List available tools');
  try {
    const listToolsRequest = {
      jsonrpc: '2.0',
      id: 1,
      method: 'tools/list'
    };

    server.stdin.write(JSON.stringify(listToolsRequest) + '\n');
    
    await new Promise(resolve => setTimeout(resolve, 1000));
    log('green', '✅ Tool list request sent');
  } catch (error) {
    log('red', `❌ Tool list test failed: ${error.message}`);
  }

  // Test 2: Health Check
  log('blue', 'Test 2: Health check');
  try {
    const healthCheckRequest = {
      jsonrpc: '2.0',
      id: 2,
      method: 'tools/call',
      params: {
        name: 'health_check',
        arguments: {
          includeDetails: true
        }
      }
    };

    server.stdin.write(JSON.stringify(healthCheckRequest) + '\n');
    
    await new Promise(resolve => setTimeout(resolve, 1000));
    log('green', '✅ Health check request sent');
  } catch (error) {
    log('red', `❌ Health check test failed: ${error.message}`);
  }

  // Test 3: NLQ Query (simulated)
  log('blue', 'Test 3: Natural language query');
  try {
    const nlqRequest = {
      jsonrpc: '2.0',
      id: 3,
      method: 'tools/call',
      params: {
        name: 'nlq_query',
        arguments: {
          query: 'test query',
          userId: 'test-user-123'
        }
      }
    };

    server.stdin.write(JSON.stringify(nlqRequest) + '\n');
    
    await new Promise(resolve => setTimeout(resolve, 2000));
    log('green', '✅ NLQ request sent');
  } catch (error) {
    log('red', `❌ NLQ test failed: ${error.message}`);
  }

  // Wait for responses
  await new Promise(resolve => setTimeout(resolve, 3000));

  // Stop server
  server.kill('SIGTERM');
  await new Promise(resolve => setTimeout(resolve, 1000));

  log('blue', '=== Tests complete ===');
  
  // Print last few lines of server output
  const logLines = serverOutput.split('\n').slice(-10).filter(line => line.trim());
  if (logLines.length > 0) {
    log('blue', 'Server output (last 10 lines):');
    logLines.forEach(line => {
      console.log(`  ${line}`);
    });
  }

  log('green', '✅ Test script finished');
  log('yellow', '💡 Review the output above to confirm the MCP Server works correctly');
  
  return true;
}

function runCommand(command, args) {
  return new Promise((resolve, reject) => {
    const proc = spawn(command, args, { stdio: 'inherit' });
    proc.on('close', (code) => {
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`Command failed with exit code ${code}`));
      }
    });
    proc.on('error', reject);
  });
}

// Run test
testMcpServer().catch(error => {
  log('red', `❌ Tests failed: ${error.message}`);
  process.exit(1);
});
