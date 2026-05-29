const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const helmet = require('helmet');
const path = require('path');
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');

// Environment variables
require('dotenv').config();
const PORT = process.env.PORT || 5000;
const JWT_SECRET = process.env.JWT_SECRET || 'wizard-secret-key-change-in-production';
const DB_URL = process.env.DATABASE_URL || 'postgresql://wizard:wizard@localhost:5432/wizard_db';

// Initialize Express app
const app = express();

// Security middleware
app.use(helmet());
app.use(cors({
  origin: process.env.CORS_ORIGIN || '*',
  credentials: true
}));

// Body parser
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Static files
app.use(express.static(path.join(__dirname, 'public')));

// Database connection pool
const pool = new Pool({
  connectionString: DB_URL,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

// Middleware to verify JWT
const verifyToken = (req, res, next) => {
  const token = req.headers['authorization']?.split(' ')[1];
  
  if (!token) {
    return res.status(401).json({ success: false, message: 'No token provided' });
  }
  
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.userId = decoded.id;
    req.userEmail = decoded.email;
    next();
  } catch (error) {
    return res.status(401).json({ success: false, message: 'Invalid token' });
  }
};

// ==================== HEALTH & SYSTEM ====================

app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.get('/api/health', (req, res) => {
  res.json({
    status: 'healthy',
    service: 'WizardOfWanks Enterprise',
    version: '2.0.0',
    timestamp: new Date().toISOString(),
    database: 'connected'
  });
});

// ==================== AUTHENTICATION ====================

app.post('/api/auth/register', async (req, res) => {
  try {
    const { email, password, name } = req.body;
    
    if (!email || !password || !name) {
      return res.status(400).json({ success: false, message: 'Missing required fields' });
    }
    
    const hashedPassword = await bcrypt.hash(password, 10);
    const id = uuidv4();
    
    const result = await pool.query(
      'INSERT INTO users (id, email, password, name, created_at) VALUES ($1, $2, $3, $4, NOW()) RETURNING id, email, name',
      [id, email, hashedPassword, name]
    );
    
    res.status(201).json({
      success: true,
      message: 'User registered successfully',
      user: result.rows[0]
    });
  } catch (error) {
    if (error.code === '23505') { // Unique constraint
      return res.status(400).json({ success: false, message: 'Email already registered' });
    }
    res.status(500).json({ success: false, message: 'Registration failed', error: error.message });
  }
});

app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    
    if (!email || !password) {
      return res.status(400).json({ success: false, message: 'Email and password required' });
    }
    
    const result = await pool.query('SELECT * FROM users WHERE email = $1', [email]);
    
    if (result.rows.length === 0) {
      return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }
    
    const user = result.rows[0];
    const passwordMatch = await bcrypt.compare(password, user.password);
    
    if (!passwordMatch) {
      return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }
    
    const token = jwt.sign(
      { id: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: '24h' }
    );
    
    res.json({
      success: true,
      message: 'Login successful',
      token,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        role: user.role
      }
    });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Login failed', error: error.message });
  }
});

app.post('/api/auth/logout', (req, res) => {
  res.json({ success: true, message: 'Logged out successfully' });
});

// ==================== USER MANAGEMENT ====================

app.get('/api/users/me', verifyToken, async (req, res) => {
  try {
    const result = await pool.query('SELECT id, email, name, role, created_at FROM users WHERE id = $1', [req.userId]);
    
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'User not found' });
    }
    
    res.json({ success: true, user: result.rows[0] });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to fetch user', error: error.message });
  }
});

app.put('/api/users/me', verifyToken, async (req, res) => {
  try {
    const { name } = req.body;
    
    const result = await pool.query(
      'UPDATE users SET name = $1, updated_at = NOW() WHERE id = $2 RETURNING id, email, name',
      [name, req.userId]
    );
    
    res.json({ success: true, user: result.rows[0] });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Update failed', error: error.message });
  }
});

// ==================== TEMPLATES ====================

app.get('/api/templates', verifyToken, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT id, name, category, description, created_by, created_at FROM templates WHERE is_deleted = false ORDER BY created_at DESC LIMIT 100'
    );
    
    res.json({ success: true, templates: result.rows });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to fetch templates', error: error.message });
  }
});

app.get('/api/templates/:id', verifyToken, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM templates WHERE id = $1 AND is_deleted = false',
      [req.params.id]
    );
    
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Template not found' });
    }
    
    res.json({ success: true, template: result.rows[0] });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to fetch template', error: error.message });
  }
});

