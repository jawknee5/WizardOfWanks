const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
require('dotenv').config();

const pool = new Pool({
  connectionString: process.env.DATABASE_URL || 'postgresql://wizard:[REDACTED]@localhost:5432/wizard_db'
});

const seedDatabase = async () => {
  const client = await pool.connect();
  try {
    console.log('Seeding database...');
    
    // Create test users
    const userId1 = uuidv4();
    const userId2 = uuidv4();
    
    const hashedPassword = await bcrypt.hash('password123', 10);
    
    await client.query('DELETE FROM templates');
    await client.query('DELETE FROM users');
    
    // Insert users
    await client.query(
      'INSERT INTO users (id, email, password, name, role) VALUES ($1, $2, $3, $4, $5)',
      [userId1, 'admin@wizardofwanks.com', hashedPassword, 'Admin User', 'admin']
    );
    
    await client.query(
      'INSERT INTO users (id, email, password, name, role) VALUES ($1, $2, $3, $4, $5)',
      [userId2, 'user@wizardofwanks.com', hashedPassword, 'Regular User', 'user']
    );
    
    // Insert sample templates
    const templates = [
      {
        id: uuidv4(),
        name: 'Business Letter Template',
        category: 'Business',
        description: 'Professional business letter format',
        content: '[Sender Address]\n[Date]\n[Recipient Address]\n\nDear [Name],\n\n[Body]\n\nSincerely,\n[Your Name]'
      },
      {
        id: uuidv4(),
        name: 'Invoice Template',
        category: 'Finance',
        description: 'Standard invoice with itemization',
        content: 'INVOICE\nInvoice #: [NUMBER]\nDate: [DATE]\nDue Date: [DUE DATE]\n\n[Items]\nTotal: [AMOUNT]'
      },
      {
        id: uuidv4(),
        name: 'Project Proposal',
        category: 'Business',
        description: 'Comprehensive project proposal template',
        content: 'PROJECT PROPOSAL\nTitle: [PROJECT NAME]\nScope: [SCOPE]\nTimeline: [TIMELINE]\nBudget: [BUDGET]'
      },
      {
        id: uuidv4(),
        name: 'Employment Contract',
        category: 'Legal',
        description: 'Employment agreement template',
        content: 'EMPLOYMENT AGREEMENT\nPosition: [POSITION]\nSalary: [SALARY]\nTerms: [TERMS]'
      },
      {
        id: uuidv4(),
        name: 'HR Onboarding Checklist',
        category: 'HR',
        description: 'Employee onboarding checklist',
        content: '- Day 1: Welcome & Orientation\n- Week 1: Training\n- Week 2: Department Integration'
      },
      {
        id: uuidv4(),
        name: 'Marketing Campaign Brief',
        category: 'Marketing',
        description: 'Marketing campaign planning template',
        content: 'CAMPAIGN BRIEF\nObjective: [OBJECTIVE]\nTarget Audience: [AUDIENCE]\nChannels: [CHANNELS]\nBudget: [BUDGET]'
      },
      {
        id: uuidv4(),
        name: 'Meeting Minutes',
        category: 'Business',
        description: 'Meeting notes and action items',
        content: 'MEETING MINUTES\nDate: [DATE]\nAttendees: [NAMES]\nTopics: [TOPICS]\nAction Items: [ITEMS]'
      },
      {
        id: uuidv4(),
        name: 'Financial Report',
        category: 'Finance',
        description: 'Quarterly financial report',
        content: 'FINANCIAL REPORT - Q[QUARTER]\nRevenue: [AMOUNT]\nExpenses: [AMOUNT]\nProfit: [AMOUNT]'
      }
    ];
    
    for (const template of templates) {
      await client.query(
        'INSERT INTO templates (id, name, category, description, content, created_by) VALUES ($1, $2, $3, $4, $5, $6)',
        [template.id, template.name, template.category, template.description, template.content, userId1]
      );
    }
    
    console.log('✓ Database seeded with 8 templates and 2 users');
  } catch (error) {
    console.error('Error seeding database:', error.message);
  } finally {
    client.release();
  }
};

seedDatabase().then(() => {
  pool.end();
  console.log('Seeding complete');
}).catch(error => {
  console.error('Seeding failed:', error);
  process.exit(1);
});
