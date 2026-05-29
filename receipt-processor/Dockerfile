# Receipt Processor Enterprise - Docker Configuration

FROM node:18-alpine

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci --only=production

# Copy source
COPY public/ ./public/
COPY src/ ./src/
COPY build/ ./build/

# Build app
RUN npm run build

# Expose port
EXPOSE 3000 5000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:3000 || exit 1

# Start app
CMD ["npm", "start"]
