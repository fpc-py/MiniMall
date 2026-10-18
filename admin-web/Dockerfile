# ============ Stage 1: 构建 ============
FROM node:20-alpine AS builder
WORKDIR /app

COPY admin-web/package*.json ./
RUN npm install

COPY admin-web/ ./
RUN npm run build

# ============ Stage 2: Nginx 托管 ============
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx-admin.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
