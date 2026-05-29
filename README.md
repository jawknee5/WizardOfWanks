# WizardOfWanks Enterprise - Project Root

📁 **WizardOfWanks/** - Complete application root directory

## 📂 Contents

### `/enterprise/` - Main Application
- **server.js** - Express backend (12KB)
- **public/index.html** - Frontend UI (28KB)
- **migrations.js** - Database setup
- **seed.js** - Sample data
- **package.json** - Dependencies
- **Dockerfile** - Container config
- **.env** - Environment variables
- **node_modules/** - Dependencies (installed)

### `/receipt-processor/` - Receipt OCR System
- Complete standalone receipt scanning application
- Advanced OCR with barcode generation
- Offline transaction queue
- User-approved manual sync
- See: `/receipt-processor/README.md`

## 🚀 Quick Start

### Windows
```batch
START.bat
```

### macOS/Linux
```bash
chmod +x START.sh
./START.sh
```

## 🌐 Access

- **Application**: http://localhost:5000
- **API Health**: http://localhost:5000/api/health
- **Demo Login**: admin@wizardofwanks.com / password123

## 📊 System Status

✅ WizardOfWanks Enterprise running on port 5000
✅ PostgreSQL database connected
✅ Ready for use

## 📖 Documentation

- **README.md** - Project overview (if in this directory)
- **enterprise/README.md** - Enterprise app docs
- **receipt-processor/README.md** - Receipt processor docs
- **docker-compose.wiz.yml** - Service configuration

## 🛠️ Commands

```bash
# Start services
START.bat                                    # Windows
./START.sh                                   # Mac/Linux
docker-compose -f docker-compose.wiz.yml up -d

# Stop services
docker-compose -f docker-compose.wiz.yml down

# View logs
docker-compose -f docker-compose.wiz.yml logs -f

# Restart
docker-compose -f docker-compose.wiz.yml restart
```

## 📝 Project Structure

```
WizardOfWanks/
├── START.bat                 # Windows startup
├── START.sh                  # Unix startup
├── README.md                 # This file
├── enterprise/
│   ├── server.js
│   ├── public/
│   ├── package.json
│   ├── Dockerfile
│   └── .env
├── receipt-processor/
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── README.md
└── docker-compose.wiz.yml
```

## ✅ What's Running

- **WizardOfWanks Enterprise** - Receipt management system
- **PostgreSQL** - Database
- **Redis** - Cache (if configured)
- **Grafana** - Monitoring (optional)

## 🔧 Configuration

Edit `.env` in the `/enterprise/` directory to configure:
- Database connection
- API ports
- Environment settings

## 📞 Support

- See `/enterprise/README.md` for WizardOfWanks docs
- See `/receipt-processor/README.md` for Receipt Processor docs
- Check logs: `docker-compose -f docker-compose.wiz.yml logs`

---

**Version**: 2.0.0 + 3.0.0 (Receipt Processor)
**Status**: ✅ Running
**Location**: H:\WizardOfWanks\

Ready to use! 🚀