app.post('/api/templates', verifyToken, async (req, res) => {
  try {
    const { name, category, description, content } = req.body;
    
    if (!name || !category) {
      return res.status(400).json({ success: false, message: 'Name and category required' });
    }
    
    const id = uuidv4();
    
    const result = await pool.query(
      'INSERT INTO templates (id, name, category, description, content, created_by, created_at) VALUES ($1, $2, $3, $4, $5, $6, NOW()) RETURNING *',
      [id, name, category, description, content, req.userId]
    );
    
    res.status(201).json({ success: true, template: result.rows[0] });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to create template', error: error.message });
  }
});

app.put('/api/templates/:id', verifyToken, async (req, res) => {
  try {
    const { name, category, description, content } = req.body;
    
    // Verify ownership
    const template = await pool.query('SELECT created_by FROM templates WHERE id = $1', [req.params.id]);
    if (template.rows.length === 0 || template.rows[0].created_by !== req.userId) {
      return res.status(403).json({ success: false, message: 'Unauthorized' });
    }
    
    const result = await pool.query(
      'UPDATE templates SET name = $1, category = $2, description = $3, content = $4, updated_at = NOW() WHERE id = $5 RETURNING *',
      [name, category, description, content, req.params.id]
    );
    
    res.json({ success: true, template: result.rows[0] });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to update template', error: error.message });
  }
});

app.delete('/api/templates/:id', verifyToken, async (req, res) => {
  try {
    const template = await pool.query('SELECT created_by FROM templates WHERE id = $1', [req.params.id]);
    if (template.rows.length === 0 || template.rows[0].created_by !== req.userId) {
      return res.status(403).json({ success: false, message: 'Unauthorized' });
    }
    
    await pool.query(
      'UPDATE templates SET is_deleted = true, updated_at = NOW() WHERE id = $1',
      [req.params.id]
    );
    
    res.json({ success: true, message: 'Template deleted' });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to delete template', error: error.message });
  }
});

// ==================== TEMPLATE CATEGORIES ====================

app.get('/api/categories', async (req, res) => {
  try {
    const categories = [
      { id: 1, name: 'Business', description: 'Business documents and forms' },
      { id: 2, name: 'Finance', description: 'Financial templates and reports' },
      { id: 3, name: 'HR', description: 'Human resources templates' },
      { id: 4, name: 'Legal', description: 'Legal document templates' },
      { id: 5, name: 'Marketing', description: 'Marketing and promotional templates' }
    ];
    
    res.json({ success: true, categories });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to fetch categories', error: error.message });
  }
});

// ==================== ADMIN ENDPOINTS ====================

app.get('/api/admin/users', verifyToken, async (req, res) => {
  try {
    const result = await pool.query('SELECT id, email, name, role, created_at FROM users ORDER BY created_at DESC LIMIT 1000');
    res.json({ success: true, users: result.rows, total: result.rows.length });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to fetch users', error: error.message });
  }
});

app.get('/api/admin/stats', verifyToken, async (req, res) => {
  try {
    const usersCount = await pool.query('SELECT COUNT(*) as count FROM users');
    const templatesCount = await pool.query('SELECT COUNT(*) as count FROM templates WHERE is_deleted = false');
    const categoriesCount = await pool.query('SELECT DISTINCT category FROM templates WHERE is_deleted = false');
    
    res.json({
      success: true,
      stats: {
        totalUsers: parseInt(usersCount.rows[0].count),
        totalTemplates: parseInt(templatesCount.rows[0].count),
        totalCategories: categoriesCount.rows.length,
        timestamp: new Date().toISOString()
      }
    });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to fetch stats', error: error.message });
  }
});

// ==================== SERVE FRONTEND ====================

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// ==================== ERROR HANDLING ====================

app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ success: false, message: 'Internal server error', error: err.message });
});

// ==================== START SERVER ====================

const startServer = async () => {
  try {
    // Test database connection
    const result = await pool.query('SELECT NOW()');
    console.log('✓ Database connected:', result.rows[0].now);
    
    app.listen(PORT, () => {
      console.log(`🚀 WizardOfWanks Enterprise running on http://localhost:${PORT}`);
      console.log(`📊 Admin Dashboard: http://localhost:${PORT}/admin`);
      console.log(`📚 API Docs: http://localhost:${PORT}/api/docs`);
    });
  } catch (error) {
    console.error('Failed to start server:', error.message);
    process.exit(1);
  }
};

startServer();

module.exports = app;
