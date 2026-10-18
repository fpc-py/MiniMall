# ============ Stage 1: 构建 ============
FROM node:20-alpine AS builder
# 跳过 playwright 浏览器下载（仅为前端用不到的本地测试依赖，避免构建镜像膨胀）
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
WORKDIR /app

COPY mobile-h5/package*.json ./
RUN npm install

COPY mobile-h5/ ./
RUN npm run build

# ============ Stage 2: Nginx 托管 ============
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx-mobile.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
