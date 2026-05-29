const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
  connectionString: process.env.DATABASE_URL || 'postgresql://wizard:[REDACTED]@localhost:5432/wizard_db'
});

const createTables = async () => {
  const client = await pool.connect();
  try {
    console.log('Creating database tables...');
    
    // Users table
    await client.query(`
      CREATE TABLE IF NOT EXISTS users (
        id UUID PRIMARY KEY,
        email VARCHAR(255) UNIQUE NOT NULL,
        password VARCHAR(255) NOT NULL,
        name VARCHAR(255) NOT NULL,
        role VARCHAR(50) DEFAULT 'user' CHECK (role IN ('user', 'admin')),
        is_active BOOLEAN DEFAULT true,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        last_login TIMESTAMP
      );
      CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
    `);
    
    // Templates table
    await client.query(`
      CREATE TABLE IF NOT EXISTS templates (
        id UUID PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        category VARCHAR(100) NOT NULL,
        description TEXT,
        content TEXT,
        created_by UUID NOT NULL REFERENCES users(id),
        is_deleted BOOLEAN DEFAULT false,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        view_count INTEGER DEFAULT 0,
        FOREIGN KEY (created_by) REFERENCES users(id)
      );
      CREATE INDEX IF NOT EXISTS idx_templates_category ON templates(category);
      CREATE INDEX IF NOT EXISTS idx_templates_created_by ON templates(created_by);
      CREATE INDEX IF NOT EXISTS idx_templates_is_deleted ON templates(is_deleted);
    `);
    
    // Audit log table
    await client.query(`
      CREATE TABLE IF NOT EXISTS audit_logs (
        id UUID PRIMARY KEY,
        user_id UUID REFERENCES users(id),
        action VARCHAR(100) NOT NULL,
        resource_type VARCHAR(100) NOT NULL,
        resource_id UUID,
        details JSONB,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
      CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_logs(user_id);
      CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);
    `);
    
    console.log('✓ Tables created successfully');
  } catch (error) {
    console.error('Error creating tables:', error.message);
  } finally {
    client.release();
  }
};

createTables().then(() => {
  pool.end();
  console.log('Migration complete');
}).catch(error => {
  console.error('Migration failed:', error);
  process.exit(1);
});
